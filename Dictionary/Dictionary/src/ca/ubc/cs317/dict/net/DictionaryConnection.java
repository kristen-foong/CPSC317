package ca.ubc.cs317.dict.net;

import ca.ubc.cs317.dict.model.Database;
import ca.ubc.cs317.dict.model.Definition;
import ca.ubc.cs317.dict.model.MatchingStrategy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

import static ca.ubc.cs317.dict.net.DictStringParser.splitAtoms;

/**
 * Created by Jonatan on 2017-09-09.
 */
public class DictionaryConnection {

    private static final int DEFAULT_PORT = 2628;

    // declare variables
    private final Socket socket;
    private final BufferedReader input;
    private final PrintWriter output;

    /** Establishes a new connection with a DICT server using an explicit host and port number, and handles initial
     * welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @param port Port number used by the DICT server
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host, int port) throws DictConnectionException {

        // TODO Add your code here
        try {
            socket = new Socket(host, port);
            input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            output = new PrintWriter(socket.getOutputStream(), true);
            Status status = Status.readStatus(input);
            System.out.println("status code: " + status.getStatusCode());
            if (status.getStatusCode() != 220) {
                throw new DictConnectionException();
            }
            System.out.println("status is ok");
        } 
        catch (Exception e) {
            DictConnectionException ex = new DictConnectionException(e);
            System.out.println("encountered an exception");
            throw ex;
        }
    }

    /** Establishes a new connection with a DICT server using an explicit host, with the default DICT port number, and
     * handles initial welcome messages.
     *
     * @param host Name of the host where the DICT server is running
     * @throws DictConnectionException If the host does not exist, the connection can't be established, or the messages
     * don't match their expected value.
     */
    public DictionaryConnection(String host) throws DictConnectionException {
        this(host, DEFAULT_PORT);
    }

    /** Sends the final QUIT message and closes the connection with the server. This function ignores any exception that
     * may happen while sending the message, receiving its reply, or closing the connection.
     *
     */
    public synchronized void close() {

        // TODO Add your code here

        /*
        try {
            output.println("QUIT");
            socket.close();
            System.out.println("Closed connection.");
        } catch (Exception e) {
            // exception
        }
        */

        try {
            output.println("QUIT");
            System.out.println("begin quit");
            //String msg = input.readLine();
            //System.out.println("msg:" + msg);
            Status status = Status.readStatus(input);
            System.out.println("quit: " + status.getStatusCode());
            if (status.getStatusCode() == 221) {
                // 221 - closing connection
                socket.close();
                System.out.println("Closed connection.");
            } else {
                throw new DictConnectionException();
            }
        } catch (Exception e) {

        }

    }

    /** Requests and retrieves all definitions for a specific word.
     *
     * @param word The word whose definition is to be retrieved.
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 definitions in the first database that has a definition for the word should be used
     *                 (database '!').
     * @return A collection of Definition objects containing all definitions returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Collection<Definition> getDefinitions(String word, Database database) throws DictConnectionException {
        Collection<Definition> set = new ArrayList<>();

        // TODO Add your code here
        try {
            output.println("DEFINE " + database.getName() + " " + word);
            Status status = Status.readStatus(input);
            if (status.getStatusCode() == 552) {
                // 552 - no match
                return set;
            } else if (status.getStatusCode() == 150) {
                // 150 - n definitions retrieved - definitions follow
                System.out.println("getdef status 150:");
                String response = status.getDetails(); // can't do readStatus, need that later
                try {
                    while(!response.equals(".")) {
                        String[] parseResponse = splitAtoms(response);
                        int n = Integer.parseInt(parseResponse[0]); // n definitions
                        System.out.println("n definitions: " + n);
                        // 151 - word database name - text follows
                        // For each definition, status code 151 is sent, followed by the textual body of the definition.
                        for (int i = 0; i < n; i++) {
                            status = Status.readStatus(input); // now read input
                            System.out.println("getdef status def check:" + status.getStatusCode());
                            response = status.getDetails();
                            parseResponse = splitAtoms(response);
                            if (status.getStatusCode() == 151) {
                                Definition def = new Definition(word, parseResponse[1]);
                                response = input.readLine();
                                while(!response.equals(".")) {
                                    def.appendDefinition(response);
                                    response = input.readLine();
                                }
                                set.add(def);
                            } else {
                                throw new DictConnectionException();
                            }
                        }
                    }
                    status = Status.readStatus(input);
                    if (status.getStatusCode() != 250) {
                        throw new DictConnectionException();
                    }
                    System.out.println("getdef done");
                } catch (Exception e) {
                    throw new DictConnectionException();
                }
            } else {
                // 550 - invalid database
                // 250 - ok (optional timing info) ???
                // throw new DictConnectionException();
                return set;
            }
        } catch (Exception e) {
            throw new DictConnectionException();
        }

        return set;
    }

    /** Requests and retrieves a list of matches for a specific word pattern.
     *
     * @param word     The word whose definition is to be retrieved.
     * @param strategy The strategy to be used to retrieve the list of matches (e.g., prefix, exact).
     * @param database The database to be used to retrieve the definition. A special database may be specified,
     *                 indicating either that all regular databases should be used (database name '*'), or that only
     *                 matches in the first database that has a match for the word should be used (database '!').
     * @return A set of word matches returned by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<String> getMatchList(String word, MatchingStrategy strategy, Database database) throws DictConnectionException {
        Set<String> set = new LinkedHashSet<>();

        // TODO Add your code here
        try {
            output.println("MATCH " + database.getName() + " " + strategy.getName() + " " + word);
            Status status = Status.readStatus(input);
            if (status.getStatusCode() == 552) {
                // 552 - no match
                return set;
            } else if (status.getStatusCode() == 152) {
                // 152 - n matches found, text follows
                System.out.println("getmatchlist status 152:");
                try {
                    String response = input.readLine();
                    while(!response.equals(".")) {
                        String[] matches = splitAtoms(response);
                        set.add(matches[1]);
                        response = input.readLine();
                    }
                } catch (Exception e) {
                    throw new DictConnectionException();
                }
                status = Status.readStatus(input);
                if (status.getStatusCode() != 250) {
                    throw new DictConnectionException();
                }
                System.out.println("getmatchlist done");
            } else {
                // 550 - invalid database
                // 551 - invalid strategy
                // 250 - ok (optional timing info) ???
                // throw new DictConnectionException();
                return set;
            }
        } catch (Exception e) {
            throw new DictConnectionException();
        }

        return set;
    }

    /** Requests and retrieves a map of database name to an equivalent database object for all valid databases used in the server.
     *
     * @return A map of Database objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Map<String, Database> getDatabaseList() throws DictConnectionException {
        Map<String, Database> databaseMap = new HashMap<>();

        // TODO Add your code here
        if (!databaseMap.isEmpty()) {
            return databaseMap;
        }
        output.println("SHOW DB");
        try {
            Status stat = Status.readStatus(input);
            if (stat.getStatusCode() == 110) {
                // 110: n databases present, text follows
                System.out.println("getdatabase status 110:");
                String response = input.readLine();
                while (!response.equals(".")) {
                    try {
                        String[] parseResponse = splitAtoms(response);
                        Database db = new Database(parseResponse[0], parseResponse[1]);
                        String dbname = db.getName();
                        databaseMap.put(dbname, db);
                        response = input.readLine();
                    } catch (Exception e) {

                    }
                }
                // check if not done / some error is going on
                stat = Status.readStatus(input);
                if (stat.getStatusCode() != 250) {
                    throw new DictConnectionException();
                }
                System.out.println("getdatabase done");
            } else if (stat.getStatusCode() == 554) {
              // 554: no databases present, do nothing
            }
        }
        catch (Exception e) {
            throw new DictConnectionException();
        }
        return databaseMap;
    }

    /** Requests and retrieves a list of all valid matching strategies supported by the server.
     *
     * @return A set of MatchingStrategy objects supported by the server.
     * @throws DictConnectionException If the connection was interrupted or the messages don't match their expected value.
     */
    public synchronized Set<MatchingStrategy> getStrategyList() throws DictConnectionException {
        Set<MatchingStrategy> set = new LinkedHashSet<>();

        // TODO Add your code here

        output.println("SHOW STRAT");

        try {
            Status status = Status.readStatus(input);
            if (status.getStatusCode() == 555) {
                // 555 - no strategies available
                return set;
            } else if (status.getStatusCode() == 111) {
                // 111 - n strategies available
                try {
                    System.out.println("getstrategy status 111: ");
                    String response = input.readLine();
                    while (!response.equals(".")) {
                        String[] parseResponse = splitAtoms(response);
                        set.add(new MatchingStrategy(parseResponse[0], parseResponse[1]));
                        response = input.readLine();
                    }
                    status = Status.readStatus(input);
                    // 250 - nothing following, should be done
                    if (status.getStatusCode() != 250) {
                        throw new DictConnectionException();
                    }
                    System.out.println("getstrategy done");
                } catch (Exception e) {
                    throw new DictConnectionException();
                }
            } else {
                // some other status code given
                throw new DictConnectionException();
            }
        } catch (Exception e) {
            throw new DictConnectionException();
        }

        return set;
    }
}

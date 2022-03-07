package ca.ubc.cs.cs317.dnslookup;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

public class DNSMessage {
    public static final int MAX_DNS_MESSAGE_LENGTH = 512;
    private final Map<String, Integer> nameToPosition = new HashMap<>();
    private final Map<Integer, String> positionToName = new HashMap<>();
    private final ByteBuffer buffer;

    public static final int QUERY = 0;

    /**
     * Initializes an empty DNSMessage with the given id.
     *
     * @param id The id of the message.
     */
    public DNSMessage(short id) {
        this.buffer = ByteBuffer.allocate(MAX_DNS_MESSAGE_LENGTH);
        // code for header
        buffer.putShort(id); // id: short
        buffer.put((byte) 0); // qr, opcode, aa, tc, rd
        buffer.put((byte) 0); // ra, z, rcode
        buffer.put((byte) 0); // qd count
        buffer.put((byte) 0);
        buffer.put((byte) 0); // an count
        buffer.put((byte) 0);
        buffer.put((byte) 0); // ns count
        buffer.put((byte) 0);
        buffer.put((byte) 0); // ar count
        buffer.put((byte) 0);
        buffer.position(12);

    }

    /**
     * Initializes a DNSMessage with the first length bytes of the given byte array.
     *
     * @param recvd The byte array containing the received message
     * @param length The length of the data in the array
     */
    public DNSMessage(byte[] recvd, int length) {
        buffer = ByteBuffer.wrap(recvd, 0, length);
        buffer.position(12);
    }

    /**
     * Getters and setters for the various fixed size and fixed location fields of a DNSMessage
     */
    public int getID() {
        return (buffer.getShort(0) & 0xFFFF);
    }

    public void setID(int id) {
        buffer.putShort(0, (short) id);
    }

    public boolean getQR() {
        int qr =  buffer.get(2) & 0x80;
        if (qr > 0) {
            return true;
        }
        return false;
    }

    public void setQR(boolean qr) {
        byte q = buffer.get(2);
        if (qr) {
            q = (byte) (q | 0b10000000);
            buffer.put(2, q);
        } else {
            q = (byte) (q & 0b01111111);
            buffer.put(2, q);
        }
    }

    public boolean getAA() {
        int aa =  buffer.get(2) & 0b00000100;
        if (aa > 0) {
            return true;
        }
        return false;
    }

    public void setAA(boolean aa) {
        byte q = buffer.get(2);
        if (aa) {
            q = (byte) (q | 0b00000100);
            buffer.put(2, q);
        } else {
            q = (byte) (q & 0b11111011);
            buffer.put(2, q);
        }
    }

    public int getOpcode() {
        byte b = buffer.get(2);
        int opcode = (b & 0b01111000) >> 3;
        return opcode;
    }

    public void setOpcode(int opcode) {
        byte q = buffer.get(2);
        q = (byte) (q & 0b10000111);
        q = (byte) (q | ((byte) opcode << 3));
        buffer.put(2, q);
    }

    public boolean getTC() {
        int tc =  buffer.get(2) & 0x02;
        if (tc > 0) {
            return true;
        }
        return false;
    }

    public void setTC(boolean tc) {
        byte q = buffer.get(2);
        if (tc) {
            q = (byte) (q | 0b00000010);
            buffer.put(2, q);
        } else {
            q = (byte) (q & 0b11111101);
            buffer.put(2, q);
        }
    }

    public boolean getRD() {
        int rd =  buffer.get(2) & 0x01;
        if (rd > 0) {
            return true;
        }
        return false;
    }

    public void setRD(boolean rd) {
        byte q = buffer.get(2);
        if (rd) {
            q = (byte) (q | 0b00000001);
            buffer.put(2, q);
        } else {
            q = (byte) (q & 0b11111110);
            buffer.put(2, q);
        }
    }

    public boolean getRA() {
        int qr =  buffer.get(3) & 0x80;
        if (qr > 0) {
            return true;
        }
        return false;
    }

    public void setRA(boolean ra) {
        byte q = buffer.get(3);
        if (ra) {
            q = (byte) (q | 0b10000000);
            buffer.put(3, q);
        } else {
            q = (byte) (q & 0b01111111);
            buffer.put(3, q);
        }
    }

    public int getRcode() {
        byte b = buffer.get(3);
        int rcode = b & 0b00001111;
        return rcode;
    }

    public void setRcode(int rcode) {
        byte q = buffer.get(3);
        q = (byte) (q & 0b11110000);
        q = (byte) (q | rcode);
        buffer.put(3, q);
    }

    public int getQDCount() {
        return (buffer.getShort(4) & 0xFFFF);
    }

    public void setQDCount(int count) {
        buffer.putShort(4, (short) count);
    }

    public int getANCount() {
        return (buffer.getShort(6) & 0xFFFF);
    }

    public int getNSCount() {
        return (buffer.getShort(8) & 0xFFFF);
    }

    public int getARCount() {
        return (buffer.getShort(10) & 0xFFFF);
    }

    public void setARCount(int count) {
        buffer.putShort(10, (short) count);
    }

    /**
     * Return the name at the current position() of the buffer.  This method is provided for you,
     * but you should ensure that you understand what it does and how it does it.
     *
     * The trick is to keep track of all the positions in the message that contain names, since
     * they can be the target of a pointer.  We do this by storing the mapping of position to
     * name in the positionToName map.
     *
     * @return The decoded name
     */
    public String getName() {
        // Remember the starting position for updating the name cache
        int start = buffer.position();
        int len = buffer.get() & 0xff;
        if (len == 0) return "";
        if ((len & 0xc0) == 0xc0) {  // This is a pointer
            int pointer = ((len & 0x3f) << 8) | (buffer.get() & 0xff);
            String suffix = positionToName.get(pointer);
            assert suffix != null;
            positionToName.put(start, suffix);
            return suffix;
        }
        byte[] bytes = new byte[len];
        buffer.get(bytes, 0, len);
        String label = new String(bytes, StandardCharsets.UTF_8);
        String suffix = getName();
        String answer = suffix.isEmpty() ? label : label + "." + suffix;
        positionToName.put(start, answer);
        return answer;
    }

    /**
     * The standard toString method that displays everything in a message.
     * @return The string representation of the message
     */
    public String toString() {
        // Remember the current position of the buffer so we can put it back
        // Since toString() can be called by the debugger, we want to be careful to not change
        // the position in the buffer.  We remember what it was and put it back when we are done.
        int end = buffer.position();
        final int DataOffset = 12;
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("ID: ").append(getID()).append(' ');
            sb.append("QR: ").append(getQR()).append(' ');
            sb.append("OP: ").append(getOpcode()).append(' ');
            sb.append("AA: ").append(getAA()).append('\n');
            sb.append("TC: ").append(getTC()).append(' ');
            sb.append("RD: ").append(getRD()).append(' ');
            sb.append("RA: ").append(getRA()).append(' ');
            sb.append("RCODE: ").append(getRcode()).append(' ')
                    .append(dnsErrorMessage(getRcode())).append('\n');
            sb.append("QDCount: ").append(getQDCount()).append(' ');
            sb.append("ANCount: ").append(getANCount()).append(' ');
            sb.append("NSCount: ").append(getNSCount()).append(' ');
            sb.append("ARCount: ").append(getARCount()).append('\n');
            buffer.position(DataOffset);
            showQuestions(getQDCount(), sb);
            showRRs("Authoritative", getANCount(), sb);
            showRRs("Name servers", getNSCount(), sb);
            showRRs("Additional", getARCount(), sb);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "toString failed on DNSMessage";
        }
        finally {
            buffer.position(end);
        }
    }

    /**
     * Add the text representation of all the questions (there are nq of them) to the StringBuilder sb.
     *
     * @param nq Number of questions
     * @param sb Collects the string representations
     */
    private void showQuestions(int nq, StringBuilder sb) {
        sb.append("Question [").append(nq).append("]\n");
        for (int i = 0; i < nq; i++) {
            DNSQuestion question = getQuestion();
            sb.append('[').append(i).append(']').append(' ').append(question).append('\n');
        }
    }

    /**
     * Add the text representation of all the resource records (there are nrrs of them) to the StringBuilder sb.
     *
     * @param kind Label used to kind of resource record (which section are we looking at)
     * @param nrrs Number of resource records
     * @param sb Collects the string representations
     */
    private void showRRs(String kind, int nrrs, StringBuilder sb) {
        sb.append(kind).append(" [").append(nrrs).append("]\n");
        for (int i = 0; i < nrrs; i++) {
            ResourceRecord rr = getRR();
            sb.append('[').append(i).append(']').append(' ').append(rr).append('\n');
        }
    }

    /**
     * Decode and return the question that appears next in the message.  The current position in the
     * buffer indicates where the question starts.
     *
     * @return The decoded question
     */
    public DNSQuestion getQuestion() {
        String h = getName();
        int recordType = buffer.getShort() & 0xFF;
        int recordClass = buffer.getShort() & 0xFF;
        RecordType rt = RecordType.getByCode(recordType);
        RecordClass rc = RecordClass.getByCode(recordClass);
        DNSQuestion q = new DNSQuestion(h, rt, rc);
        return q;
        //return null;
    }

    /**
     * Decode and return the resource record that appears next in the message.  The current
     * position in the buffer indicates where the resource record starts.
     *
     * @return The decoded resource record
     */
    public ResourceRecord getRR() {
        DNSQuestion q = getQuestion();
        RecordType rt = q.getRecordType();
        RecordClass rc = q.getRecordClass();

        int ttl = buffer.getInt();
        // result is a string
        int rlen = buffer.getShort() & 0xFF;
        // resource record type checks
        String result = "";
        if (rt.equals(RecordType.A) || rt.equals(RecordType.AAAA)) {
            byte[] data = new byte[rlen];
            buffer.get(data, 0, rlen);
            try {
                InetAddress i = InetAddress.getByAddress(data);
                return new ResourceRecord(q, ttl, i);
            } catch (Exception e) {
                // yeet
            }
        } else if (rt.equals(RecordType.CNAME) || rt.equals(RecordType.NS)) {
            return new ResourceRecord(q, ttl, getName());
        } else if (rt.equals(RecordType.MX)) {
            int pos = buffer.position();
            buffer.position(pos + 2);
            return new ResourceRecord(q, ttl, getName());
        } else {
            result = getName();
        }
        return new ResourceRecord(q, ttl, result);
    }

    /**
     * Helper function that returns a hex string representation of a byte array. May be used to represent the result of
     * records that are returned by a server but are not supported by the application (e.g., SOA records).
     *
     * @param data a byte array containing the record data.
     * @return A string containing the hex value of every byte in the data.
     */
    private static String byteArrayToHexString(byte[] data) {
        return IntStream.range(0, data.length).mapToObj(i -> String.format("%02x", data[i])).reduce("", String::concat);
    }

    /**
     * Add an encoded name to the message. It is added at the current position and uses compression
     * as much as possible.  Compression is accomplished by remembering the position of every added
     * label.
     *
     * @param name The name to be added
     */
    public void addName(String name) {
        String label;
        while (name.length() > 0) {
            Integer offset = nameToPosition.get(name);
            if (offset != null) {
                int pointer = offset;
                pointer |= 0xc000;
                buffer.putShort((short)pointer);
                return;
            } else {
                nameToPosition.put(name, buffer.position());
                int dot = name.indexOf('.');
                label = (dot > 0) ? name.substring(0, dot) : name;
                buffer.put((byte)label.length());
                for (int j = 0; j < label.length(); j++) {
                    buffer.put((byte)label.charAt(j));
                }
                name = (dot > 0) ? name.substring(dot + 1) : "";
            }
        }
        buffer.put((byte) 0);
    }

    /**
     * Add an encoded question to the message at the current position.
     * @param question The question to be added
     */
    public void addQuestion(DNSQuestion question) {
        String name = question.getHostName();
        int rt = question.getRecordType().getCode();
        int rc = question.getRecordClass().getCode();
        addName(name);
        buffer.putShort((short) rt);
        buffer.putShort((short) rc);
        setQDCount(getQDCount() + 1);
    }

    /**
     * Add an encoded resource record to the message at the current position.
     * @param rr The resource record to be added
     */
    public void addResourceRecord(ResourceRecord rr, String section) {
        DNSQuestion q = rr.getQuestion();
        RecordType rt = rr.getRecordType();

        if (rt.equals(RecordType.A) || rt.equals(RecordType.AAAA)) {
            addName(q.getHostName());
            addQType(rr.getRecordType());
            addQClass(rr.getRecordClass());
            long ttl = rr.getRemainingTTL();
            buffer.putInt((int) ttl);
            InetAddress addr = rr.getInetResult();
            byte[] a = addr.getAddress();
            buffer.putShort((short) a.length);
            buffer.put(a);
        } else if (rt.equals(RecordType.CNAME) || rt.equals(RecordType.NS)) {
            addName(q.getHostName());
            addQType(rr.getRecordType());
            addQClass(rr.getRecordClass());
            long ttl = rr.getRemainingTTL();
            buffer.putInt((int) ttl);
            String result = rr.getTextResult();
            buffer.putShort((short) result.length());
            addName(result);
        } else if (rt.equals(RecordType.MX)) {
            addName(q.getHostName());
            addQType(rr.getRecordType());
            addQClass(rr.getRecordClass());
            long ttl = rr.getRemainingTTL();
            buffer.putInt((int) ttl);
            String result = rr.getTextResult();
            buffer.putShort((short) result.length());
            buffer.putShort((short) 0); //dummy short
            addName(result);
        }
        setARCount(getARCount() + 1);
    }

    /**
     * Add an encoded type to the message at the current position.
     * @param recordType The type to be added
     */
    private void addQType(RecordType recordType) {
        int code = recordType.getCode();
        buffer.putShort((short) code);
    }

    /**
     * Add an encoded class to the message at the current position.
     * @param recordClass The class to be added
     */
    private void addQClass(RecordClass recordClass) {
        int code = recordClass.getCode();
        buffer.putShort((short) code);
    }

    /**
     * Return a byte array that contains all the data comprising this message.  The length of the
     * array will be exactly the same as the current position in the buffer.
     * @return A byte array containing this message's data
     */
    public byte[] getUsed() {
        // return new byte[0];
        int pos = buffer.position();
        byte[] arr = new byte[pos];
        buffer.position(0);
        buffer.get(arr, 0, pos);
        buffer.position(pos);
        return arr;
//        return buffer.array();
    }

    /**
     * Returns a string representation of a DNS error code.
     *
     * @param error The error code received from the server.
     * @return A string representation of the error code.
     */
    public static String dnsErrorMessage(int error) {
        final String[] errors = new String[]{
                "No error", // 0
                "Format error", // 1
                "Server failure", // 2
                "Name error (name does not exist)", // 3
                "Not implemented (parameters not supported)", // 4
                "Refused" // 5
        };
        if (error >= 0 && error < errors.length)
            return errors[error];
        return "Invalid error message";
    }
}

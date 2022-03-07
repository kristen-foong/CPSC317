package ca.ubc.cs.cs317.dnslookup;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DNSMessageTest {
    @Test
    public void testConstructor() {
        DNSMessage message = new DNSMessage((short)23);

        System.out.println(message.getQR());
        System.out.println(message.getRD());
        System.out.println(message.getQDCount());
        System.out.println(message.getANCount());
        System.out.println(message.getNSCount());
        System.out.println(message.getARCount());
        System.out.println(message.getID());

        assertFalse(message.getQR());
        assertFalse(message.getRD());
        assertEquals(0, message.getQDCount());
        assertEquals(0, message.getANCount());
        assertEquals(0, message.getNSCount());
        assertEquals(0, message.getARCount());
        assertEquals(23, message.getID());
    }

    @Test
    public void testConstructorParam() {
        // arr[0,1] id, arr[2] qr & op code & aa + tc + rd, arr[3] ra & z & rcode
        // arr[4,5] qdcount, arr[6,7] ancount, arr[8,9] nscount, arr[10,11] arcount
        byte[] arr = {0x00, 0x17, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        DNSMessage message = new DNSMessage(arr, 12);

        System.out.println(message.getQR());
        System.out.println(message.getRD());
        System.out.println(message.getQDCount());
        System.out.println(message.getANCount());
        System.out.println(message.getNSCount());
        System.out.println(message.getARCount());
        System.out.println(message.getID());

        assertFalse(message.getQR());
        assertFalse(message.getRD());
        assertEquals(0, message.getQDCount());
        assertEquals(0, message.getANCount());
        assertEquals(0, message.getNSCount());
        assertEquals(0, message.getARCount());
        assertEquals(23, message.getID());
    }


    @Test
    public void testConstructorParam2() {
        // arr[0,1] id, arr[2] qr & op code & aa + tc + rd, arr[3] ra & z & rcode
        // arr[4,5] qdcount, arr[6,7] ancount, arr[8,9] nscount, arr[10,11] arcount
        byte[] arr = {0x00, 0x20, 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        DNSMessage message = new DNSMessage(arr, 12);

        System.out.println(message.getQR());
        System.out.println(message.getTC());
        System.out.println(message.getRD());
        System.out.println(message.getQDCount());
        System.out.println(message.getANCount());
        System.out.println(message.getNSCount());
        System.out.println(message.getARCount());
        System.out.println(message.getID());

        assertFalse(message.getQR());
        assertTrue(message.getTC());
        assertTrue(message.getRD());
        assertEquals(0, message.getQDCount());
        assertEquals(0, message.getANCount());
        assertEquals(0, message.getNSCount());
        assertEquals(0, message.getARCount());
        assertEquals(32, message.getID());
    }

    @Test
    public void testBasicFieldAccess() {
        DNSMessage message = new DNSMessage((short)23);

        message.setOpcode(0);
        message.setQR(true);
        message.setRD(true);
        message.setQDCount(1);

        System.out.println(message.getOpcode());
        System.out.println(message.getQR());
        System.out.println(message.getRD());
        System.out.println(message.getQDCount());

        assertTrue(message.getQR());
        assertTrue(message.getRD());
        assertEquals(1, message.getQDCount());
    }

    @Test
    public void testBasicFieldAccess2() {
        // arr[0,1] id, arr[2] qr & op code & aa + tc + rd, arr[3] ra & z & rcode
        // arr[4,5] qdcount, arr[6,7] ancount, arr[8,9] nscount, arr[10,11] arcount
        byte[] arr = {0x00, 0x20, 0x03, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        DNSMessage message = new DNSMessage(arr, 12);
        message.setOpcode(3);
        message.setQR(true);
        message.setRD(true);
        message.setQDCount(1);
        message.setID(3);
        message.setRA(true);
        message.setRcode(3);
        message.setARCount(20);

        System.out.println(message.getOpcode());
        System.out.println(message.getQR());
        System.out.println(message.getRD());
        System.out.println(message.getQDCount());
        System.out.println(message.getANCount());

        assertEquals(3, message.getOpcode());
        assertTrue(message.getQR());
        assertTrue(message.getRD());
        assertEquals(1, message.getQDCount());
        assertTrue(message.getID() == 3);
        assertTrue(message.getRA());
        assertEquals(3, message.getRcode());
        assertEquals(20, message.getARCount());
        assertEquals(0, message.getANCount());
    }

    @Test
    public void testGetQuestion() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.A, RecordClass.IN);
        assertEquals(question.getHostName(), "norm.cs.ubc.ca");
        assertEquals(question.getRecordClass(), RecordClass.IN);
        assertEquals(question.getRecordType(), RecordType.A);
    }

    @Test
    public void testAddQuestion() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.A, RecordClass.IN);
        request.addQuestion(question);
        byte[] content = request.getUsed();

        System.out.println("req w q: " + request.toString());

        DNSMessage reply = new DNSMessage(content, content.length);
        System.out.println("reply: " + reply.toString());
        assertEquals(request.getID(), reply.getID());
        assertEquals(request.getQDCount(), reply.getQDCount());
        assertEquals(request.getANCount(), reply.getANCount());
        assertEquals(request.getNSCount(), reply.getNSCount());
        assertEquals(request.getARCount(), reply.getARCount());
        DNSQuestion replyQuestion = reply.getQuestion();
        assertEquals(question, replyQuestion);
//        System.out.println("question: " + question.toString());
//        System.out.println("reply: " + replyQuestion.toString());
    }

    @Test
    public void testAddResourceRecord() {
        DNSMessage request = new DNSMessage((short)23);
        DNSQuestion question = new DNSQuestion("norm.cs.ubc.ca", RecordType.NS, RecordClass.IN);
        ResourceRecord rr = new ResourceRecord(question, RecordType.NS.getCode(), "ns1.cs.ubc.ca");
        request.addResourceRecord(rr);
        byte[] content = request.getUsed();

        System.out.println("request: "  + request.toString());

        DNSMessage reply = new DNSMessage(content, content.length);
        assertEquals(request.getID(), reply.getID());
        assertEquals(request.getQDCount(), reply.getQDCount());
        assertEquals(request.getANCount(), reply.getANCount());
        assertEquals(request.getNSCount(), reply.getNSCount());
        assertEquals(request.getARCount(), reply.getARCount());
        ResourceRecord replyRR = reply.getRR();
        assertEquals(rr, replyRR);
    }
}

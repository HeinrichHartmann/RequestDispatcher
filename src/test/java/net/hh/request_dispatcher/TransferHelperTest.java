package net.hh.request_dispatcher;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * Created by hartmann on 4/13/14.
 */
public class TransferHelperTest {

    ZMQ.Context ctx = ZMQ.context(0);
    ZMQ.Socket  server = ctx.socket(ZMQ.PAIR);
    ZMQ.Socket  client = ctx.socket(ZMQ.PAIR);

    @Before
    public void setUp() throws Exception {
        String channel = "inproc://channel";

        server.setLinger(100);
        client.setLinger(100);

        server.bind(channel);
        client.connect(channel);
    }

    @After
    public void tearDown() throws Exception {
        client.close();
        server.close();
        ctx.term();
    }

    @Test
    public void testRegularRequest() throws Exception {
        TransferHelper.sendMessage(server, new TransferHelper.TransferWrapper("Hi", 0));
        TransferHelper.TransferWrapper reply = TransferHelper.recvMessage(client, 0);
        Assert.assertEquals("Hi",reply.getObject().toString());
        Assert.assertEquals((Integer) 0 , reply.getCallbackId());
    }

    @Test
    public void testNoRecvMessage() throws Exception {
        TransferHelper.RawTransferWrapper msg = TransferHelper.recvMessage(server, ZMQ.NOBLOCK);
        Assert.assertNull(msg);
    }

    @Test(expected = TransferHelper.ProtocolException.class)
    public void testProtocolError() throws Exception {
        ZMsg msg = new ZMsg();
        msg.push(new byte[0]);
        msg.send(server);
        TransferHelper.RawTransferWrapper reply = TransferHelper.recvMessage(client, 0);
    }

    @Test(expected = TransferHelper.ProtocolException.class)
    public void testProtocolErrorGrabagePayload() throws Exception {
        ZMsg msg = new ZMsg();

        msg.push("XXX".getBytes()); // invalid Object representation
        msg.push(TransferHelper.int2bytes(0)); // callback ID
        msg.push(new byte[0]); // Add empty frame as REQ envelope

        msg.send(server);
        TransferHelper.RawTransferWrapper reply = TransferHelper.recvMessage(client, 0);
    }

    @Test(expected = TransferHelper.ProtocolException.class)
    public void testNotEmptyDelimiter() throws Exception {
        ZMsg msg = new ZMsg();

        msg.push(SerializationHelper.serialize("Hi")); // payload
        msg.push(TransferHelper.int2bytes(0)); // callback ID
        msg.push("XXX".getBytes()); // non-empty delimiter

        msg.send(server);
        TransferHelper.RawTransferWrapper reply = TransferHelper.recvMessage(client, 0);
    }


}

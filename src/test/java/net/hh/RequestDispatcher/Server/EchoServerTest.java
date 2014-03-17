package net.hh.RequestDispatcher.Server;

import junit.framework.Assert;
import org.jeromq.ZMQ;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by hartmann on 3/17/14.
 */
public class EchoServerTest {

    static EchoServer echoServer;
    static String echoEndpoint = "tcp://127.0.0.1:60123";

    @BeforeClass
    public static void setupMockServer() throws Exception {
        echoServer = new EchoServer(echoEndpoint);
        echoServer.start();
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        EchoServer.term();
        echoServer.stop();
    }

    @Test
    public void sendMessage(){
        ZMQ.Socket s = ZMQ.context().socket(ZMQ.REQ);
        s.connect(echoEndpoint);

        for (int i = 0; i<1000; i++){
            String MSG = "TEST MESSAGE";
            s.send(MSG);

            String REP = s.recvStr(500);
            Assert.assertEquals(MSG, REP);
        }
    }

}

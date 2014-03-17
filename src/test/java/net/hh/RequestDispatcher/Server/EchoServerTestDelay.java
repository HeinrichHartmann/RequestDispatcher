package net.hh.RequestDispatcher.Server;

import junit.framework.Assert;
import org.jeromq.ZMQ;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Created by hartmann on 3/17/14.
 */
public class EchoServerTestDelay {

    static EchoServer echoServer;
    static String echoEndpoint = "tcp://127.0.0.1:60124";

    static int DURATION_MS = 500;
    static int TOLERANCE_MS = 100;

    @BeforeClass
    public static void setupMockServer() throws Exception {
        echoServer = new EchoServer(echoEndpoint);
        echoServer.setDuration(DURATION_MS);
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

        for (int i = 0; i<10; i++){
            long sent_time = System.currentTimeMillis();

            String MSG = "TEST MESSAGE";
            s.send(MSG);

            String REP = s.recvStr(DURATION_MS + 500);
            Assert.assertEquals(MSG, REP);

            long duration = System.currentTimeMillis() - sent_time;
            Assert.assertTrue(DURATION_MS - TOLERANCE_MS < duration);
            Assert.assertTrue(duration < DURATION_MS + TOLERANCE_MS);
            System.out.println("Duration " + duration);
        }
    }
}

package net.hh.request_dispatcher;

import junit.framework.Assert;
import net.hh.request_dispatcher.EchoServer;
import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zeromq.ZMQ;

/**
 * Created by hartmann on 3/17/14.
 */
public class EchoServerTestDelay {

    static { BasicConfigurator.configure(); }

    static EchoServer echoServer;
    //static String echoEndpoint = "tcp://127.0.0.1:60124";
    static String echoEndpoint = "inproc:///tmp/test";

    private static ZMQ.Context ctx = ZMQ.context(1);

    private static int DURATION_MS = 0;
    private static int TOLERANCE_MS = 10;

    @BeforeClass
    public static void setupMockServer() throws Exception {
        echoServer = new EchoServer(ctx, echoEndpoint);
        echoServer.setDelay(DURATION_MS);
        System.out.println("Starting server");
        echoServer.start();
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        System.out.println("Stopping server");
        echoServer.stop();
        ctx.term();
    }

    @Test(timeout =  1000)
    public void sendMessage() throws Exception {

        ZMQ.Socket s = ctx.socket(ZMQ.REQ);
        s.setLinger(100);

        s.connect(echoEndpoint);
        String MSG = "TEST MESSAGE";

        for (int i = 0; i < 5; i++){
            long sent_time = System.currentTimeMillis();

            s.send(MSG);

            String REP = s.recvStr(DURATION_MS + 200);
            Assert.assertEquals(MSG, REP);

            // first request is always a bit off
            if (i == 0) continue;

            long duration = System.currentTimeMillis() - sent_time;
            System.out.println(
                    "Request took (ms) " + duration +
                    " expected: " + DURATION_MS + " +- " + TOLERANCE_MS);

            Assert.assertTrue(DURATION_MS - TOLERANCE_MS < duration);
            Assert.assertTrue(duration < DURATION_MS + TOLERANCE_MS);

            DURATION_MS += 50;
            echoServer.setDelay(DURATION_MS);

        }

        System.out.println("Destroying socket");
        s.close();
    }
}

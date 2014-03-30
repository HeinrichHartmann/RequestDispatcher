package net.hh.request_dispatcher.Server;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * Created by hartmann on 3/17/14.
 */
public class EchoServerPerformanceTest {

    // CONSTATNS
    static final int K = 1000;
    static final int M = K * K;
    static final int numRequests = 1 * K;

    static final String MSG = "TEST MESSAGE";
    static final int HWM = 1;

    static final ZMQ.Context ctx = ZMQ.context(1);

    // static final String echoEndpoint = "tcp://127.0.0.1:60123";
    static String echoEndpoint = "ipc://myservertest";
    static final EchoServer echoServer = new EchoServer(ctx, echoEndpoint);

    // LOGGING
    static final Logger log = Logger.getLogger(EchoServerPerformanceTest.class);
    static {
        BasicConfigurator.configure();
        Logger.getLogger(EchoServer.class).setLevel(Level.INFO);

        Logger.getRootLogger().setLevel(Level.INFO);
    }


    @BeforeClass
    public static void setupMockServer() throws Exception {
        echoServer.start();
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        ctx.term();
    }

    @Test// (timeout = 10 * K)
    public void sendMessage() throws InterruptedException {
        ZMQ.Socket s = ctx.socket(ZMQ.DEALER);
        s.setHWM(HWM);
        s.connect(echoEndpoint);


        int recvCnt     = 0;
        long start_time = System.currentTimeMillis();
        for (int i = 0; i < numRequests; i++){

            boolean suc = ZMsg.newStringMsg("", MSG).send(s);
            Assert.assertTrue(suc);

            if ((i % (10 * K)) == 0) { log.info("Snd " + i + " | Recv " + recvCnt); };

            ZMsg msg = ZMsg.recvMsg(s, ZMQ.NOBLOCK);
            if (msg != null) {
                Assert.assertEquals(MSG, msg.peekLast().toString());
                recvCnt++;
            }
        }

        // Send TERMINATE Echo Server signal
        // ZMsg.newStringMsg("", "TERM").send(s);

        log.info("Parallel recv count " + recvCnt + " from " + numRequests);

        //Thread.sleep(1000);

        s.setReceiveTimeOut(1000);
        for (; recvCnt < numRequests; recvCnt++) {
            ZMsg msg = ZMsg.recvMsg(s);

            if (msg == null) {
                int numLeft = numRequests - recvCnt;
                float percLeft = ((float) numLeft) / numRequests * 100;
                System.out.println("Dropped " + numLeft + " messages. " + percLeft + "%.");
                break;
            }

            Assert.assertEquals(MSG, msg.peekLast().toString());

            if ((recvCnt % (10 * K)) == 0) { log.info("Recv " + recvCnt); };
        }


        log.info("Total received count = " + recvCnt);
        log.info("Closing Socket");
        s.setLinger(0);
        s.close();


        long duration = System.currentTimeMillis() - start_time;
        System.out.println("" + 1000 * (((float) numRequests) / duration) + " req/sec");
    }

}

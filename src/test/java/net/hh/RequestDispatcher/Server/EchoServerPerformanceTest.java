package net.hh.RequestDispatcher.Server;

import net.hh.RequestDispatcher.ZmqContextHolder;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

/**
 * Created by hartmann on 3/17/14.
 */
public class EchoServerPerformanceTest {

    static final Logger log = Logger.getLogger(EchoServerPerformanceTest.class);

    static {
        BasicConfigurator.configure();
    }

    static EchoServer echoServer;
    static String echoEndpoint = "tcp://127.0.0.1:60123";
    // static String echoEndpoint = "inproc://myservertest";

    @BeforeClass
    public static void setupMockServer() throws Exception {
        echoServer = new EchoServer(echoEndpoint);
        echoServer.start();
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        echoServer.stop();
        // ZmqContextHolder.destroy();
    }

    @Test
    public void sendMessage(){
        ZMQ.Socket s = ZmqContextHolder.getZContext().createSocket(ZMQ.DEALER);

        // s.setSendBufferSize(100000);
        // s.setReceiveBufferSize(100000);
        // s.setSndHWM(100000);

        s.connect(echoEndpoint);

        String MSG = "TEST MESSAGE";

        final int K = 1000;
        final int M = K * K;
        int numRequests = 100 * K;
        int recvCnt     = 0;
        long start_time = System.currentTimeMillis();



        for (int i = 0; i < numRequests; i++){
            // System.out.println("Sending Message");
            // s.send(new byte[0], ZMQ.SNDMORE);
            // s.send(MSG);

            ZMsg.newStringMsg("", MSG).send(s);

            if ((i % (10 * K)) == 0) { System.out.println("Snd " + i + " | Recv " + recvCnt); };
            // Assert.assertEquals(MSG, REP);

            if (ZMsg.recvMsg(s,ZMQ.NOBLOCK) != null) {
                recvCnt++;
            } else {
                System.out.println("No msg");
            }

        }

        System.out.println("Recv count " + recvCnt);

        for (int i = recvCnt; i < numRequests; i++){
            // System.out.println("Sending Message");
            String REP = s.recvStr();
            if ((i % (10 * K)) == 0) { System.out.println("Recv 10K " + i); };
            // Assert.assertEquals(MSG, REP);
        }

        log.info("Closing Socket");
        s.close();

        long duration = System.currentTimeMillis() - start_time;
        log.info("" + numRequests + " Requests took " + duration + " ms");
        log.info("I.E.: " + 1000 * (((float) numRequests) / duration) + " req/sec");

        ZmqContextHolder.destroy();
    }

}

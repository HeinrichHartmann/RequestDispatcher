package net.hh.request_dispatcher.mock_server_tests;

import junit.framework.Assert;
import net.hh.request_dispatcher.mock_server.EchoServer;
import org.apache.log4j.BasicConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.zeromq.ZMQ;

/**
 * Created by hartmann on 3/17/14.
 */
public class EchoServerTest {

    static {
        BasicConfigurator.configure();
    }


    static ZMQ.Context ctx;
    static EchoServer echoServer;
    // static String echoEndpoint = "tcp://127.0.0.1:60123";
    static String echoEndpoint = "inproc://echoserver";

    @BeforeClass
    public static void setupMockServer() throws Exception {
        ctx = ZMQ.context(1);
        echoServer = new EchoServer(ctx, echoEndpoint);
        echoServer.start();

        // need to bind before connect
        Thread.sleep(100);
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        echoServer.stop();
        ctx.term();
    }

    @Test(timeout = 500)
    public void sendMessage(){
        ZMQ.Socket s = ctx.socket(ZMQ.REQ);

        s.connect(echoEndpoint);

        for (int i = 0; i < 10; i++){
            String MSG = "TEST MESSAGE";
            s.send(MSG);

            String REP = s.recvStr(50);
            Assert.assertEquals(MSG, REP);
        }

        s.setLinger(100);
        s.close();

    }

}

package net.hh.request_dispatcher;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

/**
 * Created by hartmann on 3/17/14.
 */
public class DispatcherTestTimeout {

    private static final int DURATION = 500;
    private static final int GRACE = 100;

    private EchoServer echoServer;
    private String echoEndpoint = "inproc://127.0.0.1:60123";
    private ZMQ.Context ctx = ZMQ.context(0);

    Dispatcher dp;

    @Before
    public void setUp() throws Exception {
        echoServer = new EchoServer(ctx, echoEndpoint);
        echoServer.setDelay(DURATION);
        echoServer.start();

        // before each Test
        dp = new Dispatcher(ctx);
        dp.registerService(String.class, echoEndpoint);
    }

    @After
    public void tearDown() throws Exception {
        dp.shutdown(); // close sockets
        ctx.term();
    }

    private final String TIMEOUT_MSG = "TIMEOUT";

    @Test
    public void testTimeOut() throws Exception {

        final String[] answer = new String[1];

        dp.execute("REQ", new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                throw new RuntimeException("Not Timed out");
            }

            @Override
            public void onTimeout() {
                answer[0] = TIMEOUT_MSG;
            }
        });

        // set too short timeout
        dp.gatherResults(1);

        Assert.assertEquals(TIMEOUT_MSG, answer[0]);
    }


    @Test(timeout = DURATION + 5 * GRACE)
    public void testTimeOk() throws Exception {
        final String[] answer = new String[1];

        dp.execute("REQ", new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                answer[0] = "OK";
            }

            @Override
            public void onTimeout() {
                throw new RuntimeException("Timed Out");
            }
        });

        // set graceful timeout
        dp.gatherResults(DURATION + GRACE);
        Assert.assertEquals("OK", answer[0]);
    }
}

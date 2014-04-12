package net.hh.request_dispatcher;

import junit.framework.Assert;
import net.hh.request_dispatcher.server.RequestHandler;
import net.hh.request_dispatcher.server.ZmqWorker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.io.Serializable;

/**
 * Created by hartmann on 4/11/14.
 */
public class RevisedDispatcherTest {

    private final ZMQ.Context ctx = ZMQ.context(0);

    private final RevisedDispatcher dp = new RevisedDispatcher(ctx);

    private final String echoChannel = "inproc://echoChannel";

    private final ZmqWorker echoWorker = new ZmqWorker<String, String>(
            ctx,
            echoChannel,
            new RequestHandler<String, String>() {
                @Override
                public String handleRequest(String request) throws Exception {
                    return request.toString();
                }
            }
    );

    private static final String CONST = "CONST";
    private final String constChannel = "inproc://constChannel";
    private final ZmqWorker constWorker = new ZmqWorker<constReq, String>(
            ctx,
            constChannel,
            new RequestHandler<constReq, String>() {
                @Override
                public String handleRequest(constReq request) throws Exception {
                    return CONST;
                }
            }
    );

    private class constReq implements Serializable {}


    @Before
    public void setUp() throws Exception {
        dp.registerService(String.class, echoChannel);
        dp.registerService(constReq.class, constChannel);

        echoWorker.start();
        constWorker.start();
    }

    @After
    public void tearDown() throws Exception {
        dp.shutdown();
        ctx.term(); // shuts down workers.
    }


    @Test
    public void testExecute() throws Exception {
        final String[] answer = new String[2];

        dp.execute("MSG", new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                answer[0] = reply;
            }
        });

        dp.execute(new constReq(), new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                answer[1] = reply;
            }
        });

        // gather all results
        dp.gatherResults(-1);

        Assert.assertEquals("MSG", answer[0]);
        Assert.assertEquals(CONST, answer[1]);
    }

    @Test
    public void testExecuteSync() throws Exception {
        String answer = (String) dp.executeSync("MSG", -1);
        Assert.assertEquals("MSG", answer);
    }

    @Test
    public void testShutdown() throws Exception {

    }
}

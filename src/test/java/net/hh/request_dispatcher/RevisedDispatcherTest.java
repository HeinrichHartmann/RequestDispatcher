package net.hh.request_dispatcher;

import junit.framework.Assert;
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

    private final Dispatcher dp = new Dispatcher(ctx);

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

    public static class constReq implements Serializable {
        public constReq() {}
    }

    private final String sleepChannel = "inproc://sleepChannel";
    private final ZmqWorker sleepWorker = new ZmqWorker<sleepReq, String>(
            ctx,
            sleepChannel,
            new RequestHandler<sleepReq, String>() {
                @Override
                public String handleRequest(sleepReq request) throws Exception {
                    Thread.sleep(100);
                    return null;
                }
            }
    );

    public static class sleepReq implements Serializable {
        public sleepReq() {}
    }


    @Before
    public void setUp() throws Exception {
        dp.registerService(String.class, echoChannel);
        dp.registerService(constReq.class, constChannel);
        dp.registerService(sleepReq.class, sleepChannel);

        echoWorker.start();
        constWorker.start();
        sleepWorker.start();
    }

    @After
    public void tearDown() throws Exception {
        dp.shutdown(); // closes adapter sockets.

        ctx.term(); // shuts down workers.
    }


    @Test
    public void testExecuteSync() throws Exception {
        String answer = (String) dp.executeSync("MSG", -1);
        Assert.assertEquals("MSG", answer);
    }

    @Test
    public void testExecute() throws Exception {
        dp.execute("MSG",           CallbackFactory(0));
        dp.execute(new constReq(),  CallbackFactory(1));

        // gather all results
        dp.gatherResults(-1);

        Assert.assertEquals("MSG", replies[0]);
        Assert.assertEquals(CONST, replies[1]);
    }

    @Test
    public void testTimeout() throws Exception {
        dp.execute(new sleepReq(), CallbackFactory(0));

        dp.gatherResults(50);

        Assert.assertEquals("TOUT", returnCodes[0]);
    }


    private final String [] returnCodes = new String[10];
    private final String [] replies = new String[10];

    private Callback CallbackFactory(final int index) {
        return new Callback() {
            @Override
            public void onSuccess(Serializable reply) {
                replies[index] = reply.toString();
                returnCodes[index] = "SUC";
            }

            @Override
            public void onTimeout() {
                returnCodes[index] = "TOUT";
            }

            @Override
            public void onError(RequestException e) {
                returnCodes[index] = "ERR";
            }
        };
    }

}

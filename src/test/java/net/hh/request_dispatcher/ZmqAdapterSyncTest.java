package net.hh.request_dispatcher;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.util.concurrent.TimeoutException;

/**
 * Created by hartmann on 4/10/14.
 */
public class ZmqAdapterSyncTest {

    private final ZMQ.Context ctx = ZMQ.context(0);

    private final String echoChannel = "inproc://echoChannel";
    private final ZmqWorker echoWorker = new ZmqWorker<String, String>(
            ctx,
            echoChannel,
            new RequestHandler<String, String>() {
                @Override
                public String handleRequest(String request) throws Exception {
                    return request;
                }
            }
    );
    private final ZmqAdapterSync<String, String> echoAdapter = new ZmqAdapterSync<String, String>(ctx, echoChannel);

    private final String errorChannel = "inproc://errorChannel";
    private final ZmqWorker errorWorker = new ZmqWorker<String, String>(
            ctx,
            errorChannel,
            new RequestHandler<String, String>() {
                @Override
                public String handleRequest(String request) throws Exception {
                    throw new Exception();
                }
            }
    );
    private final ZmqAdapterSync<String, String> errorAdapter = new ZmqAdapterSync<String, String>(ctx, errorChannel);


    private final String sleepChannel = "inproc://sleepChannel";
    private final ZmqWorker sleepWorker = new ZmqWorker<String, String>(
            ctx,
            sleepChannel,
            new RequestHandler<String, String>() {
                @Override
                public String handleRequest(String request) throws Exception {
                    Thread.sleep(100);
                    return "OK";
                }
            }
    );
    private final ZmqAdapterSync<String, String> sleepAdapter = new ZmqAdapterSync<String, String>(ctx, sleepChannel);



    @Before
    public void setUp() throws Exception {
        echoWorker.start();
        errorWorker.start();
        sleepWorker.start();
    }

    @After
    public void tearDown() throws Exception {
        echoAdapter.close();
        errorAdapter.close();
        sleepAdapter.close();

        ctx.term(); // closes running workers
    }

    //
    // SEND SYNC
    //

    @Test
    public void testSendSync() throws Exception {
        String MSG = "Hello World";
        String response = echoAdapter.sendSync(MSG, -1);
        Assert.assertEquals(MSG, response);
    }

    @Test(expected = RequestException.class)
    public void testSendSyncError() throws Exception {
        String response = errorAdapter.sendSync("", -1);
    }

    @Test(expected = TimeoutException.class)
    public void testSendSyncTimeout() throws Exception {
        String response = sleepAdapter.sendSync("MSG", 50);
    }
}

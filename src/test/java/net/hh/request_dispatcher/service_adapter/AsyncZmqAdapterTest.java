package net.hh.request_dispatcher.service_adapter;

import junit.framework.Assert;
import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.server.RequestException;
import net.hh.request_dispatcher.server.RequestHandler;
import net.hh.request_dispatcher.server.ZmqWorker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hartmann on 4/10/14.
 */
public class AsyncZmqAdapterTest {

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
    private final AsyncZmqAdapter<String, String> echoAdapter = new AsyncZmqAdapter<String, String>(ctx, echoChannel);

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
    private final AsyncZmqAdapter<String, String> errorAdapter = new AsyncZmqAdapter<String, String>(ctx, errorChannel);


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
    private final AsyncZmqAdapter<String, String> sleepAdapter = new AsyncZmqAdapter<String, String>(ctx, sleepChannel);

    private AtomicInteger requestCount = new AtomicInteger(0);
    private final String countChannel = "inproc://countChannel";
    private final ZmqWorker countWorker = new ZmqWorker<String, String>(
            ctx,
            countChannel,
            new RequestHandler<String, String>() {
                @Override
                public String handleRequest(String request) throws Exception {
                    requestCount.incrementAndGet();
                    return null;
                }
            }
    );
    private final AsyncZmqAdapter<String, String> countAdapter = new AsyncZmqAdapter<String, String>(ctx, countChannel);



    @Before
    public void setUp() throws Exception {
        echoWorker.start();
        errorWorker.start();
        sleepWorker.start();
        countWorker.start();
    }

    @After
    public void tearDown() throws Exception {
        echoAdapter.close();
        errorAdapter.close();
        sleepAdapter.close();
        countAdapter.close();

        ctx.term(); // closes running workers
    }



    //
    // SEND ASYNC
    //

    @Test
    public void testSend() throws Exception {
        String MSG = "MSG";
        final String[] answer = new String[1];

        echoAdapter.execute(MSG, new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                answer[0] = reply;
            }
        });
        echoAdapter.recvAndExec(-1);

        Assert.assertEquals(MSG, answer[0]);
    }

    @Test
    public void testSendError() throws Exception {
        String MSG = "MSG";
        final String[] answer = new String[1];

        errorAdapter.execute(MSG, new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
            }

            @Override
            public void onError(RequestException e) {
                answer[0] = "ERR";
            }
        });

        errorAdapter.recvAndExec(-1);

        Assert.assertEquals("ERR", answer[0]);
    }

    @Test
    public void testSendTimeout() throws Exception {
        String MSG = "MSG";
        final String[] answer = new String[1];

        sleepAdapter.execute(MSG, new Callback<String>() {
            @Override
            public void onSuccess(String reply) {}

            @Override
            public void onTimeout() {
                answer[0] = "TOUT";
            }
        });

        sleepAdapter.recvAndExec(50);

        Assert.assertEquals("TOUT", answer[0]);
    }

    @Test
    public void testStackedSend() throws Exception {
        final String[] answer = new String[2];

        echoAdapter.execute("MSG0", new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                answer[0] = reply;

                echoAdapter.execute("MSG1", new Callback<String>() {
                    @Override
                    public void onSuccess(String reply) {
                        answer[1] = reply;
                    }
                });

            }
        });

        echoAdapter.recvAndExec(-1);
        echoAdapter.recvAndExec(-1);

        Assert.assertEquals("MSG0", answer[0]);
        Assert.assertEquals("MSG1", answer[1]);
    }

    @Test
    public void testOneWayExecute() throws Exception {
        int NUM_REQ = 100;

        for (int i = 0; i < NUM_REQ; i++) {
            countAdapter.execute("Hi", null);
        }

        Thread.sleep(NUM_REQ * 10); // 10ms per request.

        Assert.assertEquals(NUM_REQ, requestCount.intValue());

    }
}

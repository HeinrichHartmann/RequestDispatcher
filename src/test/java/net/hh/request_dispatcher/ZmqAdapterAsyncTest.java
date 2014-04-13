package net.hh.request_dispatcher;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by hartmann on 4/10/14.
 */
public class ZmqAdapterAsyncTest {

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
    private final ZmqAdapterAsync<String, String> echoAdapter = new ZmqAdapterAsync<String, String>(ctx, echoChannel);

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
    private final ZmqAdapterAsync<String, String> errorAdapter = new ZmqAdapterAsync<String, String>(ctx, errorChannel);


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
    private final ZmqAdapterAsync<String, String> sleepAdapter = new ZmqAdapterAsync<String, String>(ctx, sleepChannel);

    private AtomicInteger requestCount = new AtomicInteger(0);
    private final String countChannel = "inproc://countChannel";
    private final ZmqWorker countWorker = new ZmqWorker<String, String>(
            ctx,
            countChannel,
            new RequestHandler<String, String>() {
                @Override
                public String handleRequest(String request) throws Exception {
                    requestCount.incrementAndGet();
                    return "OK";
                }
            }
    );
    private final ZmqAdapterAsync<String, String> countAdapter = new ZmqAdapterAsync<String, String>(ctx, countChannel);



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

        echoAdapter.recvAndExec(0); // blocking receive

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

        ZmqAdapterAsync.RC rc = errorAdapter.recvAndExec(0);

        Assert.assertEquals(ZmqAdapterAsync.RC.ERR, rc);
        Assert.assertEquals("ERR", answer[0]);
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

        ZmqAdapterAsync.RC rc1 = echoAdapter.recvAndExec(0);
        ZmqAdapterAsync.RC rc2 = echoAdapter.recvAndExec(0);

        Assert.assertEquals(ZmqAdapterAsync.RC.SUC, rc1);
        Assert.assertEquals(ZmqAdapterAsync.RC.SUC, rc2);

        Assert.assertEquals("MSG0", answer[0]);
        Assert.assertEquals("MSG1", answer[1]);
    }

    @Test(timeout = 1000)
    public void testOneWayExecute() throws Exception {
//        final String [] answer = new String[1];
//        int NUM_REQ = 100;
//
//        for (int i = 0; i < NUM_REQ; i++) {
//            countAdapter.execute("Hi", null);
//        }
//
//        countAdapter.execute("END", new Callback<String>() {
//            @Override
//            public void onSuccess(String reply) {
//                answer[0] = reply;
//            }
//        });
//
//        countAdapter.recvAndExec(0);
//
//        Thread.sleep(100);
//
//        // Assert.assertEquals("END", answer[0]);
//
//        Assert.assertEquals(NUM_REQ, requestCount.intValue());

    }
}

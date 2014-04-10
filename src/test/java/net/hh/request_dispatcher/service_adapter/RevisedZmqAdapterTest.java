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

import java.util.concurrent.TimeoutException;

/**
 * Created by hartmann on 4/10/14.
 */
public class RevisedZmqAdapterTest {

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
    private final RevisedZmqAdapter<String, String> echoAdapter = new RevisedZmqAdapter<String, String>(ctx, echoChannel);

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
    private final RevisedZmqAdapter<String, String> errorAdapter = new RevisedZmqAdapter<String, String>(ctx, errorChannel);


    private final String sleepChannel = "inproc://errorChannel";
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
    private final RevisedZmqAdapter<String, String> sleepAdapter = new RevisedZmqAdapter<String, String>(ctx, sleepChannel);



    @Before
    public void setUp() throws Exception {
        echoWorker.start();
        errorWorker.start();
        sleepWorker.start();
    }

    @After
    public void tearDown() throws Exception {
        ctx.term(); // closes running workers
    }

    //
    // SEND SYNC
    //

    @Test
    public void testSendSync() throws Exception {
        String MSG = "Hello World";
        String response = echoAdapter.sendSync(MSG, 0);
        Assert.assertEquals(MSG, response);
    }

    @Test(expected = RequestException.class)
    public void testSendSyncError() throws Exception {
        String response = errorAdapter.sendSync("", 0);
    }

    @Test(expected = TimeoutException.class)
    public void testSendSyncTimeout() throws Exception {
        String response = errorAdapter.sendSync("MSG", 50);
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
        echoAdapter.recvAndExec(0);

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
            public void onError(RequestException e) throws RequestException {
                super.onError(e);
                answer[0] = "ERR";
            }
        });

        echoAdapter.recvAndExec(0);

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
            public void onTimeOut() {
                answer[0] = "TOUT";
            }
        });

        sleepAdapter.recvAndExec(50);

        Assert.assertEquals("TOUT", answer[0]);
    }

}

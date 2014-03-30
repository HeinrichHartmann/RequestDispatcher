package net.hh.request_dispatcher.worker;

import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.Dispatcher;
import net.hh.request_dispatcher.service_adapter.ZmqAdapter;
import net.hh.request_dispatcher.server.RequestHandler;
import net.hh.request_dispatcher.server.ZmqWorker;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZMQ;

/**
 * Created by hartmann on 3/30/14.
 */
public class ZmqWorkerTest {

    ZMQ.Context ctx;
    ZmqWorker<String, String> worker;
    Dispatcher dp;

    @Before
    public void setUp() throws Exception {
        ctx = ZMQ.context(0);

        String channel = "inproc://" + this.hashCode();

        worker = new ZmqWorker<String, String>(ctx, channel, new RequestHandler<String, String>() {
            @Override
            public String handleRequest(String request) {
                return "HelloWorld";
            }
        });

        dp = new Dispatcher();
        dp.registerServiceAdapter("TEST", new ZmqAdapter(ctx, channel));
        dp.setDefaultService(String.class, "TEST");

        worker.start();
    }

    @After
    public void tearDown() throws Exception {
        dp.close();
        ctx.term();
        worker.join();
    }

    @Test(timeout = 500)
    public void testRun() throws Exception {
        final String[] answer = new String[1];

        dp.execute("Hi", new Callback<String>(){
            @Override
            public void onSuccess(String reply) {
                System.out.println("Success " + reply);
                answer[0] = reply;
            }
        });

        dp.gatherResults();

        Assert.assertEquals("HelloWorld", answer[0]);
    }
}

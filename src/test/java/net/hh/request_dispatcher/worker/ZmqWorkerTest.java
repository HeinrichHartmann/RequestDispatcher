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
    ZmqWorker stringWorker;
    ZmqWorker exampleTOStringZmqWorker;
    Dispatcher dp;

    @Before
    public void setUp() throws Exception {
        ctx = ZMQ.context(0);

        dp = new Dispatcher();

        /// SETUP WORKER: String -> String

        String stringChannel = "inproc://string" + this.hashCode();

        stringWorker = new ZmqWorker<String, String>(ctx,
                stringChannel, new RequestHandler<String, String>() {
            @Override
            public String handleRequest(String request) {
                return "HelloWorld";
            }
        });

        dp.registerServiceAdapter("STRING", new ZmqAdapter(ctx, stringChannel));
        dp.setDefaultService(String.class, "STRING");

        stringWorker.start();

        /// SETUP WORKER: ReqTO -> ReplyTo

        String exampleChannel = "inproc://example" + this.hashCode();

        exampleTOStringZmqWorker = new ZmqWorker<ReqTO, ReplyTo>(ctx, exampleChannel,
                new RequestHandler<ReqTO, ReplyTo>() {
            @Override
            public ReplyTo handleRequest(ReqTO request) {
                return new ReplyTo(request.toString());
            }
        });

        dp.registerServiceAdapter("EXAMPLE", new ZmqAdapter(ctx, exampleChannel));

        dp.setDefaultService(ReqTO.class, "EXAMPLE");

        exampleTOStringZmqWorker.start();
    }

    @After
    public void tearDown() throws Exception {
        dp.close();
        ctx.term();
        stringWorker.join();

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

    @Test(timeout = 500)
    public void testRun2() throws Exception {
        final String[] answer = new String[1];

        String MSG = "Hi";

        dp.execute(new ReqTO(MSG), new Callback<ReplyTo>(){
            @Override
            public void onSuccess(ReplyTo reply) {
                answer[0] = reply.toString();
            }
        });

        dp.gatherResults();
        Assert.assertEquals(MSG, answer[0]);
    }
}
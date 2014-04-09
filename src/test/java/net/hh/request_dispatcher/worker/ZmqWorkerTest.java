package net.hh.request_dispatcher.worker;

import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.Dispatcher;
import net.hh.request_dispatcher.server.RequestException;
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
    ZmqWorker errorWorker;
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

        /// SETUP WORKER: ReqTO -> RepTO

        String exampleChannel = "inproc://example" + this.hashCode();

        exampleTOStringZmqWorker = new ZmqWorker<ReqTO, RepTO>(ctx, exampleChannel,
                new RequestHandler<ReqTO, RepTO>() {
            @Override
            public RepTO handleRequest(ReqTO request) {
                return new RepTO(request.toString());
            }
        });

        dp.registerServiceAdapter("EXAMPLE", new ZmqAdapter(ctx, exampleChannel));

        dp.setDefaultService(ReqTO.class, "EXAMPLE");

        exampleTOStringZmqWorker.start();

        /// SETUP WORKER: String -> ERROR

        String errorChannel = "inproc://error" + this.hashCode();

        errorWorker = new ZmqWorker<String, String>(ctx,
                stringChannel, new RequestHandler<String, String>() {
            @Override
            public String handleRequest(String request) throws Exception {
                throw new Exception("ERROR");
            }
        });

        dp.registerServiceAdapter("ERROR", new ZmqAdapter(ctx, stringChannel));
        errorWorker.start();

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

        dp.execute(new ReqTO(MSG), new Callback<RepTO>(){
            @Override
            public void onSuccess(RepTO reply) {
                answer[0] = reply.toString();
            }
        });

        dp.gatherResults();
        Assert.assertEquals(MSG, answer[0]);
    }


    @Test(timeout = 500)
    public void testSync() throws Exception {
        String MSG = "Hi";

        String answer = (String) dp.executeSync(MSG, 100);

        Assert.assertEquals("HelloWorld", answer);
    }

    @Test(timeout = 500, expected = RequestException.class)
    public void testSyncError() throws Exception {

        String answer = (String) dp.executeSync("ERROR","REQEST", 100);

    }


}

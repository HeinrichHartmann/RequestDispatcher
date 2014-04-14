package net.hh.request_dispatcher;

import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by hartmann on 4/6/14.
 */
public class ZmqWorkerProxyTest {

    private final String inputChannel = "inproc://proxyinput";
    private final ZMQ.Context ctx = ZMQ.context(0);
    private final ZmqWorkerProxy proxy = new ZmqWorkerProxy(ctx, inputChannel);

    @Test //(timeout = 2000)
    public void testStartStop() throws Exception {

        int NUM_WORKERS = 2;

        for (int i = 0; i < NUM_WORKERS; i++) {
            final int j = i;
            proxy.add(new ZmqWorker<String, String>(
                    new RequestHandler<String, String>() {
                        @Override
                        public String handleRequest(String request) {
                            return "" + j;
                        }
                    })
            );
        }

        proxy.startWorkers();
        proxy.shutdown();

        // proxy does not own the context
        ctx.term();
    }

    @Test // (timeout = 2000)
    public void testProxy() throws Exception {

        int NUM_WORKERS = 5;
        int NUM_REQUESTS = NUM_WORKERS * 3;

        for (int i = 0; i < NUM_WORKERS; i++) {
            final int j = i;
            proxy.add(new ZmqWorker<String, String>(
                    new RequestHandler<String, String>() {
                        @Override
                        public String handleRequest(String request) {
                            return "" + j;
                        }
                    })
            );
        }

        proxy.startWorkers();


        /////////////// SEND REQUESTS /////////////////////

        Dispatcher dp = new Dispatcher(ctx);

        /// SETUP WORKER: String -> String
        dp.registerService(String.class, inputChannel);

        final Set<String> answers = new HashSet<String>(NUM_WORKERS);

        for (int i = 0; i < NUM_REQUESTS; i++) {
            dp.execute("", new Callback<String>() {
                @Override
                public void onSuccess(String reply) {
                    answers.add(reply);
                }
            });
        }

        dp.gatherResults();
        dp.shutdown();

        Thread.sleep(100);

        for (int i = 0; i < NUM_WORKERS; i++) {
            System.out.println("Checking: " + i);
            Assert.assertTrue(answers.contains("" + i));
        }

        proxy.shutdown();

        // proxy does not own the context
        ctx.term();
    }
}

package net.hh.request_dispatcher.worker;

import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.Dispatcher;
import net.hh.request_dispatcher.server.RequestHandler;
import net.hh.request_dispatcher.server.ZmqWorker;
import net.hh.request_dispatcher.server.ZmqWorkerProxy;
import net.hh.request_dispatcher.service_adapter.ZmqAdapter;
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
    private final ZmqWorkerProxy proxy = new ZmqWorkerProxy(inputChannel);
    private final ZMQ.Context ctx = proxy.getContext();

    @Test(timeout = 1000)
    public void testStartStop() throws Exception {

        int NUM_WORKERS = 2;

        for (int i = 0; i < NUM_WORKERS; i++) {
            final int j = i;
            proxy.add(new ZmqWorker<String, String>(
                    null,
                    new RequestHandler<String, String>() {
                        @Override
                        public String handleRequest(String request) {
                            return "" + j;
                        }
                    })
            );
        }

        proxy.startWorkers();

        proxy.doProxyBackground();

        // Thread.sleep(200); // wait a little while

        // proxy.stopWorkers();


    }

    @Test(timeout = 1000)
    public void testProxy() throws Exception {

        int NUM_WORKERS = 10;
        int NUM_REQUESTS = NUM_WORKERS * 3;

        for (int i = 0; i < NUM_WORKERS; i++) {
            final int j = i;
            proxy.add(new ZmqWorker<String, String>(
                    null,
                    new RequestHandler<String, String>() {
                        @Override
                        public String handleRequest(String request) {
                            return "" + j;
                        }
                    })
            );
        }

        proxy.startWorkers();

        proxy.doProxyBackground();

        /////////////// SEND REQUESTS /////////////////////

        Dispatcher dp = new Dispatcher();

        /// SETUP WORKER: String -> String
        dp.registerServiceAdapter(String.class, new ZmqAdapter(ctx, inputChannel));

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
        dp.close();

        Thread.sleep(100);

        for (int i = 0; i < NUM_WORKERS; i++) {
            System.out.println("Checking: " + i);
            Assert.assertTrue(answers.contains("" + i));
        }

        // proxy.stopWorkers();
    }
}

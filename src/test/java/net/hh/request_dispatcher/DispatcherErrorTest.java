package net.hh.request_dispatcher;

import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.Dispatcher;
import net.hh.request_dispatcher.RequestException;
import net.hh.request_dispatcher.RequestHandler;
import net.hh.request_dispatcher.ZmqWorker;
import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ;

/**
 * Created by hartmann on 4/2/14.
 */
public class DispatcherErrorTest {

    @Test(timeout = 1000)
    public void testErrorHandling() throws Exception {
        final String[] answer = new String[3];

        // SETUP

        ZMQ.Context ctx = ZMQ.context(0);
        String channel = "inproc://" + hashCode();


        // Need to construct worker (bind socket) before the dispatcher (connect socket) for inproc
        ZmqWorker<String, String> worker =
                new ZmqWorker<String, String>(
                        ctx,
                        channel,
                        new RequestHandler<String, String>() {
                            @Override
                            public String handleRequest(String request) throws RequestException {
                                answer[0]="ERROR_WORKER";
                                throw new RequestException("ERROR_MSG");
                            }
                        });

        Dispatcher dp = new Dispatcher(ctx);
        dp.registerService(String.class, channel);

        worker.start();

        // LOGIC

        dp.execute("Request", new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                throw new RuntimeException();
            }

            @Override
            public void onError(RequestException e) {
                answer[1]="ERROR_CALLBACK";
                answer[2]=e.getMessage();
            }
        });

        dp.gatherResults();

        // SHUTDOWN
        dp.close();
        ctx.term();
        worker.join();

        Assert.assertEquals("ERROR_WORKER", answer[0]);
        Assert.assertEquals("ERROR_CALLBACK", answer[1]);
        Assert.assertTrue(answer[2].endsWith("ERROR_MSG")); // prefixed by class name
    }
}

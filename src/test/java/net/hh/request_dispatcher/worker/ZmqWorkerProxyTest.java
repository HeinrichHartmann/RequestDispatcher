package net.hh.request_dispatcher.worker;

import junit.framework.Assert;
import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.Dispatcher;
import net.hh.request_dispatcher.server.RequestHandler;
import net.hh.request_dispatcher.server.ZmqWorker;
import net.hh.request_dispatcher.server.ZmqWorkerProxy;
import net.hh.request_dispatcher.service_adapter.ZmqAdapter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.zeromq.ZMQ;

import static org.mockito.Mockito.verify;

/**
 * Created by hartmann on 4/6/14.
 */
@RunWith(MockitoJUnitRunner.class)
public class ZmqWorkerProxyTest {

    private ZMQ.Context ctx = ZMQ.context(1);
    private String inputChannel = "ipc://proxyinput";
    private ZmqWorkerProxy proxy = new ZmqWorkerProxy(inputChannel);

    @Mock
    private ZmqWorker<ReqTO, RepTO> mockWorker;

    @Test
    public void testAddWorkers() throws Exception {
        Assert.assertTrue(proxy.add(mockWorker));

        proxy.startWorkers();

        verify(mockWorker).start();

        Assert.assertTrue(proxy.remove(mockWorker));
    }

    @Test
    public void testProxy() throws Exception {

        for (int i = 0; i < 5; i++) {
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

        for (int i = 0; i < 100; i++) {
            dp.execute("Hi", new Callback<String>() {
                @Override
                public void onSuccess(String reply) {
                    System.out.println(reply);
                }
            });
        }

        dp.gatherResults();

        Thread.sleep(100);

        proxy.stopWorkers();
    }
}

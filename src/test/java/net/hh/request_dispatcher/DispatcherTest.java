package net.hh.request_dispatcher;

import org.junit.*;
import org.zeromq.ZMQ;

/**
 * Created by hartmann on 3/17/14.
 */
public class DispatcherTest {

    private ZMQ.Context ctx = ZMQ.context(0);
    private String echoEndpoint = "inproc://127.0.0.1:60123";
    private EchoServer echoServer;

    private Dispatcher dp;

    @Before
    public void setUp() throws Exception {
        echoServer = new EchoServer(ctx, echoEndpoint);
        echoServer.start();

        // before each Test
        dp = new Dispatcher(ctx);
        dp.registerService(String.class, echoEndpoint);
        dp.registerService(TestDTO.class, echoEndpoint);
    }

    @After
    public void tearDown() throws Exception {
        dp.close(); // close sockets
        ctx.term();
    }

    @Test
    public void testExecute() throws Exception {
        final String[] answer = new String[1];
        dp.execute("hi", new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                answer[0] = reply;
            }
        });
        dp.gatherResults();
        Assert.assertEquals("hi", answer[0]);
    }

    @Test(timeout = 500)
    public void testChainedExecute() throws Exception {
        final String[] answer = new String[2];

        dp.execute("msg1", new Callback<String>() {
            @Override
            public void onSuccess(String reply) {
                answer[0] = reply;
                dp.execute("msg2", new Callback<String>() {
                    @Override
                    public void onSuccess(String reply) {
                        Assert.assertEquals("msg1", answer[0]);
                        answer[1] = reply;
                    }
                });
            }
        });

        dp.gatherResults();

        Assert.assertEquals("msg1", answer[0]);
        Assert.assertEquals("msg2", answer[1]);
    }

    @Test(timeout = 500)
    public void testPromise() throws Exception {
        final String[] answer = new String[3];

        Callback<TestDTO> callback1 = new Callback<TestDTO>() {
            @Override
            public void onSuccess(TestDTO reply) {
                answer[0] = reply.toString();
            }
        };
        dp.execute(new TestDTO("msg1"), callback1);

        Callback<TestDTO> callback2 = new Callback<TestDTO>() {
            @Override
            public void onSuccess(TestDTO reply) {
                answer[1] = reply.toString();
            }
        };
        dp.execute(new TestDTO("msg2"), callback2);

        Callback<TestDTO> callback3 = new Callback<TestDTO>() {
            @Override
            public void onSuccess(TestDTO reply) {
                throw new RuntimeException();
            }
        };
        // do not execute this callback

        dp.promise(new Runnable() {
            @Override
            public void run() {
                Assert.assertEquals("msg1", answer[0]);
                Assert.assertEquals("msg2", answer[1]);
                answer[2] = "promise";
            }
        },
                callback1, callback2
        );

        dp.promise(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException();
            }
        }, callback3
        );

        dp.gatherResults();

        Assert.assertEquals("msg1", answer[0]);
        Assert.assertEquals("msg2", answer[1]);
        Assert.assertEquals("promise", answer[2]);
    }

    @Test(timeout = 500)
    public void testEmptyPromise() throws Exception {
        final String[] answer = new String[3];

        dp.promise(new Runnable() {
            @Override
            public void run() {
                answer[0] = "unconditionalPromise";
                dp.execute(new TestDTO(""), new Callback<TestDTO>() {
                    @Override
                    public void onSuccess(TestDTO reply) {
                        answer[1] = "dependentCallback";
                    }
                });
            }
        }
        );

        dp.gatherResults();

        Assert.assertEquals("unconditionalPromise", answer[0]);
        Assert.assertEquals("dependentCallback", answer[1]);
    }

    @Test
    public void testIntByteConv() throws Exception {
        for (int i = -5000; i < 5000; i += 235) {
            Assert.assertEquals(i, TransferHelper.bytes2int(TransferHelper.int2bytes(i)));
        }
    }
}



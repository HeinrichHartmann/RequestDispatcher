package net.hh.RequestDispatcher.Server;

import net.hh.RequestDispatcher.Callback;
import net.hh.RequestDispatcher.Dispatcher;
import net.hh.RequestDispatcher.Service.ZmqService;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestReply;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestRequest;
import org.apache.log4j.BasicConfigurator;
import org.junit.*;

/**
 * Created by hartmann on 3/17/14.
 */
public class DispatcherTest {

    static EchoServer echoServer;
    static String echoEndpoint = "tcp://127.0.0.1:60123";

    @BeforeClass
    public static void setupMockServer() throws Exception {
        BasicConfigurator.configure();


        echoServer = new EchoServer(echoEndpoint);
        echoServer.start();
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        EchoServer.term();
        echoServer.stop();

        ZmqService.term();
    }

    Dispatcher dp;

    @Before
    public void setUp() throws Exception {
        // before each Test
        dp = new Dispatcher();
        dp.registerServiceProvider("ECHO", new ZmqService(echoEndpoint));
        dp.setDefaultService(TestRequest.class, "ECHO");
    }

    @After
    public void tearDown() throws Exception {
        dp.close(); // close sockets
    }

    @Test
    public void testExecute() throws Exception {
        final String[] answer = new String[1];
        dp.execute(new TestRequest("hi"), new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                answer[0] = reply.serialize();
            }
        });
        dp.gatherResults();
        Assert.assertEquals("hi", answer[0]);
    }

    @Test
    public void chainedExecute() throws Exception {
        final String[] answer = new String[2];

        dp.execute(new TestRequest("msg1"), new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                answer[0] = reply.serialize();
                dp.execute(new TestRequest("msg2"), new Callback<TestReply>(new TestReply()) {
                    @Override
                    public void onSuccess(TestReply reply) {
                        Assert.assertEquals("msg1", answer[0]);
                        answer[1] = reply.serialize();
                    }
                });
            }
        });

        dp.gatherResults();

        Assert.assertEquals("msg1", answer[0]);
        Assert.assertEquals("msg2", answer[1]);
    }

    @Test
    public void testPromise() throws Exception {
        final String[] answer = new String[3];

        Callback<TestReply> callback1 = new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                answer[0] = reply.serialize();
            }
        };
        dp.execute(new TestRequest("msg1"), callback1);

        Callback<TestReply> callback2 = new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                answer[1] = reply.serialize();
            }
        };
        dp.execute(new TestRequest("msg2"), callback2);

        Callback<TestReply> callback3 = new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
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

    @Test
    public void testEmptyPromise() throws Exception {
        final String[] answer = new String[3];

        dp.promise(new Runnable() {
            @Override
            public void run() {
                answer[0] = "unconditionalPromise";

                dp.execute(new TestRequest(""), new Callback<TestReply>(new TestReply()) {
                    @Override
                    public void onSuccess(TestReply reply) {
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

}



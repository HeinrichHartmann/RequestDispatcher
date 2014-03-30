package net.hh.request_dispatcher;

import net.hh.request_dispatcher.Server.EchoServer;
import net.hh.request_dispatcher.service.ZmqService;
import net.hh.request_dispatcher.transfer.test_service.TestDTO;
import org.junit.*;
import org.zeromq.ZMQ;

/**
 * Created by hartmann on 3/17/14.
 */
public class DispatcherTestTimeout {

    private static final int DURATION = 10000;
    private static final int GRACE = 1000;

    static EchoServer echoServer;
    static String echoEndpoint = "inproc://127.0.0.1:60123";
    static ZMQ.Context ctx = ZMQ.context(0);

    @BeforeClass
    public static void setupMockServer() throws Exception {
        // BasicConfigurator.configure();

        echoServer = new EchoServer(ctx, echoEndpoint);
        echoServer.setDelay(DURATION);
        echoServer.start();
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        echoServer.stop();
    }

    Dispatcher dp;

    @Before
    public void setUp() throws Exception {
        // before each Test
        dp = new Dispatcher();
        dp.registerServiceProvider("ECHO", new ZmqService(ctx, echoEndpoint));
        dp.setDefaultService(TestDTO.class, "ECHO");
    }

    @After
    public void tearDown() throws Exception {
        dp.close(); // close sockets
    }

    private final String TIMEOUT_MSG = "TIMEOUT";

    @Test
    public void testTimeOut() throws Exception {

        final String[] answer = new String[1];

        dp.execute(new TestDTO(), new Callback<TestDTO>(new TestDTO()) {
            @Override
            public void onSuccess(TestDTO reply) {
                throw new RuntimeException("Not Timed out");
            }

            @Override
            public void onTimeOut(String errorMessage) {
                answer[0] = TIMEOUT_MSG;
            }
        });

        // set too short timeout
        dp.gatherResults(1);

        Assert.assertEquals(TIMEOUT_MSG, answer[0]);
    }


    @Test(timeout = DURATION + 5 * GRACE)
    public void testTimeOk() throws Exception {
        final String[] answer = new String[1];

        dp.execute(new TestDTO(), new Callback<TestDTO>(new TestDTO()) {
            @Override
            public void onSuccess(TestDTO reply) {
                answer[0] = "OK";
            }

            @Override
            public void onTimeOut(String errorMessage) {
                throw new RuntimeException("Timed Out");
            }
        });

        // set graceful timeout
        dp.gatherResults(DURATION + GRACE);
        Assert.assertEquals("OK", answer[0]);
    }
}

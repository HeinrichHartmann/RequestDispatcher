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
public class DispatcherTestTimeout {

    private static int DURATION = 1000;
    private static int GRACE = 100;

    static EchoServer echoServer;
    static String echoEndpoint = "tcp://127.0.0.1:60123";

    @BeforeClass
    public static void setupMockServer() throws Exception {
        BasicConfigurator.configure();

        echoServer = new EchoServer(echoEndpoint);
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
        dp.registerServiceProvider("ECHO", new ZmqService(echoEndpoint));
        dp.setDefaultService(TestRequest.class, "ECHO");
    }

    @After
    public void tearDown() throws Exception {
        dp.close(); // close sockets
    }

    private final String TIMEOUT_MSG = "TIMEOUT";

    // @Test
    public void testTimeOut() throws Exception {

        final String[] answer = new String[1];

        dp.execute(new TestRequest(), new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
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


    @Test(timeout = 1000)
    public void testTimeOk() throws Exception {
        final String[] answer = new String[1];

        dp.execute(new TestRequest(), new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
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

package net.hh.RequestDispatcher.Server;

import net.hh.RequestDispatcher.Callback;
import net.hh.RequestDispatcher.Dispatcher;
import net.hh.RequestDispatcher.Service.ZmqService;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestReply;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestRequest;
import org.junit.*;

/**
 * Created by hartmann on 3/17/14.
 */
public class DispatcherTestTimeout {

    static EchoServer echoServer;
    static String echoEndpoint = "tcp://127.0.0.1:60123";

    @BeforeClass
    public static void setupMockServer() throws Exception {
        echoServer = new EchoServer(echoEndpoint);
        echoServer.setDuration(100);
        echoServer.start();
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        EchoServer.term();
        echoServer.stop();

        ZmqService.term(); // term context
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

    private final String PAYLOAD = "HI";
    private final String TIMEOUT_MSG = "TIMEOUT";

    @Test
    public void testTimeOut() throws Exception {
        final String[] answer = new String[1];
        dp.execute(new TestRequest(PAYLOAD), new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                answer[0] = reply.serialize();
            }

            @Override
            public void onTimeOut(String errorMessage) {
                answer[0] = TIMEOUT_MSG;
            }
        });

        // set too short timeout
        dp.gatherResults(50);
        Assert.assertEquals(TIMEOUT_MSG, answer[0]);
    }


    @Test
    public void testTimeOk() throws Exception {
        final String[] answer = new String[1];
        dp.execute(new TestRequest(PAYLOAD), new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                answer[0] = reply.serialize();
            }

            @Override
            public void onTimeOut(String errorMessage) {
                answer[0] = TIMEOUT_MSG;
            }
        });

        // set too short timeout
        dp.gatherResults(200);
        Assert.assertEquals(PAYLOAD, answer[0]);
    }
}

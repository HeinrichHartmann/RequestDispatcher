package net.hh.RequestDispatcher;

import net.hh.RequestDispatcher.Server.EchoServer;
import net.hh.RequestDispatcher.Service.ZmqService;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestReply;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestRequest;
import org.junit.*;

/**
 * Created by hartmann on 3/17/14.
 */
public class DispatcherTest {

    static EchoServer echoServer;
    static String echoEndpoint = "tcp://127.0.0.1:60123";

    @BeforeClass
    public static void setupMockServer() throws Exception {
        echoServer = new EchoServer(echoEndpoint, "");
        echoServer.start();
    }

    @AfterClass
    public static void stopMockServer() throws Exception {
        EchoServer.term();
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
        ZmqService.term(); // term context
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
}

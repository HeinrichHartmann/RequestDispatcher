package net.hh.RequestDispatcher.Service;

import net.hh.RequestDispatcher.Server.EchoServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by hartmann on 3/15/14.
 */
public class ZmqServiceTest {

    EchoServer A = new EchoServer("tcp://*:61001", "A");
    EchoServer B = new EchoServer("tcp://*:61001", "B");

    @Before
    public void setUp() throws Exception {
        A.start();
        B.stop();
    }

    @After
    public void tearDown() throws Exception {
        A.stop();
        B.stop();
    }

    @Test
    public void testTerm() throws Exception {

    }

}

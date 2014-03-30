package net.hh.request_dispatcher.Server;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Created by hartmann on 3/21/14.
 */
public class PrintServerTest {

    static {
        Logger.getLogger(PrintServer.class).setLevel(Level.DEBUG);
        BasicConfigurator.configure();
    }

    @Test// (timeout = 100)
    public void testPrint() throws Exception {
        ZMQ.Context ctx = ZMQ.context(1);
        ZMQ.Socket  snd = ctx.socket(ZMQ.PUSH);
        ZMQ.Socket  recv = ctx.socket(ZMQ.PULL);

        String channel = "inproc://127.0.0.1:60153";

        snd.bind(channel);
        recv.connect(channel);

        ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
        PrintStream  out          = new PrintStream(outputBuffer);

        Thread t = new PrintServer(recv, out);
        t.start();

        String MSG = "Lorem Ipsum";

        snd.send(MSG);
        snd.close();

        ctx.term();
        t.join();

        Assert.assertEquals(MSG + "\n", outputBuffer.toString());
    }
}

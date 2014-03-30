package net.hh.request_dispatcher.mock_server;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.io.PrintStream;

/**
 * Prints messages from socket to outputStream
 *
 * Created by hartmann on 3/21/14.
 */
public class PrintServer extends Thread {

    private static final Logger log = Logger.getLogger(PrintServer.class);

    ZMQ.Socket socket;
    PrintStream outputStream;

    /**
     * Constructs Server Object.
     * @param socket        to listen on
     * @param outputStream  to write to
     */
    public PrintServer(ZMQ.Socket socket, PrintStream outputStream) {
        this.socket = socket;
        this.outputStream = outputStream;
    }

    /**
     * Prints messages of socket to System.out
     * @param socket
     */
    public PrintServer(ZMQ.Socket socket) {
        this(socket, System.out);
    }

    /**
     * Print messages to output stream.
     * Socket gets closed on ETERM.
     */
    @Override
    public void run() {
        try {
            log.info("Starting print loop.");
            String msg = null;
            while (true) {
                msg = socket.recvStr();
                log.debug("Received " + msg);
                outputStream.println(msg);
            }
        } catch (ZMQException e) {
            log.info("Catched ETERM. Closing socket. ");
            socket.close();
            outputStream.flush();
        }
    }

    public static void main(String[] args) {
        BasicConfigurator.configure();
        ZMQ.Context ctx = ZMQ.context(1);
        ZMQ.Socket  in  = ctx.socket(ZMQ.PULL);
        in.setReceiveBufferSize(0);
        in.setHWM(1);
        in.bind("tcp://127.0.0.1:60153");
        new PrintServer(in).run();
        ctx.term();
    }

}

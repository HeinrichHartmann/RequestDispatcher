package net.hh.RequestDispatcher.Server;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.List;

/**
 * ZMQ ReqReply Server that returns the [multipart] message that was received to the sender.
 */
public class EchoServer {

    private static final Logger log = Logger.getLogger(EchoServer.class);

    private static final ZMQ.Context ctx = ZMQ.context(1);

    private final String endpoint;

    private int duration;

    /**
     * @param endpoint to listen on
     */
    public EchoServer(String endpoint) {
        this.endpoint = endpoint;
    }

    /**
     * Set Thread.sleep() delay for each message
     * @param duration
     */
    public void setDuration(int duration){
        this.duration = duration;
    };

    /**
     * Start Request/Reply Loop
     */
    public void serve() {
        log.info("Starting echo server");

        log.debug("Test with: zmqdump REQ \"" + endpoint + "\"");
        ZMQ.Socket socket = ctx.socket(ZMQ.REP);
        socket.bind(endpoint);

        // socket.setReceiveTimeOut(5000);

        while (!Thread.currentThread().isInterrupted()) {
            log.info("Listening on " + endpoint);

            try {

                ZMsg msg = ZMsg.recvMsg(socket);

                log.info("Received Message " + printMsg(msg));

                if (msg == null) {
                    break;
                }

                Thread.sleep(duration);

                msg.send(socket);

            } catch (ZMQException e) {
                if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                    break;
                }
            } catch (InterruptedException e) {
                // interrupted sleep
                break;
            }
        }

        System.out.println("Closing sockets");
        socket.close();
    }

    private String printMsg(ZMsg msg) {
        List<String> out = new ArrayList<String>();

        for (ZFrame frame : msg){
            out.add(frame.toString());
        }

        return "[" + StringUtils.join(out, ",") + "]";
    }

    /////////////// THREAD HANDLING /////////////////////
    private Thread thread = new Thread(new Runnable() {
        public void run() {
            serve();
        }
    });

    public void start() {
        thread.start();
    }

    public void stop() {
        try {
            thread.interrupt();
            thread.join();
            System.out.println("Terminated Server Thread");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void term() {
        System.out.println("Terminating Context");
        ctx.term();
    }

    public void join() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

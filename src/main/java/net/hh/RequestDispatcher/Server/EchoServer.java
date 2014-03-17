package net.hh.RequestDispatcher.Server;

import org.jeromq.ZFrame;
import org.jeromq.ZMQ;
import org.jeromq.ZMQException;
import org.jeromq.ZMsg;

/**
 * Mock ZMQ ReqReply Server
 */
public class EchoServer {

    private static final ZMQ.Context ctx = ZMQ.context(1);

    private final String responsePrefix;

    private final String endpoint;

    private int duration;

    public EchoServer(
            String endpoint,
            String responsePrefix) {
        this.responsePrefix = responsePrefix;
        this.endpoint = endpoint;
    }

    public void setDuration(int duration){
        this.duration = duration;
    };

    public void serve() {
        System.out.println("Starting echo server on " + endpoint);
        System.out.println("Test with: zmqdump REQ \"" + endpoint+ "\"");
        ZMQ.Socket socket = ctx.socket(ZMQ.REP);
        socket.bind(endpoint);
        // socket.setReceiveTimeOut(5000);

        while (!Thread.currentThread().isInterrupted()) {
            System.out.println("Listening on " + endpoint);

            try {
                ZMsg msg = ZMsg.recvMsg(socket);

                System.out.print("Received: | ");
                for (ZFrame frame : msg){
                    System.out.print(frame.toString() + " | ");
                }
                System.out.println("");

                if (msg == null) {
                    continue;
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

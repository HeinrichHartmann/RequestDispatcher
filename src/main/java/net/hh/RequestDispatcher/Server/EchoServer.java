package net.hh.RequestDispatcher.Server;

import org.jeromq.ZMQ;
import org.jeromq.ZMQException;

/**
 * Mock ZMQ ReqReply Server
 */
public class EchoServer {

    private static final ZMQ.Context ctx = ZMQ.context(1);

    private final String responsePrefix;
    private final String endpoint;

    public EchoServer(String endpoint, String responsePrefix){
        this.responsePrefix = responsePrefix;
        this.endpoint = endpoint;
    }

    public void serve() {

        ZMQ.Socket socket = ctx.socket(ZMQ.REP);
        socket.bind(endpoint);
        socket.setReceiveTimeOut(5000);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                String m = socket.recvStr();
                if (m == null) { continue; }

                socket.send(responsePrefix + ":" + m);
            } catch (ZMQException e) {
                if (e.getErrorCode () == ZMQ.Error.ETERM.getCode ()) {
                    break;
                }
            }
        }

        socket.close();

        System.out.println("Terminated Server");
    }

    private Thread thread = new Thread(new Runnable() {
        @Override
        public void run() {
            serve();
        }
    });

    public void start() {
        thread.start();
    }

    public void stop(){
        try {
            thread.interrupt();
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void term(){
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

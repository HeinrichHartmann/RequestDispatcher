package net.hh.RequestDispatcher;

import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class TermOnSend {
    public static void main(String[] args) {
        final ZMQ.Context ctx = ZMQ.context(1);

        Thread threadB = new Thread(new Runnable() {
            @Override
            public void run() {
                ZMQ.Socket socket = ctx.socket(ZMQ.PUSH);

                try {
                    socket.send("Some message"); // blocks
                } catch (ZMQException e) {
                    socket.setLinger(0); // throws zmq.ZError$CtxTerminatedException
                    socket.close();
                }
            }
        });

        threadB.start();

        // give thread B some time to start up
        try {
            Thread.sleep(20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ctx.term(); // blocks and does not return
    }
}
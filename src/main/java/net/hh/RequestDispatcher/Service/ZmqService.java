package net.hh.RequestDispatcher.Service;

import java.util.ArrayList;

import org.jeromq.ZMQ;

public class ZmqService implements Service {

    private static final ZMQ.Context ctx = ZMQ.context(1);

    private static int threads = 0;

    private final ZMQ.Socket socket;

    protected String endpoint;

    public ZmqService(
            String endpoint) {
        this.endpoint = endpoint;
        socket = ctx.socket(ZMQ.DEALER);
        socket.connect(endpoint);
    }

    /**
     * Terminate ZMQ Context.Starting service
     * 
     * Need to be called before sockets are closed.
     * Blocking calls return with a ZMQError.
     */
    public static void term() {
        ctx.term();
    }

    public void close() {
        socket.close();
    }

    public void send(String[] m) {
        sendMultipart(socket, m);
    }

    public String[] recv() {
        return recvMultipart(socket);
    }

    public static ZMQ.Poller getPoller() {
        return ctx.poller();
    }

    public ZMQ.Socket getSocket() {
        return socket;
    }

    // HELPER METHODS

    private synchronized void sendMultipart(ZMQ.Socket socket, String[] multipart) {
        if (multipart.length == 0) return;

        // Empty Request Envelope
        socket.send(new byte[0], ZMQ.SNDMORE);

        int i = 0;
        for (; i < multipart.length - 1; i++) {
            System.out.println("Sending " + multipart[i]);
            socket.sendMore(multipart[i]);
        }

        System.out.println("Sending " + multipart[i]);
        socket.send(multipart[i]);
    }

    private synchronized String[] recvMultipart(ZMQ.Socket socket) {
        ArrayList<String> buffer = new ArrayList<String>(1);

        // Discard Reply Envelope
        socket.recv();

        // buffer.add(socket.recvStr());
        while (socket.hasReceiveMore()) {
            buffer.add(socket.recvStr());
        }

        String[] out = new String[buffer.size()];
        for (int i = 0; i < buffer.size(); i++) {
            out[i] = buffer.get(i);
            System.out.println("Received " + buffer.get(i));
        }

        return out;
    }

}

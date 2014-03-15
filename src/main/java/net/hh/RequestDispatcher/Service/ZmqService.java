package net.hh.RequestDispatcher.Service;

import org.jeromq.ZMQ;

import java.util.ArrayList;

public class ZmqService extends Service {

    private static final ZMQ.Context ctx = ZMQ.context(1);

    private final ZMQ.Socket socket;

    protected String endpoint;

    public ZmqService(String endpoint){
        this.endpoint = endpoint;
        this.socket   = ctx.socket(ZMQ.DEALER);
        this.socket.connect(endpoint);
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

    @Override
    public void close(){
        this.socket.close();
    }

    @Override
    public void send(String[] m) {
        sendMultipart(socket, m);
    }

    @Override
    public String[] recv() {
        return recvMultipart(socket);
    }

    public static ZMQ.Poller getPoller(){
        return ctx.poller();
    }

    public ZMQ.Socket getSocket(){
        return socket;
    }

    // HELPER METHODS

    private void sendMultipart(ZMQ.Socket socket, String[] multipart) {
        if (multipart.length == 0) return;

        // Empty Request Envelope
        socket.send(new byte[0], ZMQ.SNDMORE);

        int i = 0;
        for ( ; i < multipart.length - 1; i++){
            System.out.println("Sending " + multipart[i]);
            socket.sendMore(multipart[i]);
        }

        System.out.println("Sending " + multipart[i]);
        socket.send(multipart[i]);
    }

    private String[] recvMultipart(ZMQ.Socket socket) {
        ArrayList<String> buffer = new ArrayList<String>(1);

        // Discard Reply Envelope
        socket.recv();

        // buffer.add(socket.recvStr());
        while (socket.hasReceiveMore()) {
            buffer.add(socket.recvStr());
        }

        String [] out = new String[buffer.size()];
        for (int i = 0; i < buffer.size(); i++){
            out[i] = buffer.get(i);
            System.out.println("Received " + buffer.get(i));
        }

        return out;
    }

}

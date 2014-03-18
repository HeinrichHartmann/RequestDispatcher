package net.hh.RequestDispatcher.Service;

import org.apache.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.Arrays;

public class ZmqService implements Service {

    private static final Logger log = Logger.getLogger(ZmqService.class);

    private static final ZContext zCtx = new ZContext(1);

    private final ZMQ.Socket socket;

    protected String endpoint;

    public ZmqService(String endpoint) {
        log.debug("Setup ZmqService with DEALER socket for endpoint " + endpoint);

        this.endpoint = endpoint;
        socket = zCtx.createSocket(ZMQ.DEALER);
        socket.connect(endpoint);
    }

    /**
     * Terminate ZMQ Context and terminate all sockets.
     */
    public static void term() {
        log.info("Destroying context.");
        zCtx.destroy();
    }

    @Deprecated
    public void close() {
        zCtx.destroySocket(socket);
    }

    public void send(String[] mmsg) {
        log.debug("Sending message " + Arrays.asList(mmsg));

        ZMsg frames = ZMsg.newStringMsg(mmsg);

        // Add empty frame as REQ envelope
        frames.push(new byte[0]);

        frames.send(socket);
    }

    public String[] recv() {
        ZMsg mmsg = ZMsg.recvMsg(socket);

        mmsg.pollFirst();


        String [] out = new String[mmsg.size()];


        // copy mmsg.size() since it is changed in the loop at pollFirst()
        int len = mmsg.size();
        for(int i=0; i < len; i++) {

            out[i] = mmsg.pollFirst().toString();
        }

        return out;
    }

    public static ZMQ.Poller getPoller() {

        // Work around bug in zContext. See pull request:
        // https://github.com/zeromq/zeromq/pull/145
        if (zCtx.getContext() == null) {
            zCtx.createSocket(ZMQ.PAIR);
        }

        return zCtx.getContext().poller();
    }

    public ZMQ.Socket getSocket() {
        return socket;
    }

    //////////////////// HELPER METHODS /////////////////////
    private synchronized void sendMultipart(ZMQ.Socket socket, String[] multipart) {
        if (multipart.length == 0) return;

        // Empty Request Envelope
        socket.send(new byte[0], ZMQ.SNDMORE);

        int i = 0;
        for (; i < multipart.length - 1; i++) {
            socket.sendMore(multipart[i]);
        }

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
        }

        return out;
    }

}

package net.hh.request_dispatcher.service;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.io.Serializable;
import java.util.Arrays;

public class ZmqService<RequestType extends Serializable, ReplyType extends Serializable>  {

    private static final Logger log = Logger.getLogger(ZmqService.class);

    private final ZMQ.Context ctx;

    private final ZMQ.Socket socket;

    protected String endpoint;

    public ZmqService(String endpoint) {
        this(ZMQ.context(1), endpoint);
    }

    public ZmqService(ZMQ.Context ctx, String endpoint) {
        log.debug("Setup ZmqService with DEALER socket for endpoint " + endpoint);

        this.ctx = ctx;

        this.endpoint = endpoint;
        this.socket = ctx.socket(ZMQ.DEALER);

        this.socket.setHWM(1000);
        socket.setLinger(100);
        socket.connect(endpoint);
    }

    public void close() {
        log.info("Closing Sockets.");
        socket.close();
    }

    public void send(ZMsg mmsg) {
        log.debug("Sending message " + Arrays.asList(mmsg));

        // Add empty frame as REQ envelope
        mmsg.push(new byte[0]);

        mmsg.send(socket);
    }

    public ZMsg recv() {
        try {
            ZMsg mmsg = ZMsg.recvMsg(socket);

            // Remove REP envelope
            mmsg.pollFirst();

            return mmsg;
        } catch (ZMQException e) {
            if (e.getErrorCode() == ZMQ.Error.ETERM.getCode() );
            socket.close();
            return null;
        }
    }

    public ZMQ.Socket getSocket() {
        return socket;
    }

}

package net.hh.RequestDispatcher.Service;

import net.hh.RequestDispatcher.ZmqContextHolder;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.util.Arrays;

public class ZmqService implements Service {

    private static final Logger log = Logger.getLogger(ZmqService.class);

    private final ZMQ.Context ctx;

    private final ZMQ.Socket socket;

    protected String endpoint;

    public ZmqService(String endpoint) {
        log.debug("Setup ZmqService with DEALER socket for endpoint " + endpoint);

        this.ctx = ZmqContextHolder.getContext();
        this.endpoint = endpoint;
        this.socket = ctx.socket(ZMQ.DEALER);

        socket.connect(endpoint);
    }

    public void close() {
        log.info("Closing Sockets.");
        this.socket.close();
        ZmqContextHolder.term();
    }

    public void send(String[] mmsg) {
        log.debug("Sending message " + Arrays.asList(mmsg));

        ZMsg frames = ZMsg.newStringMsg(mmsg);

        // Add empty frame as REQ envelope
        frames.push(new byte[0]);

        frames.send(socket);
    }

    public String[] recv() {
        try {
            ZMsg mmsg = ZMsg.recvMsg(socket);

            // Remove REP envelope
            mmsg.pollFirst();

            String [] out = new String[mmsg.size()];

            // copy mesage size() since it is changed in the loop at pollFirst()
            int len = mmsg.size();

            for(int i=0; i < len; i++) {
                out[i] = mmsg.pollFirst().toString();
            }

            return out;
        } catch (ZMQException e) {
            if (e.getErrorCode() == ZMQ.Error.ETERM.getCode() );
            socket.close();
            return null;
        }
    }

    public static ZMQ.Poller getPoller() {
        // generate new poller with default size
        // size is dynamically extended when more sockets are registered.
        return new ZMQ.Poller(5);
    }

    public ZMQ.Socket getSocket() {
        return socket;
    }

}

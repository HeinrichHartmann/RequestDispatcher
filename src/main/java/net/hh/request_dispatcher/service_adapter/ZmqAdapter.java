package net.hh.request_dispatcher.service_adapter;

import net.hh.request_dispatcher.transfer.SerializationHelper;
import org.apache.log4j.Logger;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.io.Serializable;
import java.math.BigInteger;

public class ZmqAdapter implements ServiceAdapter {

    private static final Logger log = Logger.getLogger(ZmqAdapter.class);

    private final ZMQ.Context ctx;

    private final ZMQ.Socket socket;

    protected String endpoint;

    public ZmqAdapter(String endpoint) {
        this(ZMQ.context(1), endpoint);
    }

    public ZmqAdapter(ZMQ.Context ctx, String endpoint) {
        log.debug("Setup ZmqAdapter with DEALER socket for endpoint " + endpoint);

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

    @Override
    public void send(Serializable request, Integer callbackId) {
        ZMsg out = new ZMsg();

        out.push(SerializationHelper.serialize(request));
        out.push(int2bytes(callbackId));
        out.push(new byte[0]); // Add empty frame as REQ envelope

        out.send(socket);
    }


    @Override
    public ReplyWrapper recv() {
        try {
            ZMsg message = ZMsg.recvMsg(socket);
            log.trace("Received message " + message);

            ZFrame[] parts = message.toArray(new ZFrame[3]);

            // Expect message to have three parts:
            // 0. Empty Delimiter Frame
            // 1. Serialized callback ID
            // 2. Serialized payload

            if (parts.length != 3) {
                throw new IllegalArgumentException("Wrong number of Frames. Expected 3.");
            }
            if (parts[0].size() != 0) {
                throw new IllegalStateException("First frame is not empty.");
            }

            return new ReplyWrapper(
                SerializationHelper.deserialize(parts[2].getData()),
                bytes2int(parts[1].getData())
                );
        } catch (ZMQException e) {
            if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()){
                log.info("Received ETERM exepction. Closing socket.");
                socket.close();
                return null;
            } else {
                throw e;
            }
        }
    }

    @Override
    public ZMQ.PollItem getPollItem() {
        return new ZMQ.PollItem(socket, ZMQ.Poller.POLLIN);
    }

    public static byte[] int2bytes(int i) {
        return BigInteger.valueOf(i).toByteArray();
    }

    public static int bytes2int(byte[] data) {
        return new BigInteger(data).intValue();
    }


}

package net.hh.request_dispatcher.service_adapter;

import net.hh.request_dispatcher.transfer.SerializationHelper;
import org.apache.log4j.Logger;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;

public class ZmqAdapter implements ServiceAdapter {

    private static final Logger log = Logger.getLogger(ZmqAdapter.class);

    private final ZMQ.Context ctx;

    private final ZMQ.Socket asyncSocket; // for use in send() / recv()
    private final ZMQ.Socket syncSocket;  // for use in sendSync()
    private final ZMQ.Poller syncPoller = new ZMQ.Poller(1);

    protected String endpoint;

    public ZmqAdapter(String endpoint) {
        this(ZMQ.context(1), endpoint);
    }

    public ZmqAdapter(ZMQ.Context ctx, String endpoint) {
        log.debug("Setup ZmqAdapter with two DEALER sockets for endpoint " + endpoint);

        this.ctx = ctx;
        this.endpoint = endpoint;

        asyncSocket = ctx.socket(ZMQ.DEALER);
        asyncSocket.setLinger(100);
        asyncSocket.setHWM(1000);
        asyncSocket.connect(endpoint);

        syncSocket = ctx.socket(ZMQ.DEALER);
        syncSocket.setLinger(100);
        syncSocket.setHWM(10);
        syncSocket.connect(endpoint);

        syncPoller.register(syncSocket, ZMQ.Poller.POLLIN);
    }

    public void close() {
        log.info("Closing Sockets.");
        asyncSocket.close();
        syncSocket.close();
    }

    @Override
    public void send(Serializable request, Integer callbackId) {
        ZMsg out = new ZMsg();

        out.push(SerializationHelper.serialize(request));
        out.push(int2bytes(callbackId));
        out.push(new byte[0]); // Add empty frame as REQ envelope

        out.send(asyncSocket);
    }


    @Override
    public ReplyWrapper recv() {
        return _recv(asyncSocket);
    }

    @Override
    public Serializable sendSync(Serializable request, int timeout) throws IOException {
        log.debug("Called sendSync()");

        ZMsg out = new ZMsg();
        out.push(SerializationHelper.serialize(request));
        out.push(int2bytes(0)); // Generate a valid Callback ID
        out.push(new byte[0]);  // Add empty frame as REQ envelope
        out.send(syncSocket);

        log.trace("Message sent. Polling.");
        int recvCount = syncPoller.poll(timeout);

        if (recvCount == 0) return null; // timeout

        ReplyWrapper answer = _recv(syncSocket);

        return answer.getPayload();
    }


    private ReplyWrapper _recv(ZMQ.Socket socket) {
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
        return new ZMQ.PollItem(asyncSocket, ZMQ.Poller.POLLIN);
    }

    public static byte[] int2bytes(int i) {
        return BigInteger.valueOf(i).toByteArray();
    }

    public static int bytes2int(byte[] data) {
        return new BigInteger(data).intValue();
    }



}

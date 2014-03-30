package net.hh.request_dispatcher.worker;

import net.hh.request_dispatcher.transfer.SerializationHelper;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.io.Serializable;

/**
 * ZmqWorker Class
 *
 * Handles (de-)serialization of ZmqMessages on REP socket,
 * and forwards requests to RequestHandler object.
 *
 * @param <RequestType> Type of incoming requests to requestHandler
 * @param <ReplyType>   Type of replies from requestHandler
 */
public class ZmqWorker<RequestType extends Serializable, ReplyType extends Serializable> extends Thread {

    private static final Logger log = Logger.getLogger(ZmqWorker.class);

    private final RequestHandler<RequestType, ReplyType> handler;
    private final ZMQ.Socket socket;

    public ZmqWorker(ZMQ.Socket socket, RequestHandler<RequestType,ReplyType> handler) {
        this.socket = socket;
        this.handler = handler;
    }

    /**
     * Creates ZMQ Worker Thread object.
     * Socket is closed when the context is terminated.
     *
     * @param ctx
     * @param endpoint
     * @param handler
     */
    public ZmqWorker(ZMQ.Context ctx, String endpoint, RequestHandler<RequestType,ReplyType> handler){
        this(ctx.socket(ZMQ.ROUTER), handler);
        socket.setLinger(1000);
        socket.setHWM(1000);
        socket.bind(endpoint);
    }

    ///////////////////// MESSAGING //////////////////////////

    private ZMsg lastMsg = new ZMsg();

    /**
     * Read message from socket.
     * Side: Strips of id envelopes and stores for future use.
     *
     * @return payload containing the received data
     */
    private byte[] recvPayload() {
        lastMsg = ZMsg.recvMsg(socket);

        log.debug("Received message " + lastMsg);

        return lastMsg.pollLast().getData();
    }

    /**
     * Send binary serialized message,
     * to the last id stored by the recvPayload() method.
     *
     * @param payload
     */
    private void sendPayload(byte[] payload) {
        lastMsg.addLast(payload);
        log.debug("Sending message " + lastMsg);

        lastMsg.send(socket);
    }

    ////////////////////// WORKER LOGIC ////////////////////////

    @Override
    public void run() {
        log.info("Called run() on " + this);

        byte[] IdEnvelope;
        byte[] requestPayload;
        RequestType request;
        ReplyType reply;

        while (true) {
            try {
                requestPayload = recvPayload();

                if (requestPayload == null) {
                    log.warn("Received empty payload");
                    continue;
                }

                request = (RequestType) SerializationHelper.deserialize(requestPayload);

                reply = handler.handleRequest(request);

                byte[] replyPayload = SerializationHelper.serialize(reply);

                sendPayload(replyPayload);

            } catch (ClassCastException e) {
                    log.error("Cannot cast request object", e);
            } catch (ZMQException e) {
                if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                    log.info("Received ETERM.");
                    break;
                }
            }
        }
        log.info("Closing socket.");
        socket.close();
    }

    public void close() {
        socket.close();
    }
}

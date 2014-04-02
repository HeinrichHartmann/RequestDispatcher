package net.hh.request_dispatcher.server;

import net.hh.request_dispatcher.transfer.SerializationHelper;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.io.Serializable;

/**
 * ZmqWorker Class
 *
 * Handles (de-)serialization of ZmqMessages on REP socket
 * and forwards requests to RequestHandler object.
 *
 * @param <RequestType> Type of incoming requests to requestHandler
 * @param <ReplyType>   Type of replies from requestHandler
 */
public class ZmqWorker<RequestType extends Serializable, ReplyType extends Serializable>
        extends Thread implements AutoCloseable {



    private static final Logger log = Logger.getLogger(ZmqWorker.class);

    private final RequestHandler<RequestType, ReplyType> handler;
    private final ZMQ.Socket socket;

    /**
     * Creates ZmqWorker Thread object.
     * When run() is called, worker thread listens on socket and
     *
     * @param socket
     * @param handler
     */
    public ZmqWorker(final ZMQ.Socket socket, final RequestHandler<RequestType,ReplyType> handler) {
        super("ZmqWorker");
        this.socket = socket;
        this.handler = handler;
    }

    /**
     * Creates ZMQ Worker Thread object
     *
     * A Router socket is created at the given endpoint. Socket is closed when
     * the context is terminated.
     *
     * When run() is called, worker thread listens on socket, de-serializes
     * incoming request, passes the request object to the request handler,
     * serializes the result and sends it back to the origin of the request.
     *
     * @param ctx       enclosing Zmq Context
     * @param endpoint  to listen on for messages
     * @param handler   requests are passed to handleRequest() method.
     */
    public ZmqWorker(final ZMQ.Context ctx, final String endpoint, final RequestHandler<RequestType,ReplyType> handler)
    {
        this(ctx.socket(ZMQ.ROUTER), handler);
        socket.setLinger(1000);
        socket.setHWM(1000);
        socket.bind(endpoint);
        this.setName("ZmqWorker{" + endpoint + "}");
    }


    /**
     * Closes Zmq Socket.
     */
    @Override
    public void close()
    {
        socket.close();
    }

    ///////////////////// MESSAGING //////////////////////////

    private ZMsg lastMsg = new ZMsg();

    /**
     * Read multipart-message from socket.
     * Returns the last message frame as binary blop.
     *
     * Side: Stores the remaining part of the message for use in sendPayload()
     *
     * @return payload containing the received data
     */
    private byte[] recvPayload() {
        lastMsg = ZMsg.recvMsg(socket);
        log.debug("Received message " + lastMsg);
        return lastMsg.pollLast().getData();
    }

    /**
     * Send payload to peer.
     *
     * Restores request envelope by appending the contents the payload
     * last part to stored multipart message.
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

        byte[] requestPayload = new byte[0];
        RequestType request = null;
        ReplyType reply = null;
        byte[] replyPayload = new byte[0];

        while (true) {
            try {
                //TODO: Better exception handling
                requestPayload = recvPayload();

                if (requestPayload == null) {
                    log.warn("Received empty payload");
                    continue;
                }

                request = (RequestType) SerializationHelper.deserialize(requestPayload);

                try {

                    reply = handler.handleRequest(request);
                    replyPayload = SerializationHelper.serialize(reply);

                } catch (Exception e) {
                    log.warn("Catched exception. Sending back to client.", e);
                    replyPayload = SerializationHelper.serialize(new RequestException(e));
                }

                sendPayload(replyPayload);

            } catch (ClassCastException e) {
                log.error("Cannot cast request object", e);
                // continue with next request
            } catch (ZMQException e) {
                if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                    log.info("Received ETERM.");
                    break;
                } else {
                    log.info("Received ZMQException", e);
                    break;
                }
            }
        }
        log.info("Closing socket.");
        socket.close();
    }
}


package net.hh.request_dispatcher;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.io.Serializable;

/**
 * ZmqWorker Class
 * <p/>
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

    private ZMQ.Socket workSocket = null;        // mutable. Can be changed by setSocket()
    private ZMQ.Socket controlSocket = null; //mutable.

    /**
     * @param handler method to execute on work
     */
    public ZmqWorker(final RequestHandler<RequestType, ReplyType> handler) {
        super("ZmqWorker");
        this.handler = handler;
    }

    /**
     * @param workSocket    socket to listen for work
     * @param controlSocket socket to listen for control messages
     * @param handler       method to execute on work
     */
    public ZmqWorker(final ZMQ.Socket workSocket, final ZMQ.Socket controlSocket, final RequestHandler<RequestType, ReplyType> handler) {
        super("ZmqWorker");
        this.workSocket = workSocket;
        this.controlSocket = controlSocket;
        this.handler = handler;
    }

    /**
     * Creates ZMQ Worker Thread object
     * <p/>
     * A Router socket is created at the given endpoint. Socket is closed when
     * the context is terminated.
     * <p/>
     * When run() is called, worker thread listens on socket, de-serializes
     * incoming request, passes the request object to the request handler,
     * serializes the result and sends it back to the origin of the request.
     *
     * @param ctx          enclosing Zmq Context
     * @param workEndpoint to listen on for messages
     * @param handler      requests are passed to handleRequest() method.
     */
    public ZmqWorker(final ZMQ.Context ctx,
                     final String workEndpoint,
                     final RequestHandler<RequestType, ReplyType> handler) {
        this(ctx.socket(ZMQ.ROUTER), ctx.socket(ZMQ.SUB), handler);
        workSocket.setLinger(1000);
        workSocket.setHWM(1000);
        workSocket.bind(workEndpoint);
        this.setName("ZmqWorker{" + workEndpoint + "}");
    }

    /**
     * Side: Closes previously set socket.
     */
    public void replaceWorkSocket(ZMQ.Socket socket) {
        if (workSocket != null) this.workSocket.close();
        this.workSocket = socket;
    }

    /**
     * Side: Closes previously set socket.
     */
    public void replaceControlSocket(ZMQ.Socket socket) {
        if (controlSocket != null) this.controlSocket.close();
        this.controlSocket = socket;

    }

    /**
     * Closes Zmq Socket.
     * <p/>
     * WARNING: This does not necessarily shut down the worker thread.
     */
    @Override
    public void close() {
        closeSockets();
    }

    private void closeSockets() {
        workSocket.close();
        controlSocket.close();
    }

    ////////////////////// WORKER LOGIC ////////////////////////

    @Override
    public void run() {
        try {
            log.info("Called run() on " + this);
            if (workSocket == null || controlSocket == null) {
                throw new IllegalStateException("Sockets not initialized");
            }

            ZMQ.Poller poller = new ZMQ.Poller(2);

            ZMQ.PollItem payloadPoller = new ZMQ.PollItem(workSocket, ZMQ.Poller.POLLIN);
            poller.register(payloadPoller);

            ZMQ.PollItem controlPoller = new ZMQ.PollItem(controlSocket, ZMQ.Poller.POLLIN);
            poller.register(controlPoller);

            // Loop variables
            Serializable reply = null;

            while (!Thread.interrupted()) {
                log.trace("Waiting for messages.");
                poller.poll();

                if (payloadPoller.isReadable()) {
                    try {
                        TransferWrapper wrappedRequest = TransferHelper.recvMessage(workSocket,ZMQ.NOBLOCK);
                        log.trace("Received message " + wrappedRequest);

                        try {
                            reply = handler.handleRequest((RequestType) wrappedRequest.getObject());
                        } catch (Exception e) {
                            log.warn("Catched exception. Sending back to client.", e);
                            reply = new RequestException(e);
                        }

                        if (wrappedRequest.isOneWayRequest()) {
                            log.debug("Received one way request. No reply sent.");
                            continue;
                        }

                        log.trace("Sending message " + reply);
                        TransferHelper.sendMessage(
                                workSocket,
                                wrappedRequest.constructReply(reply)
                        );
                    } catch (ClassCastException e) {
                        log.error("Cannot cast request object", e);
                        // continue with next request
                    } catch (TransferHelper.ProtocolException | SerializationException e) {
                        log.error(e);
                    } catch (TransferHelper.ZmqEtermException e) {
                        log.error(e);
                        break;
                    }
                } else if (controlPoller.isReadable()) {
                    String CMD = controlSocket.recvStr();
                    log.debug("Received Command " + CMD);

                    if (CMD.equals(Commands.CMD_STOP)) {
                        break;
                    }
                } else {
                    log.warn("No messages received! Exiting.");
                    break;
                }
            }
        } finally {
            log.info("Terminating Loop closing sockets.");
            closeSockets();
        }
    }

    /**
     * Commands for control socket
     */
    public static class Commands {

        public static final String CMD_STOP = "STOP";

    }

}


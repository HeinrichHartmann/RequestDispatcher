package net.hh.request_dispatcher;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Manages communication with a single service.
 *
 * Features:
 * - Send messages to remote service.
 * - save callbacks for later execution
 * - match incoming requests to corresponging callbacks
 *
 * ZMQ Shutdown;
 * - Context is *not* terminated by this class.
 * - Non-blocking use: Call close before terminating context.
 * - Blocking sockets are closed when ctx.term() is called.
 *

 */
class AsyncZmqAdapter<Request extends Serializable, Reply extends Serializable> {

    private final Logger log = Logger.getLogger(AsyncZmqAdapter.class);

    private final ZMQ.Socket socket;
    private final ZMQ.Poller poller = new ZMQ.Poller(1);

    private final HashMap<Integer, Callback<Reply>> pendingCallbacks = new HashMap<Integer, Callback<Reply>>();
    private int callbackCounter = 0;

    private final String endpoint; // for debugging

    // DIRTY HACK
    public Callback lastCallback;

    /**
     * Return codes for recvAndExec()
     */
    public static enum RC {
        SUC,            // message received and onSuccess() called
        ERR,            // message received and onError() called
        NO_CALLBACK,    // message received and no callback available
        NO_MESSAGE      // no message received
    }


    // CONSTRUCTOR //
    public AsyncZmqAdapter(ZMQ.Socket socket) {
        this.socket = socket;
        poller.register(socket, ZMQ.Poller.POLLIN);

        this.endpoint = "";
    }

    public AsyncZmqAdapter(ZMQ.Context ctx, String endpoint) {
        this.endpoint = endpoint;

        socket = ctx.socket(ZMQ.DEALER);
        socket.setLinger(100);
        socket.setHWM(1000);
        socket.connect(endpoint);

        poller.register(socket, ZMQ.Poller.POLLIN);
    }

    /**
     * Non-blocking asynchronus request execution.
     * @param request   to be sent to the server
     * @param callback  executed on recvAndExec()
     */
    public void execute(Request request, Callback<Reply> callback) {
        int callbackId = -1;

        if (callback != null) {
            callbackId = callbackCounter++;
            pendingCallbacks.put(callbackId, callback);
        }

        try {
            TransferHelper.sendMessage(socket, new TransferHelper.TransferWrapper(request, callbackId));
        } catch (TransferHelper.ZmqEtermException e) {
            log.error("ETERM. Closing sockets.");
            close();
        } catch (IOException e) {
            log.error(e);
        }
    }


    // CALLBACK HANDLING //

    /**
     * Receive message on socket and execute corresponding callback.
     *
     * @param  flag     passed to ZMQ Socket
     * @return rc       return code. See enum descriptions.
     */
    public RC recvAndExec(int flag) {
        log.debug("Called recvAndExec() on " + this);

        TransferHelper.TransferWrapper reply = null;

        try {
            reply = TransferHelper.recvMessage(socket, flag);
            log.debug("Recieved message " + reply);
        } catch (TransferHelper.ZmqEtermException e) {
            log.error(e);
            close();
            return RC.NO_MESSAGE;
        } catch (IOException e) {
            log.error(e);
            return RC.NO_MESSAGE;
        }

        if (reply == null) { return RC.NO_MESSAGE; }

        Callback<Reply> callback = pendingCallbacks.get(reply.getCallbackId());
        pendingCallbacks.remove(reply.getCallbackId());

        // DIRTY HACK to executed callbacks accessible from outside the class
        lastCallback = callback;

        if (callback == null) {
            log.warn("No callback for message" + reply);
            return RC.NO_CALLBACK;
        }

        if (reply.isError()) {
            callback.onError((RequestException) reply.getObject());
            return RC.ERR;
        } else {
            callback.onSuccess((Reply) reply.getObject());
            return RC.SUC;
        }
    }

    /**
     * Calls onTimeout() of all pending callbacks and remove callbacks from list.
     */
    public void timeout() {
        for (Callback c: pendingCallbacks.values()){
            c.onTimeout();
        }

        pendingCallbacks.clear();
    }

    public boolean hasPendingCallbacks() {
        return !pendingCallbacks.isEmpty();
    }


    // ZMQ INTERNALS //

    /**
     * Signals if socket is ready to read.
     */
    public ZMQ.PollItem getPollItem() {
        return new ZMQ.PollItem(socket, ZMQ.Poller.POLLIN);
    }

    /**
     * Close socket.
     */
    public void close() {
        socket.close();
    }

    @Override
    public String toString() {
        return "AsyncZmqAdapter{" +
                "endpoint=" + endpoint + ", " +
                "callbackCounter=" + callbackCounter + ", " +
                "pendingCallbacks=" + pendingCallbacks.size() +
                '}';
    }
}

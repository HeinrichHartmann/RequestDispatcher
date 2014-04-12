package net.hh.request_dispatcher.service_adapter;

import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.server.RequestException;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

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
public class AsyncZmqAdapter<Request extends Serializable, Reply extends Serializable> {

    private final Logger log = Logger.getLogger(AsyncZmqAdapter.class);

    private final ZMQ.Socket socket;
    private final ZMQ.Poller poller = new ZMQ.Poller(1);

    private final HashMap<Integer, Callback<Reply>> pendingCallbacks = new HashMap<Integer, Callback<Reply>>();
    private int callbackCounter = 0;

    // CONSTRUCTOR //
    public AsyncZmqAdapter(ZMQ.Socket socket) {
        this.socket = socket;
        poller.register(socket, ZMQ.Poller.POLLIN);
    }

    public AsyncZmqAdapter(ZMQ.Context ctx, String endpoint) {
        this(ctx.socket(ZMQ.DEALER));

        socket.setLinger(100);
        socket.setHWM(1000);
        socket.connect(endpoint);
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

        AdapterHelper.sendMessage(socket, request, callbackId);
    }


    // CALLBACK HANDLING //

    /**
     * Receive message on socket and execute corresponding callback.
     *
     * @param  timeout  -1 for blocking infinite timeout.
     *                  0 for direct non-blocking return.
     *                  When timeout is reached onTimeout() method of *ALL* callbacks
     *                  are called.
     * @return rc       true if message received and callback executed.
     */
    public boolean recvAndExec(int timeout) throws RequestException {
        int messageCount = poller.poll(timeout);

        if (messageCount > 0) {
            ReplyWrapper reply = AdapterHelper.recvMessage(socket);

            log.debug("Recieved message " + reply);

            Callback<Reply> callback = pendingCallbacks.get(reply.getCallbackId());
            pendingCallbacks.remove(reply.getCallbackId());

            if (callback == null) {
                log.warn("No callback for message" + reply);
                return false;
            }

            if (reply.isError()) {
                callback.onError((RequestException) reply.getPayload());
            } else {
                callback.onSuccess((Reply) reply.getPayload());
            }

            return true;

        } else { // TIMEOUT
            timeout();
            return false;
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

}

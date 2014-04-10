package net.hh.request_dispatcher.service_adapter;

import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.server.RequestException;
import org.zeromq.ZMQ;

import java.io.Serializable;
import java.util.concurrent.TimeoutException;

/**
 * Manages communication with a single service
 *
 * Features:
 * - Send messages to remote service.
 * - save callbacks for later execution
 * - match incoming requests to corresponging callbacks
 */
public class RevisedZmqAdapter<Request extends Serializable, Reply extends Serializable> {

    public RevisedZmqAdapter(ZMQ.Socket socket) {}

    public RevisedZmqAdapter(ZMQ.Context ctx, String endpoint) {}

    // REQUEST EXECUTION //
    public void execute(Request request, Callback<Reply> callback) {}

    /**
     * @param request           to be sent to server
     * @param timeout           in ms. 0 means infinite wait.
     * @return response         as provided by server
     *
     * @throws RequestException thrown by request handler
     * @throws TimeoutException thrown on timeout
     */
    public Reply sendSync(Request request, int timeout) throws RequestException, TimeoutException { return null; }

    // CALLBACK HANDLING //

    /**
     * Signals if socket is ready to read.
     */
    public ZMQ.PollItem getPollItem() { return null; }

    /**
     * Receive message on socket and execute corresponding callback.
     *
     * @param  timeout  0 for infinite wait, -1 for direct non-blocking return.
     *                  When timeout is reached onTimeout() method of *ALL* callbacks
     *                  are called.
     * @return rc       as provided by socket
     */
    public boolean recvAndExec(int timeout)  { return false; }


    // SHUTDOWN //

    /**
     * Close socket.
     */
    public void close() {}

}

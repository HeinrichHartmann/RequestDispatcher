package net.hh.request_dispatcher;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.io.IOException;
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
class SyncZmqAdapter<Request extends Serializable, Reply extends Serializable> {

    private final Logger log = Logger.getLogger(SyncZmqAdapter.class);

    private final ZMQ.Socket socket;

    private final ZMQ.Poller poller = new ZMQ.Poller(1);

    // CONSTRUCTOR //

    public SyncZmqAdapter(ZMQ.Socket socket) {
        this.socket = socket;
        poller.register(socket, ZMQ.Poller.POLLIN);
    }

    public SyncZmqAdapter(ZMQ.Context ctx, String endpoint) {
        this(ctx.socket(ZMQ.DEALER));

        socket.setLinger(100);
        socket.setHWM(1000);
        socket.connect(endpoint);
    }

    /**
     * @param request           to be sent to server
     * @param timeout           in ms. if 0 it will return immediately. -1 means infinite wait.
     * @return response         never null.
     *
     * @throws RequestException thrown by request handler
     * @throws TimeoutException thrown on timeout
     */
    public Reply sendSync(Request request, int timeout) throws RequestException, TimeoutException {
        try {

            TransferHelper.sendMessage(socket, new TransferHelper.TransferWrapper(request, 0));

            // wait for messages
            int recvCount = poller.poll(timeout);

            if (recvCount == 0) throw new TimeoutException();

            TransferHelper.TransferWrapper answer = null;

            answer = TransferHelper.recvMessage(socket, 0);

            if (answer == null) { throw new IllegalStateException("Poller signaled, but no message received."); }

            if (answer.isError()) throw (RequestException) answer.getObject();

            return (Reply) answer.getObject();

        } catch (TransferHelper.ZmqEtermException e) {
            log.info("Eterm. Closing socket.");
            socket.close();
            throw new RequestException(e);
        } catch (IOException e) {
            throw new RequestException(e);
        }
    }

    // ZMQ INTERNALS //

    /**
     * Close socket.
     */
    public void close() {
        socket.close();
    }

}

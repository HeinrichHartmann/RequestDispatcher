package net.hh.request_dispatcher;

import org.zeromq.ZMQ;

import java.io.Serializable;

/**
 * Dispatches Requests to external services over ZMQ.
 * - match request object to the correct service by type
 *
 * Created by hartmann on 4/10/14.
 */
public class RevisedDispatcher {


    public RevisedDispatcher(ZMQ.Context ctx) {}

    public RevisedDispatcher() {
        this(ZMQ.context(1));
    }

    // SERVICE MANAGEMENT //

    public void registerService(final Class requestClass, final String endpoint) {}

    // REQUEST EXECUTION //

    /**
     * @param request   sent to the registered service.
     * @param callback  that handles the response. Executed on gatherResults()
     */
    public void execute(final Serializable request, final Callback callback) {}

    /**
     * @param request   sent to the registered service. Null on timeout.
     * @param timeout   in ms.
     * @return response
     */
    public Serializable executeSync(final Serializable request, int timeout) { return null;  }

    // CALLBACK EXECUTION //

    public void gatherResults(final int timeout) {}

    //// OTHER

    /**
     * Manage coordinated shutdown of all sockets.
     */
    public void shutdown() {}

}

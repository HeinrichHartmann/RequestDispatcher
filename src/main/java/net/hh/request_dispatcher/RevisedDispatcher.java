package net.hh.request_dispatcher;

import net.hh.request_dispatcher.server.RequestException;
import net.hh.request_dispatcher.service_adapter.AsyncZmqAdapter;
import net.hh.request_dispatcher.service_adapter.SyncZmqAdapter;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Dispatches Requests to external services over ZMQ.
 * - match request object to the correct service by type comparison
 *
 * Created by hartmann on 4/10/14.
 */
public class RevisedDispatcher {

    private Logger log = Logger.getLogger(RevisedDispatcher.class);

    private final ZMQ.Context ctx;
    private final Map<Class, AsyncZmqAdapter> asyncAdapters = new HashMap<Class, AsyncZmqAdapter>();
    private final Map<Class, SyncZmqAdapter> syncAdapters = new HashMap<Class, SyncZmqAdapter>();

    private final ZMQ.Poller poller = new ZMQ.Poller(0);

    // CONSTRUCTORS //
    public RevisedDispatcher(ZMQ.Context ctx) {
        this.ctx = ctx;
    }

    public RevisedDispatcher() {
        this(ZMQ.context(1));
    }

    // SERVICE MANAGEMENT //

    public void registerService(final Class requestClass, final String endpoint) {
        log.debug("Registering ServiceAcapter for class " + requestClass);

        syncAdapters.put(requestClass, new SyncZmqAdapter(ctx, endpoint));

        AsyncZmqAdapter asyncZmqAdapter = new AsyncZmqAdapter(ctx, endpoint);
        asyncAdapters.put(requestClass, asyncZmqAdapter);
        poller.register(asyncZmqAdapter.getPollItem());

    }

    // REQUEST EXECUTION //

    /**
     * @param request   sent to the registered service.
     * @param callback  that handles the response. Executed on gatherResults()
     */
    public void execute(final Serializable request, final Callback callback) {
        log.debug("Dispatching request of type " + request.getClass());

        if (! asyncAdapters.containsKey(request.getClass())) {
            throw new IllegalStateException("No adapter registered for class " + request.getClass());
        }

        asyncAdapters.get(request.getClass()).execute(request, callback);
    }

    /**
     * @param request   sent to the registered service. Null on timeout.
     * @param timeout   in ms.
     * @return response
     */
    public Serializable executeSync(final Serializable request, int timeout) throws TimeoutException, RequestException {
        if (! syncAdapters.containsKey(request.getClass())) {
            throw new IllegalStateException("No adapter registered for class " + request.getClass());
        }

        return syncAdapters.get(request.getClass()).sendSync(request, timeout);
    }

    // CALLBACK EXECUTION //

    public void gatherResults(final int timeout) {
        log.debug("Gathering results with timeout " + timeout);

        CountdownTimer timer = new CountdownTimer(timeout);
        timer.start();

        while(havePendingCallbacks()) {
            int messageCount = poller.poll(timer.timeLeft());

            if (messageCount > 0) {
                for (AsyncZmqAdapter asyncZmqAdapter : asyncAdapters.values()) {
                    asyncZmqAdapter.recvAndExec(ZMQ.NOBLOCK); // non blocking recv
                }
            } else { // nc <= 0
                if (timer.timeLeft() <= 0) {
                    log.debug("Timeout.");
                    timeoutAll();
                    break;
                } else {
                    log.debug("Interrupted while polling.");
                    // TODO: Shutdown sockets?
                    throw new IllegalStateException();
                }
            }
        } // while(havePendingCallbacks())
        log.debug("Finished gathering results.");
    }

    /**
     * @return true if one or more callbacks are pending.
     */
    private boolean havePendingCallbacks() {
        for (AsyncZmqAdapter asyncZmqAdapter : asyncAdapters.values()) {
            if (asyncZmqAdapter.hasPendingCallbacks()) return true;
        }
        return false;
    }

    /**
     * Call timeout() methods of all asyncAdapters.
     */
    private void timeoutAll() {
        for (AsyncZmqAdapter asyncZmqAdapter : asyncAdapters.values()) {
            asyncZmqAdapter.timeout();
        }
    }

    //// OTHER

    /**
     * Manage coordinated shutdown of all sockets.
     */
    public void close() {
        for (AsyncZmqAdapter asyncZmqAdapter : asyncAdapters.values()) {
            asyncZmqAdapter.close();
        }

        for (SyncZmqAdapter syncZmqAdapter : syncAdapters.values()) {
            syncZmqAdapter.close();
        }
    }

    //// HELPER

    private class CountdownTimer {
        long start_time;
        int timeout;

        /**
         * @param timeout   counter in ms.
         *                  -1  for infinite wait
         *                  0   for direct return
         */
        CountdownTimer(int timeout) {
            this.timeout = timeout;
        }

        public void start() {
            start_time = System.currentTimeMillis();
        }

        /**
         * @return  time left on countdown.
         *          0 if no time is left.
         *          always -1  if timeout was set to -1
         *          always 0   if timeout was set to 0
         */
        public int timeLeft() {
            if (timeout == -1)  return -1;
            if (timeout == 0)   return 0;
            return (int) Math.max(0, (timeout - (System.currentTimeMillis() - start_time)));
        }
    }



}

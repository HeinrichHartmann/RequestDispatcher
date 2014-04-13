package net.hh.request_dispatcher;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Dispatches Requests to external services over ZMQ.
 * - match request object to the correct service by type comparison
 *
 * Created by hartmann on 4/10/14.
 */
public class Dispatcher {

    private Logger log = Logger.getLogger(Dispatcher.class);

    private final ZMQ.Context ctx;
    private final Map<Class, AsyncZmqAdapter> asyncAdapters = new HashMap<Class, AsyncZmqAdapter>();
    private final Map<Class, SyncZmqAdapter> syncAdapters = new HashMap<Class, SyncZmqAdapter>();

    private final ZMQ.Poller poller = new ZMQ.Poller(0);

    // CONSTRUCTORS //
    public Dispatcher(ZMQ.Context ctx) {
        this.ctx = ctx;
    }

    public Dispatcher() {
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

    public void gatherResults() {
        gatherResults(-1);
    }

    /**
     * Collect replies and execute callbacks.
     * @param timeout   in ms.
     *                  -1 blocks forever
     *                  0  returns directly
     */
    public void gatherResults(final int timeout) {
        log.debug("Gathering results with timeout " + timeout);

        CountdownTimer timer = new CountdownTimer(timeout);
        timer.start();

        // deliver promises that are note dependent on a single callback
        deliverPromises();

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

            clearDependenciesFromPromises();
            deliverPromises();

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


    ///////////// PROMISES /////////////////

    Set<PromiseContainer<Callback>> openPromises = new HashSet<PromiseContainer<Callback>>();

    public void promise(Runnable runnable, Callback... callbacks) {
        openPromises.add(new PromiseContainer<Callback>(runnable, callbacks));
    }

    private void clearDependenciesFromPromises() {
        for (AsyncZmqAdapter asyncAdapter : asyncAdapters.values()) {
            for(PromiseContainer<Callback> promise : openPromises) {
                promise.clearDependency(asyncAdapter.lastCallback);
            }
        }
    }

    private void deliverPromises() {
        List<PromiseContainer> toRemove = new ArrayList<PromiseContainer>();

        for(PromiseContainer<Callback> promise : openPromises) {
            if (promise.isCleared()) {
                promise.keep();

                // need to delay removal since we are iterating over openPromises
                // java.util.ConcurrentModificationException
                toRemove.add(promise);
            }
        }

        for(PromiseContainer promise : toRemove) {
            openPromises.remove(promise);
        }
    }

    private class PromiseContainer<T> {
        private final Runnable runnable;
        private final Set<T> dependencies = new HashSet<T>();

        private boolean kept = false;

        private PromiseContainer(Runnable runnable, T[] dependencies) {
            this.dependencies.addAll(Arrays.asList(dependencies));
            this.runnable  = runnable;
        }

        /**
         * Removes dependency from the list of dependencies of this promise
         * @param dependency
         */
        public void clearDependency(T dependency) {
            dependencies.remove(dependency);
        }

        /**
         * @return sucess   true if all dependencies are removed
         */
        public boolean isCleared() {
            return dependencies.isEmpty();
        }

        /**
         * Execute stored callback.
         *
         * Throws IllegalStateException if called more than once.
         */
        public void keep() {
            if (kept) {
                throw new IllegalStateException("Called keep() twice.");
            }

            runnable.run();
            kept = true;
        }
    }


}

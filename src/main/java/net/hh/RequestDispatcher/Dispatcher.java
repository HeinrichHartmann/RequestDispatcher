package net.hh.RequestDispatcher;

import net.hh.RequestDispatcher.Service.Service;
import net.hh.RequestDispatcher.Service.ZmqService;
import net.hh.RequestDispatcher.TransferClasses.Request;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestRequest;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;

import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * Asynchronus Request Dispatcher Class
 * 
 * New requests can be issued with the execute() methods.
 * Replys are collected on the gatherResults() method.
 *
 * @author hartmann, rpickhardt
 */
public class Dispatcher {

    private static Logger log = Logger.getLogger(Dispatcher.class);

    private static int oo = Integer.MAX_VALUE;

    // holds registered services
    private final Map<String, Service> serviceInstances = new HashMap<String, Service>();

    // holds default services for request types
    private final Map<Class, String> defaultService = new HashMap<Class, String>();

    /////////////////// CONSTRUCTOR ////////////////////////

    public Dispatcher() {}


    ////////////////// SERVICE ADMINISTRATION //////////////////

    public void registerServiceProvider(String serviceName, ZmqService service) {
        if (serviceInstances.containsKey(serviceName)) {
            throw new IllegalArgumentException("Service Already registered");
        }
        serviceInstances.put(serviceName, service);

        registerPoller(service);
    }

    private Service getServiceProvider(final String serviceName) {
        if (! serviceInstances.containsKey(serviceName)){
            throw new IllegalArgumentException("No service provider registered for name " + serviceName);
        }
        return serviceInstances.get(serviceName);
    }

    //////////////// DEFAULT SERVICE RESOLUTION ////////////////////

    public void setDefaultService(Class<TestRequest> requestClass, String serviceName) {
        defaultService.put(requestClass, serviceName);
    }

    private String inferServiceName(final Request request) {
        if (! defaultService.containsKey(request.getClass())) {
            throw new IllegalArgumentException("No default service registered for request type");
        }

        return defaultService.get(request.getClass());
    }

    /////////////////// REQUEST EXECUTION  //////////////////////

    /**
     * Sends a non-blocking Request to the default service registered for the
     * Request type.
     *
     * Requires default service to be registered for request type.
     *
     * @param request
     * @param callback
     */
    public void execute(Request request, Callback callback) {
        execute(inferServiceName(request), request, callback);
    }

    /**
     * Sends a non-blocking Request to the service specified by serviceName.
     * The Response will be processed in the callback which has to be implented.
     *
     * @param serviceName   service identifier. Has to be registered before usage.
     * @param request       request object that will be serialized and passed to the server
     * @param callback
     */
    public void execute(String serviceName, Request request, Callback callback) {

        int id = generateCallbackId(callback);

        registerCallbackObject(id, callback);

        getServiceProvider(serviceName).send(encodeMessage(id, request));

    }

    /**
     * Listens on sockets and executes appropriate callbacks.
     * Blocks until all Replies are received.
     */
    public void gatherResults() {
        // wait forever
        gatherResults(oo);
    }

    /**
     * Listens on sockets and executes appropriate callbacks.
     * Blocks untial all Replies are received or timeout is hit.
     *
     * @param timeout maximal time to wait for replies
     */
    public void gatherResults(int timeout){
        log.debug("Gathering results with timeout " + timeout);

        Timer timer = new Timer(timeout);
        timer.start();

        // deliver promises that are note dependent on a single callback
        deliverPromises();

        while (hasPendingCallbacks()){

            try {

                String [] message = pollMessage(timer.timeLeft());

                log.debug("Recieved message " + Arrays.asList(message));

                int id      = parseId(message);
                String body = parseBody(message);
    
                Callback callback = pullCallbackObject(id);
    
                callback.processBody(body);

                clearDependenciesFromPromises(callback);
                deliverPromises();

            }

            catch (TimeoutException e) {

                for (Callback c: pendingCallbacks.values()){
                    c.processOnTimeout(e.getMessage());
                }

                pendingCallbacks.clear();

                System.out.println("SERVER: timeout on the server");
            }
        }
    }

    /**
     * Closes sockets of all registered services
     */
    public void close() {
        log.debug("Dispatcher object.");
        for (Service s : serviceInstances.values()){
            s.close();
        }
    }


    //////////////////// POLLING //////////////////////////

    private final ZMQ.Poller poller = ZmqService.getPoller();
    private final List<Service> pollServiceList = new ArrayList<Service>();

    /**
     * Recieves multipart messages from all open sockets as String [].
     *
     * @param timeout               maximal time to wait for messages
     *                              in milliseconds
     * @return multipartMessage
     * @throws TimeoutException     if timeout exceeded
     */
    private String[] pollMessage(int timeout) throws TimeoutException {
        log.debug("Polling sockets with timeout " + timeout);

        int messageCount = poller.poll(timeout);

        if (messageCount > 0){
            // message received
            for (int i = 0; i < pollServiceList.size(); i++){
                if (poller.pollin(i)){
                    return pollServiceList.get(i).recv();
                }
            }

            throw new IllegalStateException("No message recieved on polling.");
        } else {
            log.debug("Socket polling timed out after " + timeout + "ms");

            throw new TimeoutException("Request timed out after " + timeout + " milliseconds.");
        }
    }

    private void registerPoller(ZmqService service){
        poller.register(service.getSocket(), ZMQ.Poller.POLLIN);
        pollServiceList.add(service);
    }

    ///////////// Callback Object Storage ////////////

    private final Map<Integer, Callback> pendingCallbacks = new HashMap<Integer, Callback>();

    /**
     * Stores callback object in set. Returns ID for access.
     *
     * @param id
     * @param callback
     * @return callbackId
     */
    private void registerCallbackObject(int id, Callback callback){
        pendingCallbacks.put(id, callback);
    }

    private Callback pullCallbackObject(int callbackId){
        Callback out = pendingCallbacks.get(callbackId);
        pendingCallbacks.remove(callbackId);
        return out;
    }

    private int generateCallbackId(Callback callback) {
        // TODO: Make unique
        return callback.hashCode();
    }

    private boolean hasPendingCallbacks(){
        return ! pendingCallbacks.isEmpty();
    }

    ////////////// MESSAGE FRAMING ////////////

    /**
     * Generate ZMQ Message from request and callback id
     *
     * @param callbackId
     * @param request
     * @return message
     */
    private String[] encodeMessage(int callbackId, Request request) {
        // return new String[] { callbackID,request.serialize() };
        return new String[] {String.valueOf(callbackId), request.serialize() };
    }

    private int parseId(String [] message) {
        return Integer.valueOf(message[0]);
    }

    private String parseBody(String[] message) {
        return message[1];
    }

    ////////////// TIMEOUT ////////////

    // Helper class for managing timeouts
    private class Timer {
        long start_time;
        int timeout;

        Timer(int timeout) {
            this.timeout = timeout;
        }

        public void start() {
            start_time = System.currentTimeMillis();
        }

        public int timeLeft() {
            return (int) (timeout - (System.currentTimeMillis() - start_time));
        }
    }


    ///////////// PROMISES /////////////////

    Set<PromiseContainer<Callback>> openPromises = new HashSet<PromiseContainer<Callback>>();

    public void promise(Runnable runnable, Callback... callbacks) {
        openPromises.add(new PromiseContainer<Callback>(runnable, callbacks));
    }

    private void clearDependenciesFromPromises(Callback callback) {
        for(PromiseContainer<Callback> promise : openPromises) {
            promise.clearDependency(callback);
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

package net.hh.request_dispatcher;

import net.hh.request_dispatcher.server.RequestException;
import net.hh.request_dispatcher.service_adapter.ServiceAdapter;
import net.hh.request_dispatcher.transfer.SerializationHelper;
import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.Serializable;
import java.math.BigInteger;
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
    private final Map<String, ServiceAdapter> serviceInstances = new HashMap<String, ServiceAdapter>();

    // holds default services for request types
    private final Map<Class, String> defaultService = new HashMap<Class, String>();

    /////////////////// CONSTRUCTOR ////////////////////////

    public Dispatcher() {}

    ////////////////// ADAPTER ADMINISTRATION //////////////////

    public void registerServiceAdapter(final String serviceName, final ServiceAdapter service) {
        if (serviceInstances.containsKey(serviceName)) {
            throw new IllegalArgumentException("Service Already registered");
        }
        serviceInstances.put(serviceName, service);

        registerPoller(service);
    }

    private ServiceAdapter getServiceProvider(final String serviceName) {
        if (! serviceInstances.containsKey(serviceName)){
            throw new IllegalArgumentException("No service provider registered for name " + serviceName);
        }
        return serviceInstances.get(serviceName);
    }

    //////////////// DEFAULT SERVICE RESOLUTION ////////////////////

    public void setDefaultService(final Class requestClass, final String serviceName) {
        defaultService.put(requestClass, serviceName);
    }

    private String inferServiceName(final Serializable request) {
        if (! defaultService.containsKey(request.getClass())) {
            throw new IllegalArgumentException("No default service registered for request type");
        }

        return defaultService.get(request.getClass());
    }

    /**
     * Convenience method that registers service adapter directly for the request class.
     * @param requestClass
     * @param service
     */
    public void registerServiceAdapter(final Class requestClass, final ServiceAdapter service) {
        String serviceName = (String) requestClass.getName();
        registerServiceAdapter(serviceName, service);
        setDefaultService(requestClass, serviceName);
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
    public void execute(final Serializable request, final Callback callback) {
        // TODO: Enforce Request and Reply types
        execute(inferServiceName(request), request, callback);
    }

    /**
     * Sends a non-blocking Request to the service specified by serviceName.
     * The Response will be processed in the callback which has to be implemented.
     *
     * @param serviceName   service identifier. Has to be registered before usage.
     * @param request       request object that will be serialized and passed to the server
     * @param callback
     */
    public void execute(final String serviceName, final Serializable request, final Callback callback) {

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
    public void gatherResults(final int timeout){
        log.debug("Gathering results with timeout " + timeout);

        Timer timer = new Timer(timeout);
        timer.start();

        Callback callback = null;
        ZMsg message = new ZMsg();
        int id = 0;
        Serializable reply = null;

        // deliver promises that are note dependent on a single callback
        deliverPromises();

        while (hasPendingCallbacks()){

            try {

                message = pollMessage(timer.timeLeft());

                log.debug("Recieved message " + Arrays.asList(message));

                id    = parseId(message);
                reply = parseReply(message);
    
                callback = pullCallbackObject(id);

                if (reply instanceof RequestException) {
                    callback.onError((RequestException) reply);
                } else {
                    callback.onSuccess(reply);
                }

                clearDependenciesFromPromises(callback);
                deliverPromises();

            } catch (TimeoutException e) {

                for (Callback c: pendingCallbacks.values()){
                    c.onTimeOut(e.getMessage());
                }

                pendingCallbacks.clear();

                log.info("SERVER: timeout on the server");
            } catch (RequestException e) {
                log.error("Error occured in Worker.", e);
            }
        }
    }

    /**
     * Closes sockets of all registered services
     */
    public void close() {
        log.debug("Dispatcher object.");
        for (ServiceAdapter s : serviceInstances.values()){
            s.close();
        }
    }


    //////////////////// POLLING //////////////////////////

    private final ZMQ.Poller poller = new ZMQ.Poller(5);
    private final List<ServiceAdapter> pollServiceList = new ArrayList<ServiceAdapter>();

    /**
     * Recieves multipart messages from all open sockets as String [].
     *
     * @param timeout               maximal time to wait for messages
     *                              in milliseconds
     * @return multipartMessage
     * @throws TimeoutException     if timeout exceeded
     */
    private ZMsg pollMessage(final int timeout) throws TimeoutException {
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

    private void registerPoller(final ServiceAdapter service){
        poller.register(service.getPollItem());
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
    private void registerCallbackObject(final int id, final Callback callback){
        pendingCallbacks.put(id, callback);
    }

    private Callback pullCallbackObject(final int callbackId){
        Callback out = pendingCallbacks.get(callbackId);
        pendingCallbacks.remove(callbackId);
        return out;
    }

    private static int callbackCounter = 0;

    // returns a globally unique callback id
    private int generateCallbackId(final Callback callback) {
        return callbackCounter++;
        // return callback.hashCode();
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
    private ZMsg encodeMessage(int callbackId, Serializable request) {
        ZMsg out = new ZMsg();
        out.push(SerializationHelper.serialize(request));
        out.push(int2bytes(callbackId));
        return out;
    }

    private int parseId(ZMsg message) {
        return bytes2int(message.peekFirst().getData());
    }

    private Serializable parseReply(ZMsg message) {
        return (Serializable) SerializationHelper.deserialize(message.peekLast().getData());
    }

    public static byte[] int2bytes(int i) {
        return BigInteger.valueOf(i).toByteArray();
    }

    public static int bytes2int(byte[] data) {
        return new BigInteger(data).intValue();
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

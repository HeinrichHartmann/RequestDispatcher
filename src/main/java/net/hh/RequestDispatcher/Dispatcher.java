package net.hh.RequestDispatcher;

import net.hh.RequestDispatcher.Service.Service;
import net.hh.RequestDispatcher.Service.ZmqService;
import net.hh.RequestDispatcher.TransferClasses.Request;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestRequest;
import org.jeromq.ZMQ;

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

    private static int oo = Integer.MAX_VALUE;

    public void execute(Request request, Callback callback){
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

        Timer timer = new Timer(timeout);
        timer.start();

        while (hasPendingCallbacks()){

            try {

                String [] message = pollMessage(timer.timeLeft());

                int id      = parseId(message);
                String body = parseBody(message);
    
                Callback c = pullCallbackObject(id);
    
                c.processBody(body);

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

    public void close() {

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
            // timeout has happend
            throw new TimeoutException("Request timed out after " + timeout + " milliseconds.");
        }
    }

    private void registerPoller(ZmqService service){
        poller.register(service.getSocket(), ZMQ.POLLIN);
        pollServiceList.add(service);
    }


    ////////////////// SERVICE ADMINISTRATION //////////////////

    private final HashMap<String, Service> serviceInstances = new HashMap<String, Service>();

    public void registerServiceProvider(String serviceName, ZmqService service) {
        if (serviceInstances.containsKey(serviceName)) {
            throw new IllegalArgumentException("Service Already registered");
        }

        serviceInstances.put(serviceName, service);

        registerPoller(service);
    }

    public Service getServiceProvider(final String serviceName){
        if (! serviceInstances.containsKey(serviceName)){
            throw new IllegalArgumentException("No service provider registered for name " + serviceName);
        }
        return serviceInstances.get(serviceName);
    }


    //////////////// DEFAULT SERVICE RESOLUTION ////////////////////

    private final Map<Class, String> defaultService = new HashMap<Class, String>();

    public void setDefaultService(Class<TestRequest> testRequestClass, String test) {
        defaultService.put(testRequestClass, test);
    }

    private String inferServiceName(final Request request) {
        return defaultService.get(request.getClass());
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


}

package net.hh.RequestDispatcher;

import net.hh.RequestDispatcher.Service.Service;
import net.hh.RequestDispatcher.Service.ZmqService;
import net.hh.RequestDispatcher.TransferClasses.Request;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestRequest;

import org.jeromq.ZMQ;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Asynchronus Request Dispatcher Class
 * 
 * This class can send asynchronous requests to ZMQ Sockets. 
 * By default this class will not timeout but wait forever for all responses 
 * to come in. If this is not wellcome one can set a timeout parameter
 * 
 * The dispatcher will block until all responses are received or the timeout
 * value is hit.   
 * 
 * @author hartmann, rpickhardt
 */
public class Dispatcher {

    /////////////// timout
    private int timeout;
    private long startTime;

    
    public Dispatcher(){
        this.timeout = Integer.MIN_VALUE;
        startTime = -1;
    }
    
    public void execute(Request request, Callback callback){

        execute(inferServiceName(request), request, callback);

    }

    /**
     * sends a Request to the wire without blocking the thread until the Remote
     * server provides a response.
     *  
     * The Response will be processed in the callback which has to be implented.
     *  
     * @param serviceName
     * @param request
     * @param callback
     */
    public void execute(String serviceName, Request request, Callback callback) {
        initStartTime();
        
        int id = generateCallbackId(callback);
        
        requestInstances.put(id,serviceName);
        
        registerCallbackObject(id, callback);

        getServiceProvider(serviceName).send(encodeMessage(id, request));

    }

    /**
     * Blocks until all responses of all requests are delivered. 
     * 
     * If no blocking behavior is desired then use the setDefaultTimeout(int) method
     *  
     */
    public void gatherResults(){

        while (hasPendingCallbacks()){

            try {
                String [] message = pollMessage();
                if (message!=null){
                    
                    int id      = parseId(message);
                    String body = parseBody(message);
        
                    Callback c = pullCallbackObject(id);
        
                    c.processBody(body);

                }
            }
            catch (TimeoutException e) {
                for (Integer id:pendingCallbacks.keySet()){
                    Callback c = pendingCallbacks.get(id);
                    c.processOnTimeout(e.getMessage());
                }
                pendingCallbacks.clear();
                System.out.println("SERVER: timeout on the server");
            }
        }
        
        // reset the start Time
        // TODO: need setter method
        startTime = -1;

    }


    public void close() {

        for (Service s : serviceInstances.values()){
            s.close();
        }

    }

    //////////////////// POLLING //////////////////////////

    private final ZMQ.Poller poller = ZmqService.getPoller();
    private final List<Service> pollServiceList = new ArrayList<Service>();

    private String[] pollMessage() throws TimeoutException {
        // timeout in milliseconds
        int numObjects = poller.poll(timeout - (System.currentTimeMillis() - startTime));
        if (numObjects > 0){
            for (int i = 0; i < pollServiceList.size(); i++){
                if (poller.pollin(i)){
                    return pollServiceList.get(i).recv();
                }
            }
        }
        else {
            throw new TimeoutException(timeout + " ms have passed since the first request was put on the wire");
        }
        throw new IllegalStateException("No Message recieved");
    }

    private void registerPoller(ZmqService service){
        poller.register(service.getSocket(), ZMQ.POLLIN);
        pollServiceList.add(service);
    }


    ////////////////// SERVICE ADMINISTRATION //////////////////

    private final HashMap<String, Service> serviceInstances = new HashMap<String, Service>();
    private final HashMap<Integer, String> requestInstances = new HashMap<Integer, String>();

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

    ////////////// TIMEOUT ///////
    /**
     * sets the timeout the dispatcher to wait for all pending requests after 
     * the first request is sent to the wire to time milliseconds
     * 
     * @param timeout in milliseconds
     * @author rpickhardt
     */
    void setDefaultTimeout(int timeout){
        this.timeout = timeout;
    }

    /**
     * Checks sets the start time for global requests to the current time stamp
     * it it is not yet set. 
     * 
     * @author rpickhardt
     */
    private void initStartTime() {
       if (startTime < 0){
           startTime = System.currentTimeMillis();
       }
    }

    
}

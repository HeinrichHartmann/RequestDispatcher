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

/**
 * Asynchronus Request Dispatcher Class
 */
public class Dispatcher {

    public void execute(Request request, Callback callback){

        execute(inferServiceName(request), request, callback);

    }

    public void execute(String serviceName, Request request, Callback callback) {

        int id = generateCallbackId(callback);

        registerCallbackObject(id, callback);

        getServiceProvider(serviceName).send(encodeMessage(id, request));

    }

    public void gatherResults(){

        while (hasPendingCallbacks()){

            String [] message = pollMessage();

            int id      = parseId(message);
            String body = parseBody(message);

            Callback c = pullCallbackObject(id);

            c.processBody(body);

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

    private String[] pollMessage() {
        poller.poll();
        for (int i = 0; i < pollServiceList.size(); i++){
            if (poller.pollin(i)){
                return pollServiceList.get(i).recv();
            }
        }
        throw new IllegalStateException("No Message recieved");
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

}

package com.springapp.mvc.RequestDispatcher;

import com.springapp.mvc.RequestDispatcher.Service.Service;
import com.springapp.mvc.RequestDispatcher.Service.ZmqService;
import com.springapp.mvc.RequestDispatcher.TransferClasses.Request;

import java.util.HashMap;
import java.util.Map;

/**
 * Asynchronus Request Dispatcher Class
 */
public class Dispatcher {

    public void execute(Request request, Callback callback) {

        int id = generateCallbackId(callback);

        registerCallbackObject(id, callback);

        Service service = getServiceProvider(request.getClass());

        service.send(encodeMessage(id, request));

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

    public void terminate() {
        ZmqService.term();

        for (Service s : serviceInstances.values()){
            s.close();
        }
    }


    private String[] pollMessage() {
        // TODO: Add prper polling
        for (Service s : serviceInstances.values()){
            return s.recv();
        }
        return null;
    }


    ////////////////// SERVICE ADMINISTRATION //////////////////

    private final HashMap<Class, Service> serviceInstances = new HashMap<Class, Service>();

    public void registerServiceProvider(Class<? extends Request> requestType, Service service) {
        serviceInstances.put(requestType, service);
    }

    private Service getServiceProvider(final Class<? extends Request> requestType) {
        if (! serviceInstances.containsKey(requestType)) {
            throw new IllegalArgumentException("Request type not registered.");
        }
        return serviceInstances.get(requestType);
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

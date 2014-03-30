package net.hh.request_dispatcher;

import java.io.Serializable;

/**
 * Callback class that is used as an interface for the ZMQ callbacks which are 
 * called by the dispatcher
 * 
 * @author hartmann, rpickhardt
 *
 * @param <ReplyType>
 */
public abstract class Callback<ReplyType extends Serializable> {

    protected ReplyType reply;

    public Callback() {}

    /**
     * Create Callback Object
     * @param reply needs to be passed a mutable reply object will be passed to the onSuccess method.
     */
    public Callback(ReplyType reply){
        this.reply = reply;
    }

    /**
     * Will be called after successfully a response from an ZMQ socket is received
     * 
     * @param reply contains the reply
     */
    public abstract void onSuccess(ReplyType reply);

    /** 
     * override this function if you want to execute some code on a timeout
     * @param errorMessage
     */
    public void onTimeOut(String errorMessage){
        
    }

    public void processBody(String body) {
        // reply.fill(body);
        onSuccess(reply);
    }

    /**
     * passes the error message of the timeout to the onTimeOut method
     * @param message possible error message
     */
    public void processOnTimeout(String message){
        onTimeOut(message);
    }

}

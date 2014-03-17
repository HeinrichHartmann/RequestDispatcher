package net.hh.RequestDispatcher;

import net.hh.RequestDispatcher.TransferClasses.Reply;

/**
 * Callback class that is used as an interface for the ZMQ callbacks which are 
 * called by the dispatcher
 * 
 * @author hartmann, rpickhardt
 *
 * @param <ReplyType>
 */
public abstract class Callback<ReplyType extends Reply> {

    protected ReplyType reply;

    public Callback(ReplyType reply){
        this.reply = reply;
    }

    /**
     * Will be called after successfully a response of an ZMQ socket is received
     * 
     * @param reply contains the reply
     */
    public abstract void onSuccess(ReplyType reply);

    /** 
     * override this function if you want to execute some code on a timeout
     * @param reply
     */
    public void onTimeOut(String errorMessage){
        
    }
    
    public void processBody(String body) {
        reply.fill(body);
        onSuccess(reply);
    }
    
    /**
     * passes the error message of the timeout to the onTimeOut method
     * @param a possible error message
     */
    public void processOnTimeout(String message){
        onTimeOut(message);
    }

}

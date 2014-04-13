package net.hh.request_dispatcher;

import org.apache.log4j.Logger;

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

    private static Logger log = Logger.getLogger(Callback.class);

    public Callback() {}

    /**
     * Will be called after successfully a response from an ZMQ socket is received
     * 
     * @param reply contains the reply
     */
    public abstract void onSuccess(ReplyType reply);

    /** 
     * Override this function if you want to execute some code on a timeout
     */
    public void onTimeout() {
        log.debug("Called onTimeout()");
    }

    /**
     * Executed when exception thrown in handleRequest().
     *
     * @param e wraps the thrown exception
     */
    public void onError(RequestException e) {
        log.error("Called onError()", e);
    }

}

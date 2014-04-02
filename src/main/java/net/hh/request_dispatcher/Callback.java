package net.hh.request_dispatcher;

import net.hh.request_dispatcher.server.RequestException;
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
     * Do use empty constructor instead. No need to supply Reply instance any more.
     * @param reply
     */
    @Deprecated
    public Callback(ReplyType reply) {
    }

    /**
     * Will be called after successfully a response from an ZMQ socket is received
     * 
     * @param reply contains the reply
     */
    public abstract void onSuccess(ReplyType reply);

    /** 
     * Override this function if you want to execute some code on a timeout
     * @param errorMessage
     */
    public void onTimeOut(String errorMessage) {
        // pass
    };

    public void onError(RequestException e) throws RequestException {
        log.error("Called onError()", e);
        throw e;
    }

}

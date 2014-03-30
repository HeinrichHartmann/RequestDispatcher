package net.hh.request_dispatcher.worker;

import java.io.Serializable;

/**
 * Specifies the method to be called by ZmqWorker loop.
 *
 * Created by hartmann on 3/30/14.
 */
public interface RequestHandler<RequestType, ReplyType> extends Serializable {
    public ReplyType handleRequest(RequestType request);
}
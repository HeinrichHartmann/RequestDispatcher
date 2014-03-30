package net.hh.request_dispatcher.server;

import java.io.Serializable;

/**
 * Specifies the method to be called by ZmqWorker loop.
 *
 * Created by hartmann on 3/30/14.
 */
public interface RequestHandler<
        RequestType extends Serializable,
        ReplyType extends Serializable> extends Serializable {

    public ReplyType handleRequest(RequestType request);

}
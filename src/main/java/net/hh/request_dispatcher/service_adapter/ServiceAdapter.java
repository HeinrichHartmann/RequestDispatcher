package net.hh.request_dispatcher.service_adapter;

import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by hartmann on 3/30/14.
 */
public interface ServiceAdapter extends AutoCloseable {

    /**
     * Send a request to the remote service associated with the object.
     * Allows to stick a CallbackId to the request
     *
     * @param request       to be sent to the remote service
     * @param callbackId    id of the callback object to be called on receive
     * @throws IOException  if an error occures while sending the request
     */
    void send(Serializable request, Integer callbackId) throws IOException;

    /**
     * Blocking receive reply message from the remote service associated with the object.
     * Returns a pair consisting of the reply object and the CallbackId that was
     * passed along with the request.
     *
     * @return wrappedReply returned message. Null when interrupted.
     */
    ReplyWrapper recv();

    // TODO: Refactor to be usabile in generic event framework.
    // REMAKR: using socket.getFD() does not work for some reason.
    ZMQ.PollItem getPollItem();

    @Override
    void close() throws IOException;
}

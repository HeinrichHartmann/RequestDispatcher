package net.hh.request_dispatcher.service_adapter;

import net.hh.request_dispatcher.server.RequestException;
import org.zeromq.ZMQ;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by hartmann on 3/30/14.
 */
public interface ServiceAdapter extends AutoCloseable {

    /**
     * Non-blocking send request to the remote service associated with the object.
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

    /**
     * Blocking send request and receive associated reply from the remote service.
     *
     * @param request       to be sent to the service
     * @param timeout       timeout in MS
     * @return response     response from the remote service. null on timeout
     * @throws IOException
     */
    Serializable sendSync(Serializable request, int timeout) throws RequestException;

    // TODO: Refactor to be usable in generic event framework.
    // REMAKR: using socket.getFD() does not work for some reason.
    ZMQ.PollItem getPollItem();

    @Override
    void close() throws IOException;
}

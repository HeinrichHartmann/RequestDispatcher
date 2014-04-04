package net.hh.request_dispatcher.service_adapter;

import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.io.Closeable;
import java.io.Serializable;

/**
 * Created by hartmann on 3/30/14.
 */
public interface ServiceAdapter<RequestType extends Serializable, ReplyType extends Serializable> extends Closeable {

    // TODO: Refacotr to make this Request Type
    void send(ZMsg msg);

    // TODO: Refactor to be usabile in generic event framework.
    // REMAKR: using socket.getFD() does not work for some reason.
    ZMQ.PollItem getPollItem();

    // TODO: Refactor to make this ReplyType
    ZMsg recv();

    void close();
}

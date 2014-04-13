package net.hh.request_dispatcher;

import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

import java.io.Serializable;

/**
 * Extends TransferWrapperRaw by serialization functionality.
 */
public final class TransferWrapper extends TransferWrapperRaw {

    private final Serializable object;

    public TransferWrapper(Serializable object, Integer callbackId) throws CheckedSerializationException {
        this(object, callbackId, new ZFrame[0]);
    }

    public TransferWrapper(ZMsg message) throws TransferHelper.ProtocolException {
        super(message);
        try {
            this.object = SerializationHelper.deserialize(payload);
        } catch (CheckedSerializationException e) {
            throw new TransferHelper.ProtocolException(e);
        }
    }

    TransferWrapper(Serializable object, Integer callbackId, ZFrame[] envelope)  {
        super(SerializationHelper.serialize(object), callbackId, envelope);
        this.object = object;
    }

    public Serializable getObject() {
        return object;
    }

    public boolean isError() {
        return object instanceof RequestException;
    }

    public TransferWrapper constructReply(Serializable object) {
        return new TransferWrapper(object, getCallbackId(), getEnvelope());
    }

    public boolean isOneWayRequest() {
        return getCallbackId() == -1;
    }
}

package net.hh.request_dispatcher;

import java.io.Serializable;

/**
 * Class wrapping a reply object
 *
 * Created by hartmann on 4/5/14.
 */
class TransferWrapper {
    private final Serializable payload;
    private final Integer callbackId;

    public TransferWrapper(Serializable payload, Integer callbackId) {
        this.payload = payload;
        this.callbackId = callbackId;
    }

    public Serializable getPayload() {
        return payload;
    }

    public Integer getCallbackId() {
        return callbackId;
    }

    public boolean isError() {
        return payload instanceof RequestException;
    }

    @Override
    public String toString() {
        return "TransferWrapper{" +
                "payload=" + payload +
                ", callbackId=" + callbackId +
                '}';
    }

}

package net.hh.request_dispatcher.service_adapter;

import net.hh.request_dispatcher.server.RequestException;

import java.io.Serializable;

/**
 * Class wrapping a reply object
 *
 * Created by hartmann on 4/5/14.
 */
public class ReplyWrapper {
    private final Serializable payload;
    private final Integer callbackId;

    public ReplyWrapper(Serializable payload, Integer callbackId) {
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

package net.hh.request_dispatcher.worker;

import java.io.Serializable;

/**
 * Created by hartmann on 3/30/14.
 */
public class ReqTO implements Serializable {

    private final String payload;

    public ReqTO(String payload) {
        this.payload = payload;
    }

    public String toString() {
        return payload;
    }
}

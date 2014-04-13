package net.hh.request_dispatcher;

import java.io.Serializable;

/**
 * Created by hartmann on 3/30/14.
 */
public class RepTO implements Serializable {

    private final String payload;

    public RepTO(String payload) {
        this.payload = payload;
    }

    public String toString() {
        return payload;
    }
}

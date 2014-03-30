package net.hh.request_dispatcher.worker;

import java.io.Serializable;

/**
 * Created by hartmann on 3/30/14.
 */
public class ReplyTo implements Serializable {

    private final String payload;

    public ReplyTo(String payload) {
        this.payload = payload;
    }

    public String toString() {
        return payload;
    }
}

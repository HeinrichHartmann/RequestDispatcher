package net.hh.request_dispatcher.dispatcher_tests;

import java.io.Serializable;

public class TestDTO implements Serializable {

    private final String payload;

    public TestDTO(String message) {
        payload = message;
    }

    public TestDTO() {
        this("");
    }

    @Override
    public String toString() {
        return payload;
    }
}

package net.hh.request_dispatcher.transfer.test_service;

import java.io.Serializable;

public class TestRequest implements Serializable {

    private String payload;

    public TestRequest(){
        this("");
    }

    public TestRequest(
            String s) {
        payload = s;
    }

    public void fill(String m) {
        payload = m;
    }

    public String serialize() {
        return payload;
    }

}

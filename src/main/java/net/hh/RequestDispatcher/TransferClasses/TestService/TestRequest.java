package net.hh.RequestDispatcher.TransferClasses.TestService;

import net.hh.RequestDispatcher.TransferClasses.Request;

public class TestRequest implements Request {

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

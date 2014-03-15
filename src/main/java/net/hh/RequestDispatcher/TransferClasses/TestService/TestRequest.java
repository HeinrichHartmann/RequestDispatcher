package net.hh.RequestDispatcher.TransferClasses.TestService;

import net.hh.RequestDispatcher.TransferClasses.Request;

public class TestRequest implements Request {

    private String payload;

    public TestRequest(String s) {
        payload = s;
    }

    @Override
    public void fill(String m) {
        payload = m;
    }

    @Override
    public String serialize() {
        return payload;
    }

}

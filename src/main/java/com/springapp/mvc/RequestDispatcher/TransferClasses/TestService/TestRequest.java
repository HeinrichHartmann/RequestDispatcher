package com.springapp.mvc.RequestDispatcher.TransferClasses.TestService;

import com.springapp.mvc.RequestDispatcher.Service.Service;
import com.springapp.mvc.RequestDispatcher.TransferClasses.Request;

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

    public Class<? extends Service> getServiceType() {
        return TestService.class;
    }

}

package com.springapp.mvc.RequestDispatcher.TransferClasses.TestService;

import com.springapp.mvc.RequestDispatcher.TransferClasses.Reply;

public class TestReply implements Reply {

    private String payload;

    @Override
    public void fill(String message) {
        payload = message;
    }

    @Override
    public String serialize() {
        return payload;
    }
}

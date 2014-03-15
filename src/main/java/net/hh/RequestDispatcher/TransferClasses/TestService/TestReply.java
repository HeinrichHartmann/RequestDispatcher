package net.hh.RequestDispatcher.TransferClasses.TestService;

import net.hh.RequestDispatcher.TransferClasses.Reply;

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

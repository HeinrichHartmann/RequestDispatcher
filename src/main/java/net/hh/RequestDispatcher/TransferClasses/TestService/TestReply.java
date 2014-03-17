package net.hh.RequestDispatcher.TransferClasses.TestService;

import net.hh.RequestDispatcher.TransferClasses.Reply;

public class TestReply implements Reply {

    private String payload;

    public void fill(String message) {
        payload = message;
    }

    public String serialize() {
        return payload;
    }
}

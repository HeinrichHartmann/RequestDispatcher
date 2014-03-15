package com.springapp.mvc.RequestDispatcher;

import com.springapp.mvc.RequestDispatcher.TransferClasses.Reply;

public abstract class Callback<ReplyType extends Reply> {

    protected ReplyType reply;

    public Callback(ReplyType reply){
        this.reply = reply;
    }

    public abstract void onSuccess(ReplyType reply);

    public void processBody(String body) {
        reply.fill(body);
        onSuccess(reply);
    }

}

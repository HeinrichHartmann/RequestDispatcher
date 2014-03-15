package com.springapp.mvc.RequestDispatcher;

import com.springapp.mvc.RequestDispatcher.Service.ZmqService;
import com.springapp.mvc.RequestDispatcher.TransferClasses.TestService.TestReply;
import com.springapp.mvc.RequestDispatcher.TransferClasses.TestService.TestRequest;

/**
 * Start dispatcher service to querry echo server
 */
public class TestMain {

    public static void main(String[] args) {
        System.out.println("Starting service");

        Dispatcher dp = new Dispatcher();

        dp.registerServiceProvider(TestRequest.class, new ZmqService("tcp://127.0.0.1:60124"));

        dp.execute(new TestRequest("Hi From Dispatcher"), new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                System.out.println(reply.serialize());
            }
        });

        dp.execute(new TestRequest("Hi from another one"), new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                System.out.println(reply.serialize());
            }
        });


        dp.gatherResults();

        dp.terminate();

    }

}
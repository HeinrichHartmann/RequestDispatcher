package net.hh.RequestDispatcher;

import net.hh.RequestDispatcher.Service.ZmqService;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestReply;
import net.hh.RequestDispatcher.TransferClasses.TestService.TestRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Start dispatcher service to querry echo server
 */
public class TestMain {

    public static void main(String[] args) {
        System.out.println("Starting service");

        //////////// SETUP DISPATCHER ////////////
        Dispatcher dp = new Dispatcher();

        dp.registerServiceProvider("TEST-A", new ZmqService("tcp://127.0.0.1:60124"));
        dp.setDefaultService(TestRequest.class, "TEST-A");

        dp.registerServiceProvider("TEST-B", new ZmqService("tcp://127.0.0.1:60125"));

        /////////// BUSINESS LOGIC ///////////////

        final List<String> responses = new ArrayList<String>();

        dp.execute(
                new TestRequest("Hi From Dispatcher"),
                new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                // System.out.println("OUT-A " + reply.serialize());
                responses.add("A REQUEST");
            }
        });

        dp.execute(
                "TEST-B",
                new TestRequest("Hi from another one"),
                new Callback<TestReply>(new TestReply()) {

            @Override
            public void onSuccess(TestReply reply) {
                // System.out.println("OUT-B " + reply.serialize());
                responses.add("B REQUEST");
            }
        });

        dp.gatherResults();

        System.out.println("RESPONSES");
        for(String rep: responses){
            System.out.print(rep);
        }

        dp.terminate();

    }

}
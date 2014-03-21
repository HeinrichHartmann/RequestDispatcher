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

        for(int i = 0; i < 1; i++){
            final int i_copy = i;

            new Thread(new Runnable() {
            public void run() {
                issueRequests(i_copy);
            }
        }).start();
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void issueRequests(final int id) {

        System.out.println("Starting service");

        final Dispatcher dp = new Dispatcher();

        dp.registerServiceProvider("TEST-A", new ZmqService("tcp://127.0.0.1:60124"));
        dp.registerServiceProvider("TEST-B", new ZmqService("tcp://127.0.0.1:60125"));

        dp.setDefaultService(TestRequest.class, "TEST-A");

        /////////// BUSINESS LOGIC ///////////////

        final List<String> responses = new ArrayList<String>();

        final TestReply myReply = new TestReply();

        dp.execute(
                new TestRequest("Hi von Rene" + id),
                new Callback<TestReply>(myReply){

                    @Override
                    public void onSuccess(final TestReply firstReply) {
                        System.out.println("Hallo von Rene's callback: " + reply.serialize());

                        dp.execute(new TestRequest("Chained request"),
                                new Callback<TestReply>(new TestReply()){
                                    @Override
                                    public void onSuccess(final TestReply secondReply) {
                                        responses.add(
                                                "Chained Callback Action!" + "\n - " +
                                                firstReply.serialize() + "\n - " + secondReply.serialize() );
                                    }
                                });

                    }
                });

        dp.execute(
                new TestRequest("Hi From Dispatcher"),
                new Callback<TestReply>(new TestReply()) {
            @Override
            public void onSuccess(TestReply reply) {
                // System.out.println("OUT-A " + reply.serialize());
                responses.add("\n A REQUEST successfull\n");
            }
            public void onTimeOut(String errorMessage) {
                responses.add("\n A timeouted..." + errorMessage + "\n"); 
             }
            
        });

        dp.execute(
                "TEST-B",
                new TestRequest("Hi from another one"),
                new Callback<TestReply>(new TestReply()) {

            @Override
            public void onSuccess(TestReply reply) {
                // System.out.println("OUT-B " + reply.serialize());
                responses.add("\n B REQUEST successfull\n");
            }

            public void onTimeOut(String errorMessage) {
               responses.add("\n B timeouted..." + errorMessage); 
            }
        });

        dp.gatherResults(100);

        System.out.println("Thread- "+id+"RESPONSES");
        for(String rep: responses){
            System.out.print("Thread- "+id+rep);
        }

        dp.close();
    }

}
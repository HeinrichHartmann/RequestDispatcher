package com.springapp.mvc.RequestDispatcher.TransferClasses.TestService;

import com.springapp.mvc.RequestDispatcher.Service.ZmqService;

/**
 * Created by hartmann on 3/14/14.
 */
public class TestService extends ZmqService {

    public TestService(){
        this("tcp://127.0.0.1:50123");
    }

    public TestService(String endpoint) {
        super(endpoint);
    }

}

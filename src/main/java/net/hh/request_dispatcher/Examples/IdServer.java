package net.hh.request_dispatcher.Examples;

import net.hh.request_dispatcher.Callback;
import net.hh.request_dispatcher.Dispatcher;
import net.hh.request_dispatcher.RequestHandler;
import net.hh.request_dispatcher.ZmqWorkerProxy;

import java.io.Serializable;

public class IdServer {

    public static void main(String[] args) {

        // Define shared objects:
        String idChannel = "ipc:///idChannel";      // ZMQ Endpoint for communication
        class idRequest implements Serializable {}  // request class for dispatching

        //// SERVER SIDE
        //
        // Setup an idServer with a single thread listening on idChannel
        ZmqWorkerProxy proxy = new ZmqWorkerProxy(idChannel);
        proxy.add(1, new RequestHandler<idRequest, Long>() {
            long currentId = 0;

            @Override
            public Long handleRequest(idRequest request) throws Exception {
                return currentId++;
            }
        });
        proxy.startWorkers();


        //// CLIENT SIDE
        //
        // We create a dispatcher object and tell it to send idRequests to the idChannel:
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.registerService(idRequest.class, idChannel);

        // In the simplest case we do a synchronous call and wait for the result
        long id = (Long) dispatcher.executeSync(new idRequest());

        System.out.println("Sync id: " + id);
        // prints "Sync id: 0"


        // For asyncrhonus calls we define a callback object and pass it to the dispatcher
        dispatcher.execute(new idRequest(), new Callback<Long>() {
            @Override
            public void onSuccess(Long id) {
                System.out.println("Async id: " + id);
            }
        });
        // the request is now executed and is processing on the server.

        // The following call waits for the results of all requests
        // and executes the callback methods:
        dispatcher.gatherResults();

        // Cleanup:
        dispatcher.shutdown();
        proxy.shutdown();
    }

}

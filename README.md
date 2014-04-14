RequestDispatcher
=================
[![Build Status](https://travis-ci.org/HeinrichHartmann/RequestDispatcher.svg?branch=master)](https://travis-ci.org/HeinrichHartmann/RequestDispatcher)

Java library for dispatching request to extrenal services over ZeroMQ
sockets. The library targets use inside Java Servlet Containers, 
like Apache Tomcat, which expose a multithreaded environment.

It comes acompanied with ZmqWorker and ZmqProxy classes that allow 
the creation of simple remote services.

### Features

* Non-blocking access to remote Request/Reply ZMQ services.
* Asynchronus dispatching of several requests before the replys are received.
* Single threaded callback execution (gatherResults())
* Forwarding of Exceptions 
* One way requests (with callback = null)

### Example: id server

In this example we create a remote service that creates globally unique ids.

        //
        //// SHARED OBJECTS
        //
        String idChannel = "ipc:///idChannel";      // ZMQ Endpoint for communication
        class idRequest implements Serializable {}  // request class for dispatching

        //
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

        //
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
        // prints "Async id: 1"

        // Cleanup
        dispatcher.shutdown();
        proxy.shutdown();
        
        
### Components

* Request/Reply interfaces. To be implemented by new services and
  instanciated for requests. The objects are serialized and sent
  over the wire.
* Abstract Callback Class. To be extended by callback anonymous inner
  classes (AIC), hat handle the replies.
* Dispatcher Class. Orchestrages serivces and callback execution in a
  synchronus event loop.
* ZMQ Network Service library. Handles socket communication and polling.

![Class Diagram](https://raw.github.com/HeinrichHartmann/RequestDispatcher/master/img/ClassDiagram.png "Class Diagram")


### Control Flow 

![Image of control flow](https://raw.github.com/HeinrichHartmann/RequestDispatcher/master/img/DispatcherControlFlow.png "Request dispatcher control flow")


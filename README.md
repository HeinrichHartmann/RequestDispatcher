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
First we need to define some shared objects:

        String idChannel = "ipc:///idChannel";		// ZMQ Endpoint for communication
        class idRequest implements Serializable {}	// request class for dispatching

Now we create and start the id server:

        ZmqWorkerProxy proxy = new ZmqWorkerProxy(idChannel);
        proxy.add(1, new RequestHandler<idRequest, Long>() {
            long currentId = 0;

            @Override
            public Long handleRequest(idRequest request) throws Exception {
                return currentId++;
            }
        });
        proxy.startWorkers();

That's it. The server is now up and running and returns increasing numbers to our requests.
Now, to the client. We create a dispatcher object and tell it to send idRequests to the idChannel:

        Dispatcher dispatcher = new Dispatcher();
        dispatcher.registerService(idRequest.class, idChannel);

We can now use the dispatcher as follows:

	long id = (Long) dispatcher.executeSync(new idRequest());
	
This command sends a idRequest to the worker waits for the result and returns it a Serializable object.
For asyncrhonus calls use:

        dispatcher.execute(new idRequest(), new Callback<Long>() {
            @Override
            public void onSuccess(Long id) {
                System.out.println("Received id: " + id);
            }
        });

The request is now sent to the server, and when the response comes back it is queued in the socket. 
To execute defined callback we need to call:

    	dispatcher.gatherResults()

When we are finished call:

        dispatcher.shutdown();
        proxy.shutdown();

to close all sockets and terminate the ZMQ context.

### Components

* Request/Reply interfaces. To be implemented by new services and
  instanciated for requests. The objects are serialized and sent
  over the wire.
* Abstract Callback Class. To be extended by callback anonymous inner
  classes (AIC), hat handle the replies.
* Dispatcher Class. Orchestrages serivces and callback execution in a
  synchronus event loop.
* ZMQ Network Service library. Handles socket communication and polling.

### Control Flow 

![Image of control flow](https://raw.github.com/HeinrichHartmann/RequestDispatcher/master/img/DispatcherControlFlow.png "Request dispatcher control flow")

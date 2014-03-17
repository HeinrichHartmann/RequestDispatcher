RequestDispatcher
=================

Java library for dispatching request to extrenal services over ZeroMQ
sockets.

The library targets use inside Java Servlet Containers, like Apache
Tomcat, which expose a multithreaded environment.


    // setup communication sockets
    Dispatcher dispatcher = new Dispatcher();
    dispatcher.registerService(serviceID, new ZmqService(serviceEndpoint))

    // execute a new request
    disatcher.execute(
	  serviceID,
      new Request("Message to be send to service."),
      new Callback<TestReply>(new Reply()) {
        @Override
        public void onSuccess(Reply reply) {
		// Code to be executed when response arrives at server
		}
     });

    // execute another request
    dispatcher.execute(otherRequest, otherCallback);

    // listen to messages and execute callbacks
    dispatcher.gatherResults()

    // cleanup
    dispatcher.close()

Here `serviceId` and `serviceEndpoint` are string variables,
customized for the external service.

### Features

* Non-blocking access to remote Request/Reply ZMQ services.
* Asynchronus dispatching of several requests before the replys are
  received.
* Single threaded callback execution
* Thread savety. (i.e. usable in multithreaded environments.

### Components:

* Request/Reply interfaces. To be implemented by new services and
  instanciated for requests. The objects are serialized and sent
  over the wire.
* Abstract Callback Class. To be extended by callback anonymous inner
  classes (AIC), hat handle the replies.
* Dispatcher Class. Orchestrages serivces and callback execution in a
  synchronus event loop.
* ZMQ Network Service library. Handles socket communication and polling.

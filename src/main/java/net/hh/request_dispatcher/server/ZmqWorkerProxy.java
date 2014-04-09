package net.hh.request_dispatcher.server;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import zmq.Proxy;

import java.util.HashSet;
import java.util.Set;

/**
 * Dispatches work to multiple ZmqWorkers.
 *
 * Created by hartmann on 4/5/14.
 */
public class ZmqWorkerProxy {

    private static Logger log = Logger.getLogger(ZmqWorkerProxy.class);

    private static final String WORKER_PAYLOAD_CHANNEL = "inproc://workerPayload";
    private static final String WORKER_CONTROL_CHANNEL = "inproc://workerControl";
    private static final String CONTROL_STOP = "STOP";

    private final Set<ZmqWorker> managedWorkers = new HashSet<ZmqWorker>();

    private final ZMQ.Context ctx;
    private final ZMQ.Socket outsideSocket;
    private final ZMQ.Socket payloadSocket;
    private final ZMQ.Socket controlSocket;

    private enum State {
        created,
        started,
        stopped
    }
    private State state = State.created;

    /**
     * Creates a ZmqWorkerProxy object that listens for requests on the given input channel
     * and manages a set of ZmqWorker Threads.
     *
     * @param inputChannel    Endpoint to listen for requests.
     */
    public ZmqWorkerProxy(final String inputChannel) {
        this.ctx = ZMQ.context(1);

        outsideSocket = ctx.socket(ZMQ.ROUTER);
        outsideSocket.setLinger(1000);
        outsideSocket.setHWM(1000);
        outsideSocket.bind(inputChannel);

        payloadSocket = ctx.socket(ZMQ.DEALER);
        payloadSocket.setLinger(1000);
        payloadSocket.setHWM(1000);
        payloadSocket.bind(WORKER_PAYLOAD_CHANNEL);

        controlSocket = ctx.socket(ZMQ.PUB);
        controlSocket.setLinger(1000);
        controlSocket.setHWM(1000);
        controlSocket.bind(WORKER_CONTROL_CHANNEL);
    }

    /**
     * Start all currently maintained workers.
     *
     * Can only be called once.
     */
    public void startWorkers() {
        if (state != State.created) {
            throw new IllegalStateException("Workers already started.");
        }

        for(ZmqWorker worker : managedWorkers) {
            worker.start();
        }

        state = State.started;
    }

    /**
     * Stop all worker threads.
     *
     * Can only be called when threads startWorkers() has been called.
     */
    public void stopWorkers() {
        if (state != State.started) {
            throw new IllegalStateException("Workers not started.");
        }

        closeSockets();

        ctx.term();
        // terminates zmq worker threads.

        // join worker threads
        for (ZmqWorker worker : managedWorkers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                log.error("Interrupted join", e);
                throw new IllegalStateException(e);
            }
        }

        state = State.stopped;
    }

    /**
     * Start serving workers at port.
     *
     * Blocking call. Can only be terminated when ctx.term() is called.
     */
    public void doProxyLoop() {
        Proxy.proxy(outsideSocket.base(), payloadSocket.base(), null);
    }

    /**
     * Start doProxyLoop in a separate threads.
     *
     * Thread terminates when context is terminated.
     */
    public void doProxyBackground() {
        log.debug("Starting proxy loop in background thread.");

        new Thread(new Runnable() {
            @Override
            public void run() {
                doProxyLoop();
            }
        }).start();
    }

    //////////////////////  SET INTERFACE IMPLEMENTATION ////////////////////////////

    /**
     * Add a ZmqWorker for maintenance by ZmqWorkerProxy.
     *
     * @param worker to be added
     * @return true if this set did not already contain the specified worker
     */
    public boolean add(final ZmqWorker worker) {
        worker.setWorkSocket(generateWorkerSocket());
        return managedWorkers.add(worker);
    }

    /**
     * Remove worker object from maintenance by ZmqWorkerProxy.
     *
     * @param worker to be removed
     * @return true if this set contained the specified element
     */
    public boolean remove(final ZmqWorker worker) {
        return managedWorkers.remove(worker);
    }

    /**
     * Clear all workers from maintenance.
     */
    public void clear() {
        managedWorkers.clear();
    }

    ////////////////////////////// HELPER METHODS /////////////////////////////////

    private ZMQ.Socket generateWorkerSocket() {
        ZMQ.Socket socket = ctx.socket(ZMQ.ROUTER);
        socket.setLinger(100);
        socket.setHWM(1);
        socket.connect(WORKER_PAYLOAD_CHANNEL);
        return socket;
    }

    private void closeSockets() {
        log.trace("Closing internal sockets.");
        outsideSocket.close();
        controlSocket.close();
        payloadSocket.close();
    }
}

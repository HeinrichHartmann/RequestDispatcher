package net.hh.request_dispatcher.Server;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

/**
 * ZMQ ReqReply Server that returns the [multipart] message that was received to the sender.
 */
public class EchoServer implements Runnable {

    private static final Logger log = Logger.getLogger(EchoServer.class);

    // SOCKET CONFIG
    static final int K = 1000;
    static final int M = K * K;
    static final int HWM = 100;
    static final int LINGER = 1000;

    // Constructor parameters
    private final ZMQ.Context ctx;
    private final String endpoint;

    private ZMQ.Socket socket;
    private int delay;

    // Thread handling
    private Thread thread;
    private boolean ownContext;

    private ZMQ.Socket pipe;


    /**
     * Creates Echo Server that listens on a given enpoint.
     *
     * @param endpoint
     */
    public EchoServer(String endpoint) {
        this(ZMQ.context(1), endpoint);
        ownContext = true;
    }

    /**
     * Opens a socket on endpoint that returns multipart messages.
     * <p/>
     * Start server with start() in separate Thread.
     * Server will shut down properly when context is terminated.
     *
     * @param ctx      context to register socket on
     * @param endpoint endpoint to listen on
     */
    public EchoServer(ZMQ.Context ctx, String endpoint) {
        this.ctx = ctx;
        this.endpoint = endpoint;
        this.ownContext = false;

        // create socket
        socket = ctx.socket(ZMQ.REP);

        // config
        socket.setHWM(HWM);
        socket.setLinger(LINGER);

        // bind socket
        // When using inproc it is important to do this before sockets connect.
        // Binding the socket inside the constructor ensures this.
        socket.bind(endpoint);
    }

    /**
     * Start Echo Server
     */
    public void run() {
        log.info("Starting service.");
        int msgCount = 0;
        try {
            boolean suc; // success flag

            // here is the logic
            while (!Thread.currentThread().isInterrupted()) {
                ZMsg msg = ZMsg.recvMsg(socket);
                msgCount++;
                log.debug("Received Message " + msg);

                if (msg == null) {
                    log.debug("Interrupted (on recv).");
                    break;
                }

                if ( msg.peekLast().toString().equals("TERM") ) {
                    log.info("TERM");
                    break;
                }

                Thread.sleep(delay);

                log.debug("Sending message " + msg);
                suc = msg.send(socket);
                check(suc);
            }

        } catch (ZMQException e) {
            if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                log.debug("Received ETERM");
            }
        } catch (InterruptedException e) {
            log.debug("Interrupted (on sleep).");
        } finally {
            log.info("Received " + msgCount + " Messages.");
            log.info("Closing socket.");
            socket.close();
            socket = null;
        }
    }

    private void check(boolean suc) {
        if (suc) return;
        throw new IllegalStateException();
    }

    /**
     * Set Thread.sleep() delay for each message
     *
     * @param delay
     */
    public synchronized void setDelay(int delay) {
        this.delay = delay;
    }

    /**
     * Starts echo server in a new thread.
     */
    public synchronized void start() {
        if (thread != null) throw new IllegalStateException("Thread already started");
        thread = new Thread(this);
        thread.start();
    }

    /**
     * Stops the echo server.
     * Only works if a new context was created at startup. Otherwise the server
     * will shut down when the passed context terminates.
     */
    public synchronized void stop() {
        if (thread == null) throw new IllegalStateException("Thread not started.");

        // Cleanup context if we created it.
        if (ownContext) {
            ctx.term(); // this will cause ETERM at socket and close it.
        } else {
            log.warn("Cannot shutdown echo server. Terminate context manually.");
        }

        //TODO: Handle shutdown when context was not created.
    }

}

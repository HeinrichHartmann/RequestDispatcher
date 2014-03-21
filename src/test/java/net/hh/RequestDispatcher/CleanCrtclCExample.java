package net.hh.RequestDispatcher;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by hartmann on 3/19/14.
 */
public class CleanCrtclCExample {

    private static final Logger log = Logger.getLogger(CleanCrtclCExample.class);

    static {
        BasicConfigurator.configure();
    }


    @Test
    public void CCExample() throws Throwable {
        //  Prepare our context and socket
        final ZMQ.Context context = ZMQ.context(1);
        final Thread mainThread = Thread.currentThread();
        final Waiter waiter = new Waiter();

        final Thread zmqThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        log.info("Opening socket");
                        final ZMQ.Socket socket = context.socket(ZMQ.REP);
                        socket.bind("tcp://*:5555");

                        waiter.resume();

                        while (!Thread.currentThread().isInterrupted()) {
                            try {
                                log.info("blocking recv");
                                socket.recv(0);
                            } catch (ZMQException e) {
                                if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                                    log.info("catched ETERM");
                                    break;
                                }
                            }
                        }
                        log.info("Closing socket");
                        socket.close();
                    }
                });

        log.info("Starting thread");
        zmqThread.start();

        waiter.await();

        // make sure thread is actually blocking at socket.
        Thread.sleep(10);

        log.info("terminating context");
        context.term();

        log.info("Interrupting thread");
        zmqThread.interrupt();

        log.info("Joining thread");
        try {
            zmqThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void CCExampleStripped() throws Throwable {
        //  Prepare our context and socket
        final ZMQ.Context context = ZMQ.context(1);
        final Thread mainThread = Thread.currentThread();
        final AtomicBoolean isTerm = new AtomicBoolean(false);

        final Waiter waiter = new Waiter();

        final Thread zmqThread = new Thread(
                new Runnable() {
                    @Override
                    public void run() {
                        log.info("Opening socket");

                        ZMQ.Socket socket = context.socket(ZMQ.REP);
                        socket.bind("tcp://*:5555");

                        waiter.resume();
                        waiter.assertFalse(isTerm.get());
                        try {
                            log.info("blocking recv");
                            socket.recv(0);

                            // this should not be reached
                            waiter.fail();
                        } catch (ZMQException e) {
                            if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()) {
                                log.info("catched ETERM");
                            }
                        }
                        socket.close();
                    }
                });

        log.info("Starting thread");
        zmqThread.start();

        waiter.await();

        // make sure thread is actually blocking at socket.
        Thread.sleep(100);

        log.info("terminating context");
        context.term();
        isTerm.set(false);


        log.info("Joining thread");
        try {
            zmqThread.join();
        } catch (InterruptedException e) {
            Assert.fail();
        }
        waiter.resume();
    }
}

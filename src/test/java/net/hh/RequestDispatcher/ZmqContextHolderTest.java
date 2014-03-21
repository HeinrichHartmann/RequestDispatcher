package net.hh.RequestDispatcher;

import org.jodah.concurrentunit.Waiter;
import org.junit.Assert;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

import java.util.concurrent.CyclicBarrier;

/**
 * Created by hartmann on 3/19/14.
 */
public class ZmqContextHolderTest {

    @Test
    public void testCreateCloseSocket() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);
        s.close();
        c.term();
    }

    // NON TEST
    public void testCreateCloseSocketNotWorking() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);
        c.term(); // hangs
    }

    @Test(expected =  IllegalStateException.class)
    public void failingInterruptTest() throws Throwable {
        final Thread mainThread = Thread.currentThread();
        final ZMQ.Context c = ZMQ.context(1);

        Thread t = new Thread() {
            @Override
            public void run() {
                System.out.println("Starting Thread");
                final ZMQ.Socket s = c.socket(ZMQ.REP);
                s.bind("tcp://*:35153");

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        System.out.println("Listening on socket.");
                        s.recv (0);
                    } catch (ZMQException e) {
                        if (e.getErrorCode () == ZMQ.Error.ETERM.getCode ()) {
                            System.out.println("Catche ETERM.");
                            break;
                        }
                    }
                }
                System.out.println("closing socket");
                s.close();

                // Causes IllegalState Exception
                mainThread.interrupt();
            }
        };

        t.start();

        // waiting for blocking recv.
        Thread.sleep(1000);

        System.out.println("Terminating context.");
        c.term();
        // IllegalState exception is thrown here!

        System.out.println("Interrupting thread.");
        t.interrupt();

        System.out.println("Joining thread.");
        t.join();
    }



    public void testCreateSocket() throws Throwable {
        final boolean[] cterm = new boolean[] {false};
        final ZMQ.Context c = ZMQ.context(1);
        final ZMQ.Socket s = c.socket(ZMQ.DEALER);

        final CyclicBarrier barrier = new CyclicBarrier(2);

        final Waiter waiter = new Waiter();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Starting Thread");
                try {

                    waiter.assertFalse(cterm[0]);
                    waiter.resume();

                    System.out.println("blocking recv");

                    // blocks until c.term() called. Then ETERM is thrown.
                    s.recv();

                    // this should never be reached!
                    Assert.fail();

                } catch (ZMQException e){
                    System.out.println("Received ZMQ Exception. " + e);
                    waiter.assertEquals(ZMQ.Error.ETERM.getCode(), e.getErrorCode());
                    waiter.assertTrue(cterm[0]);
                }

                System.out.println("closing socket");
                s.close();
            }
        });

        t.start();

        // wait until socket is blocking
        waiter.await(100);
        Thread.sleep(100);

        System.out.println("Terminating context");
        cterm[0] = true;
        c.term();

        t.join();
    }

    @Test
    public void interruptSocketTest() throws Throwable {
        final ZMQ.Context c = ZMQ.context(1);

        final Waiter waiter = new Waiter();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("Starting Thread");

                final ZMQ.Socket s = c.socket(ZMQ.REP);
                s.bind("tcp://*:35153");

                // signal main thread to continue execution
                waiter.resume();

                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        s.recv (0);
                    } catch (ZMQException e) {
                        if (e.getErrorCode () == ZMQ.Error.ETERM.getCode ()) {
                            break;
                        }
                    }
                }

                System.out.println("closing socket");
                s.close();
            }
        });

        t.start();

        // System.out.println("waiting for thread to start.");
        waiter.await(1000);

        // waiting for blocking recv.
        // Thread.sleep(1000);

        System.out.println("Terminating context.");
        c.term();

        System.out.println("Interrupting thread.");
        t.interrupt();

        t.join();
    }

}

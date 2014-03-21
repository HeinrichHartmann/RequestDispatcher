package net.hh.RequestDispatcher;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import zmq.ZError;

public class ContextShutdownTest {
    private static final Logger log = Logger.getLogger(ContextShutdownTest.class);

    static {
        BasicConfigurator.configure();
    }

    @Test(timeout = 500)
    public void testNormalTerm() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);
        log.info("Closing socket.");
        s.close();
        log.info("Terminating context.");
        c.term();
    }

    @Test(timeout = 100)
    public void FAILING_termBeforeClose() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);
        c.term();
        s.close();
    }


    @Test(timeout = 100)
    public void term_after_failing_connect() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);

        // not existent
        s.connect("tcp://127.0.0.1:51234");

        log.info("Closing socket.");
        s.close();
        log.info("Terminating context.");
        c.term();
    }

    @Test(timeout = 100)
    public void term_after_bind() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);

        s.bind("tcp://127.0.0.1:51234");

        log.info("Closing socket.");
        s.close();
        log.info("Terminating context.");
        c.term();
    }

    @Test(timeout = 100)
    public void FAILING_term_after_bind_and_send() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);

        s.bind("tcp://127.0.0.1:51234");
        s.send("Hi");

        log.info("Closing socket.");
        s.close();
        log.info("Terminating context.");
        c.term();
    }

    /// FUNNY BEHAVIOUR. NEED SLEEPS TO BE REPRODUCABLE
    @Test(timeout = 100)
    public void FAILING_term_after_connect_and_snd() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);

        s.connect("tcp://127.0.0.1:51234");
        Thread.sleep(20);

        s.send("Hi");
        Thread.sleep(20);

        log.info("Closing socket.");
        s.close();
        log.info("Terminating context.");
        c.term();
    }

    @Test(timeout = 100)
    public void term_after_connect_and_snd_with_linger() throws Exception {
        ZMQ.Context c = ZMQ.context(1);
        ZMQ.Socket s = c.socket(ZMQ.DEALER);

        s.connect("tcp://127.0.0.1:51234");
        s.send("Hi");
        s.setLinger(0);

        log.info("Closing socket.");
        s.close();
        log.info("Terminating context.");
        c.term();
    }

    @Test(timeout = 100)
    public void testBlockOnRecvTerm() throws Exception {
        final ZMQ.Context c = ZMQ.context(1);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ZMQ.Socket s = c.socket(ZMQ.PULL);
                s.bind("tcp://*:12345");
                try {
                    log.info("socket.recv()");
                    s.recv(); // blocks
                    log.info("recv returns. UNREACHABLE");
                } catch (ZMQException e) {
                    log.info("Catched ETERM");
                } finally {
                    log.info("Closing socket.");
                    s.close();
                }
            }
        });

        t.start();
        Thread.sleep(50);

        log.info("Terminating context");
        c.term();
        log.info("done.");
    }


    @Test(timeout = 500)
    public void FAILING_termOnSend_no_linger() throws Exception {
        final ZMQ.Context c = ZMQ.context(1);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ZMQ.Socket s = c.socket(ZMQ.PUSH);

                log.info("Set HWM");
                s.setHWM(1);

                // address does not exists
                log.info("Socket Connect");
                s.connect("tcp://127.0.0.1:40162");

                try {
                    int i = 0;

                    // snd until block
                    while (true) {
                        log.info("snd msg " + i++);
                        s.send("Blablabla");
                    }

                } catch (ZMQException e) {
                    log.info("ETERM");

                    log.info("Closing socket.");
                    s.close(); // blocks forever since messages are waiting

                    log.info("NEVER REACHED");
                }
            }
        });

        t.start();
        Thread.sleep(100);

        log.info("Terminating context");
        c.term(); // blocks
        log.info("NEVER REACHED");
    }


    @Test(timeout = 1000)
    public void FAILING_termOnSend_set_ling_on_ETERM() throws Exception {
        final Thread mainThread = Thread.currentThread();
        final ZMQ.Context c = ZMQ.context(1);

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ZMQ.Socket s = c.socket(ZMQ.PUSH);

                log.info("Set HWM");
                s.setHWM(1);

                // address does not exists
                log.info("Socket Connect");
                s.connect("tcp://127.0.0.1:40162");

                try {
                    int i = 0;

                    // snd until block
                    while (true) {
                        log.info("snd msg " + i++);
                        s.send("Blablabla");
                    }

                } catch (ZMQException e) {

                    log.info("ETERM");

                    // throws  zmq.ZError$CtxTerminatedException
                    s.setLinger(0);

                    log.info("NEVER REACHED");

                    log.info("Closing socket.");
                    s.close();
                }
            }
        });

        t.start();
        Thread.sleep(200);

        log.info("Terminating context");
        c.term(); // blocks
        log.info("NEVER REACHED");
    }


    @Test(timeout = 500)
    public void testBlockOnSendTerm_catch_cxt_term_interrupt_main() throws Exception {
        final ZMQ.Context c = ZMQ.context(1);

        final Thread mainThread = Thread.currentThread();

        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                ZMQ.Socket s = c.socket(ZMQ.PUSH);

                log.info("Set HWM");
                s.setHWM(1);

                // address does not500 exists
                log.info("Socket Connect");
                s.connect("tcp://127.0.0.1:40162");

                try {
                    int i = 0;

                    // snd until block
                    while (true) {
                        log.info("snd msg " + i++);
                        s.send("Blablabla");
                    }

                } catch (ZMQException e) {
                    log.info("ETERM");

                    try {
                        log.info("Set linger to 0");
                        s.setLinger(0);

                    } catch (ZError.CtxTerminatedException f) {
                        log.info("Context Terminated Exception!");

                        log.info("Interrupt main thred.");
                        mainThread.interrupt();
                    }

                    log.info("Closing socket.");
                    s.close();
                }
            }
        });

        t.start();
        Thread.sleep(100);

        log.info("Terminating context");
        try {
            c.term();
        } catch (IllegalStateException e) {
            log.info("Catched Illegal State exception");
        }
    }

    @Test(timeout = 500)
    public void termSendBlock_linger_set() throws Exception {
        final ZMQ.Context ctx = ZMQ.context(1);

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ZMQ.Socket s = ctx.socket(ZMQ.PUSH);
                s.setLinger(200);
                s.setHWM(1);
                s.connect("tcp://127.0.0.1:50124"); // not existent

                log.info("Created socket.");

                try {
                    while (true) {
                        log.info("Sending message");
                        s.send("Hi!");
                    }

                } catch (ZMQException e) {
                    log.info("Catched ETERM");
                } finally {
                    log.info("Closing socket");
                    s.close();
                    log.info("Done closing socket. Still waiting for linger 200ms.");
                }
            }
        });

        thread.start();

        Thread.sleep(100);

        log.info("Context term.");
        ctx.close();
        log.info("done.");

    }
}
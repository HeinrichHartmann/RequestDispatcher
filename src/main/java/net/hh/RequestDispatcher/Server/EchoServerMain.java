package net.hh.RequestDispatcher.Server;

/**
 * Created by hartmann on 3/13/14.
 */
public class EchoServerMain {

    public static void main(String[] args) {
        try {
            System.out.println("Starting echo server.");
            EchoServer A = new EchoServer("tcp://*:60126", "A");
            A.start();

            // Give the server some time to start
            Thread.sleep(3 * 1000);

            // Need to call ctx.term first
            // https://github.com/zeromq/jeromq/issues/116
            EchoServer.term();
            A.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}

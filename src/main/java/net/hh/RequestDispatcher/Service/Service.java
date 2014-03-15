package net.hh.RequestDispatcher.Service;

public abstract class Service {

    public abstract void send(String[] m );

    public abstract String [] recv();

    public abstract void close();
}

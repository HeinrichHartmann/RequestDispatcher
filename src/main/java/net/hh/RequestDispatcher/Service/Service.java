package net.hh.RequestDispatcher.Service;

/**
 * The service interface for zmq socket services
 * 
 * @author heinrichhartmann
 * 
 */
public interface Service {

    public void send(String[] m);

    public String[] recv();

    public void close();

}

package net.hh.RequestDispatcher;

import org.apache.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Assistent class to hold zmq context objects.
 *
 * Created by hartmann on 3/19/14.
 */
public class ZmqContextHolder {

    private static final Logger log = Logger.getLogger(ZmqContextHolder.class);

    private static ZContext mainZContext = new ZContext();
    private static boolean isDestoyed = false;

    /**
     * Do not instanciate this class.
     *
     * Use getContext() and getManagedContext() to get context instances.
     */
    private ZmqContextHolder(){}

    /**
     * Returns main context of application.
     *
     * Use getManagedContext()
     *
     * @return context
     */
    public static ZMQ.Context getContext() {
        if (isDestoyed) throw new IllegalStateException("Context Already Destroyed.");

        return mainZContext.getContext();
    }


    public static ZContext getZContext() {
        return mainZContext;
    }

    /**
     * Returns shadowed context object with own set of managed sockets
     * but shared io-threads and internals with mainContext.
     *
     * @return zCtx
     */
    public static ZContext getShadowContext() {
        if (isDestoyed) throw new IllegalStateException("Context Already Destroyed.");

        return ZContext.shadow(mainZContext);
    }

    public synchronized static void destroy() {
        if (isDestoyed) return;
        log.info("Destroying sockets");

        isDestoyed = true;
        mainZContext.destroy();
    }

    public synchronized static void term() {
        if (isDestoyed) return;
        log.info("Terminating socket");

        isDestoyed = true;
        mainZContext.getContext().term();
    }

}

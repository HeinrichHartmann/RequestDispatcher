package net.hh.request_dispatcher;

import org.apache.log4j.Logger;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.math.BigInteger;

/**
 * Static helper methods for accessing sockets.
 *
 * - throws ZmqEtermExcetion on ETERM, so sockets can be closed on upper level.
 */
class TransferHelper {

    private static final Logger log = Logger.getLogger(TransferHelper.class);

    public static void sendMessage(ZMQ.Socket socket, TransferWrapper transferWrapper) throws ZmqEtermException {
        try {
            boolean rc = transferWrapper.toMessage().send(socket);
            if (!rc) throw new ZMQException.IOException(new IOException("Error sending message"));
        } catch (ZMQException e) {
            if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()){
                log.debug("Received ETERM.");
                throw new ZmqEtermException(e);
            } else {
                throw e;
            }
        }
    }

    /**
     * Receive and parse multipart message.
     *
     * @param socket    zmq socket to receive message on
     * @param flag      parameters passed to ZMsg.recvMsg()
     * @return reply    null if no message is received
     *
     * @throws ProtocolException    RequestDispatcher protocol violated
     * @throws ZmqEtermException    when context is closed while receiving messages
     * @throws IOException          other IOErrors (encompasses Protocol and ZmqEtermException)
     */
    public static TransferWrapper recvMessage(ZMQ.Socket socket, int flag) throws ZmqEtermException, ProtocolException {
        {
            try {
                ZMsg message = ZMsg.recvMsg(socket, flag);

                if (message == null || message.size() <= 2 ) {
                    // message.size() check works around bug in jeromq:
                    // https://github.com/zeromq/jeromq/commit/750b5f408ab2e925d17de700eda3e16185b770fc
                    return null;
                }

                return new TransferWrapper(message);

            } catch (ZMQException e) {
                if (e.getErrorCode() == ZMQ.Error.ETERM.getCode()){
                    log.debug("Received ETERM.");
                    throw new ZmqEtermException(e);
                } else {
                    throw e;
                }
            }
        }
    }

    //// Integer conversion

    public static byte[] int2bytes(int i) {
        return BigInteger.valueOf(i).toByteArray();
    }

    public static int bytes2int(byte[] data) {
        return new BigInteger(data).intValue();
    }

    /**
     * thrown when Dispatcher protocol is violated
     */
    static class ProtocolException extends IOException {
        public ProtocolException(String message) {
            super(message);
        }

        public ProtocolException(Exception e) {
            super(e);
        }
    }

    /**
     * thrown when context is closed on blocking recv.
     */
    static class ZmqEtermException extends IOException {
        ZmqEtermException(Exception e) {
            super(e);
        }
    }



}

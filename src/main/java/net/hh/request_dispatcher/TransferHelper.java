package net.hh.request_dispatcher;

import org.apache.log4j.Logger;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;
import org.zeromq.ZMsg;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * Created by hartmann on 4/10/14.
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

                if (message == null || message.size() == 0 ) {
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

    /**
     * Wraps payload and callbackId and evelope frames
     */
    static class RawTransferWrapper {
        protected final byte[]        payload;
        private final Integer       callbackId;
        private final ZFrame[]      envelope;

        public RawTransferWrapper(byte[] payload, Integer callbackId, ZFrame[] envelope) {
            this.payload = payload;
            this.callbackId = callbackId;
            this.envelope = envelope;
        }

        public RawTransferWrapper(ZMsg message) throws ProtocolException {
            int N = message.size();

            // Expect message at least three parts:
            // 0       envelope frame         -> envelope[N-4]
            // ...
            // N-4     envelope frame         -> envelope[0]
            // N-3     Empty Delimiter Frame
            // N-2     Serialized callback ID -> callbackId
            // N-1     Serialized payload     -> payload

            if (N <= 2) {
                throw new ProtocolException("Wrong number of Frames. Expected lower than 3: " + message.size());
            }

            ZFrame payloadFrame = message.pollLast();
            ZFrame callbackFrame = message.pollLast();
            ZFrame delimiterFrame = message.pollLast();

            if (delimiterFrame.size() != 0) {
                throw new ProtocolException("Delimiter frame not empty.");
            }

            callbackId = bytes2int(callbackFrame.getData());

            payload = payloadFrame.getData();

            envelope = new ZFrame[N-3];
            for(int i = 0; i < N-3; i++) {
                envelope[i] = message.pollLast();
            }

            if (message.size() != 0) throw new IllegalStateException("Message not empty.");
        }

        public ZMsg toMessage() {
            ZMsg out = new ZMsg();

            out.addFirst(payload);
            out.addFirst(int2bytes(callbackId));
            out.addFirst(new byte[0]);

            for (ZFrame f : envelope){
                out.addFirst(f);
            }

            return out;
        }

        public Integer getCallbackId() {
            return callbackId;
        }

        public ZFrame[] getEnvelope() {
            return envelope;
        }

        @Override
        public String toString() {
            return "RawTransferWrapper{" +
                    "payload=" + payload +
                    ", callbackId=" + callbackId +
                    ", envelope.length=" + envelope.length +
                    '}';
        }

    }

    /**
     * Additional methods for serialization added.
     */
    public final static class TransferWrapper extends RawTransferWrapper {

        private final Serializable object;

        public TransferWrapper(Serializable object, Integer callbackId) throws SerializationException {
            this(object, callbackId, new ZFrame[0]);
        }

        public TransferWrapper(ZMsg message) throws ProtocolException {
            super(message);
            try {
                this.object = SerializationHelper.deserialize(payload);
            } catch (SerializationException e) {
                throw new ProtocolException(e);
            }
        }

        private TransferWrapper(Serializable object, Integer callbackId, ZFrame[] envelope) throws SerializationException {
            super(SerializationHelper.serialize(object), callbackId, envelope);
            this.object = object;
        }

        public Serializable getObject() {
            return object;
        }

        public boolean isError() {
            return object instanceof RequestException;
        }

        public TransferWrapper constructReply(Serializable object) throws SerializationException {
            return new TransferWrapper(object, getCallbackId(), getEnvelope());
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

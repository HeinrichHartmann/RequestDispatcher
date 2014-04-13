package net.hh.request_dispatcher;

import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

/**
 * Wraps payload and callbackId and evelope frames.
 *
 * Handles conversion from raw types to messages.
 * Enforces transfer protocol
 */
class TransferWrapperRaw {
    protected final byte[] payload;
    private final Integer  callbackId;
    private final ZFrame[] envelope;

    public TransferWrapperRaw(byte[] payload, Integer callbackId, ZFrame[] envelope) {
        this.payload = payload;
        this.callbackId = callbackId;
        this.envelope = envelope;
    }

    /**
     * Expect multipart message at least three parts:
     *
     * 0       envelope frame         -> envelope[N-4]
     * ...
     * N-4     envelope frame         -> envelope[0]
     * N-3     Empty Delimiter Frame
     * N-2     Serialized callback ID -> callbackId
     * N-1     Serialized payload     -> payload
     *
     * @param message input
     * @throws TransferHelper.ProtocolException if protocol is violated
     */
    public TransferWrapperRaw(ZMsg message) throws TransferHelper.ProtocolException {
        if (message == null) { throw new IllegalArgumentException(); }
        int N = message.size();

        if (N <= 2) {
            throw new TransferHelper.ProtocolException("Wrong number of Frames. Expected lower than 3: " + message.size());
        }

        ZFrame payloadFrame = message.pollLast();
        ZFrame callbackFrame = message.pollLast();
        ZFrame delimiterFrame = message.pollLast();

        if (delimiterFrame.size() != 0) {
            throw new TransferHelper.ProtocolException("Delimiter frame not empty.");
        }

        callbackId = TransferHelper.bytes2int(callbackFrame.getData());

        payload = payloadFrame.getData();

        envelope = new ZFrame[N-3];
        for(int i = 0; i < N-3; i++) {
            envelope[i] = message.pollLast();
        }

        if (message.size() != 0) throw new IllegalStateException("Message not empty.");
    }

    /**
     * Inverse to ZMsg constructor.
     *
     * @return message
     */
    public ZMsg toMessage() {
        ZMsg out = new ZMsg();

        out.addFirst(payload);
        out.addFirst(TransferHelper.int2bytes(callbackId));
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
        return "TransferWrapperRaw{" +
                "payload=" + payload +
                ", callbackId=" + callbackId +
                ", envelope.length=" + envelope.length +
                '}';
    }

}

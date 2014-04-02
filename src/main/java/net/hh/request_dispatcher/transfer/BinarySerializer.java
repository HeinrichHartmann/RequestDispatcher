package net.hh.request_dispatcher.transfer;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

/**
 * Warpper for Java Object Serialization classes.
 * Created by hartmann on 3/30/14.
 */
public class BinarySerializer implements Serializer {

    public byte[] serialize(Serializable o) {
        return SerializationUtils.serialize(o);
    }

    public Object deserialize(byte[] data) {
        return SerializationUtils.deserialize(data);
    }

}

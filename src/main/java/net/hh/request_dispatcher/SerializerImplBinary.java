package net.hh.request_dispatcher;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

/**
 * Warpper for Java Object Serialization classes.
 * Created by hartmann on 3/30/14.
 */
class SerializerImplBinary implements Serializer {

    public byte[] serialize(Serializable o) {
            return SerializationUtils.serialize(o);
    }

    public Object deserialize(byte[] data) throws CheckedSerializationException {
        try {
            return SerializationUtils.deserialize(data);
        } catch (org.apache.commons.lang3.SerializationException e) {
            throw new CheckedSerializationException(e);
        }
    }

}

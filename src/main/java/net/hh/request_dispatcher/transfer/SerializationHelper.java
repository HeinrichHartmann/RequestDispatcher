package net.hh.request_dispatcher.transfer;

import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

/**
 * Created by hartmann on 3/30/14.
 */
public class SerializationHelper {

    public static byte[] serialize(Serializable o) {
        return SerializationUtils.serialize(o);
    }

    public static Object deserialize(byte[] data) {
        return SerializationUtils.deserialize(data);
    }
}

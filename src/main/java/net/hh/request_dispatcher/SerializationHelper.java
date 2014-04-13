package net.hh.request_dispatcher;

import java.io.Serializable;

/**
 * Static implementation fo Serializer interface.
 *
 * Created by hartmann on 4/2/14.
 */
class SerializationHelper {

    private static Serializer serializer = new SerializerImplBinary();

    /**
     * Serialize object to binary blop.
     *
     * @param object
     * @return blop     encoded object.
     * @throws org.apache.commons.lang3.SerializationException if serialization fails.
     *                  as this is a rare event and is caused by a programming error,
     *                  this exception is unchecked.
     *
     */
    public static byte[] serialize(Serializable object) {
        return serializer.serialize(object);
    }

    /**
     * Create object from binary blop.
     * Inverse method to serialize()
     *
     * @param blop
     * @return object
     */
    public static Serializable deserialize(byte[] blop) throws CheckedSerializationException {
        try {
            return (Serializable) serializer.deserialize(blop);
        } catch (ClassCastException e) {
            throw new CheckedSerializationException(e);
        }
    }

}

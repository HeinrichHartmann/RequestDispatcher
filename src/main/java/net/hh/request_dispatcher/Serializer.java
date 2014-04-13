package net.hh.request_dispatcher;

import java.io.Serializable;

/**
 * Generic serialization interface.
 *
 * Created by hartmann on 4/2/14.
 */
interface Serializer {

    /**
     * Serialize object to binary blop.
     *
     * @param object
     * @return blop     encoded object.
     */
    public byte[] serialize(Serializable object);

    /**
     * Create object from binary blop.
     * Inverse method to serialize()
     *
     * @param blop
     * @return object
     */
    public Object deserialize(byte[] blop);

}

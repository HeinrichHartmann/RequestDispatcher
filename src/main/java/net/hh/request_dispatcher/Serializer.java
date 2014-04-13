package net.hh.request_dispatcher;

import org.apache.commons.lang3.SerializationException;

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
     *
     * @throws SerializationException if serialization fails.
     *                                as this is a rare event and is caused by a programming error,
     *                                this execption is unchecked.
     *
     */
    public byte[] serialize(Serializable object);

    /**
     * Create object from binary blop.
     * Inverse method to serialize()
     *
     * @param blop
     * @return object
     * @throws net.hh.request_dispatcher.CheckedSerializationException if something is not ok with the data.
     */
    public Object deserialize(byte[] blop) throws CheckedSerializationException;

}

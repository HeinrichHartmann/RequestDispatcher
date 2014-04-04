package net.hh.request_dispatcher.transfer;

import java.io.Serializable;

/**
 * Static implementation fo Serializer interface.
 *
 * Created by hartmann on 4/2/14.
 */
public class SerializationHelper {

    private static Serializer serializer = new XmlSerializer();

    /**
     * Serialize object to binary blop.
     *
     * @param object
     * @return blop     encoded object.
     */
    public static byte[] serialize(Serializable object){
        return serializer.serialize(object);
    }

    /**
     * Create object from binary blop.
     * Inverse method to serialize()
     *
     * @param blop
     * @return object
     */
    public static Object deserialize(byte[] blop) {
        return serializer.deserialize(blop);
    }


}

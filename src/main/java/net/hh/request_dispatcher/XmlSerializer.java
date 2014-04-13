package net.hh.request_dispatcher;

import com.thoughtworks.xstream.XStream;

import java.io.Serializable;

/**
 * Warpper for XStream serialization
 * http://xstream.codehaus.org/tutorial.html
 *
 * Created by hartmann on 3/30/14.
 */
class XmlSerializer implements Serializer {

    private static final XStream xStream = new XStream();

    public byte[] serialize(Serializable o) {
        return xStream.toXML(o).getBytes();
    }

    public Object deserialize(byte[] data) {
        return xStream.fromXML(new String(data));
    }

}

package net.hh.request_dispatcher;

import java.io.IOException;

/**
 * thrown if something goes wrong with serialization
 */
class SerializationException extends IOException {

    public SerializationException() {
    }

    public SerializationException(String message) {
        super(message);
    }

    public SerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SerializationException(Throwable cause) {
        super(cause);
    }
}


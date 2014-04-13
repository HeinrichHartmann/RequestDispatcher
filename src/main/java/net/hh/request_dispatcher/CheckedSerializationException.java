package net.hh.request_dispatcher;

import java.io.IOException;

/**
 * thrown if something goes wrong with serialization
 */
class CheckedSerializationException extends IOException {

    public CheckedSerializationException() {
    }

    public CheckedSerializationException(String message) {
        super(message);
    }

    public CheckedSerializationException(String message, Throwable cause) {
        super(message, cause);
    }

    public CheckedSerializationException(Throwable cause) {
        super(cause);
    }
}


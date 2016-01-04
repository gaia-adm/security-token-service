package com.hp.gaia.sts.util;

/**
 * Created by belozovs on 1/4/2016.
 */
public class TokenStorageException extends RuntimeException {

    public TokenStorageException() {
        super();
    }

    public TokenStorageException(String message) {
        super(message);
    }

    public TokenStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public TokenStorageException(Throwable cause) {
        super(cause);
    }

    public TokenStorageException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

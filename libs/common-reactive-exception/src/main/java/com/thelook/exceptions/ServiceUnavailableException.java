package com.thelook.exceptions;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException (String msg) {
        super(msg);
    }

}

package com.thelook.exceptions;

public class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException(String msg) {
        super(msg);
    }

}

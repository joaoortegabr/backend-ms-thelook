package com.thelook.exceptions;

public class UnprocessableRequestException extends RuntimeException {

    public UnprocessableRequestException(String msg) {
        super(msg);
    }

}

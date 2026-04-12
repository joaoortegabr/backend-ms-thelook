package com.thelook.exceptions;

import org.springframework.core.NestedRuntimeException;

public class StorageException extends NestedRuntimeException {

    public StorageException(String msg) {
        super(msg);
    }

    public StorageException(String msg, Throwable cause) {
        super(msg, cause);
    }

}

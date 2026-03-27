package com.thelook.exceptions;

public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(Object id) {
        super("Registro não encontrado: id " + id);
    }

}

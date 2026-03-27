package com.thelook.exceptions;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.Instant;

public record StandardError(
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy'T'HH:mm:ss'Z'", timezone = "GMT") Instant timestamp,
        Integer status,
        String error,
        String message,
        String path,
        String correlationId
    ) implements Serializable {

}

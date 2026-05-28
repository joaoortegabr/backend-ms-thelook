package com.thelook.ms_feed.validation;

import com.thelook.exceptions.UnprocessableRequestException;

import java.util.List;
import java.util.UUID;

public class SearchAfterValidator {

    private static final int EXPECTED_SIZE = 2;
    private static final int CURSOR_INDEX_TIMESTAMP = 0;
    private static final int CURSOR_INDEX_ID = 1;

    private SearchAfterValidator() {}

    /**
     * Valida o cursor search-after do Elasticsearch.
     * O sort do feed é sempre [createdAt DESC, id DESC], portanto o cursor deve conter
     * exatamente 2 elementos: um timestamp (epoch millis) e um UUID (tiebreaker lexicográfico).
     */
    public static void validate(List<Object> lastSortValues) {
        if (lastSortValues == null || lastSortValues.isEmpty()) {
            return;
        }

        if (lastSortValues.size() != EXPECTED_SIZE) {
            throw new UnprocessableRequestException(
                    "Cursor inválido: esperado " + EXPECTED_SIZE + " valores, recebido " + lastSortValues.size());
        }

        validateTimestamp(lastSortValues.get(CURSOR_INDEX_TIMESTAMP));
        validateUuid(lastSortValues.get(CURSOR_INDEX_ID));
    }

    private static void validateTimestamp(Object value) {
        if (value == null) {
            throw new UnprocessableRequestException("Cursor inválido: timestamp não pode ser nulo");
        }
        try {
            long timestamp = Long.parseLong(value.toString());
            if (timestamp < 0) {
                throw new UnprocessableRequestException("Cursor inválido: timestamp não pode ser negativo");
            }
        } catch (NumberFormatException e) {
            throw new UnprocessableRequestException("Cursor inválido: timestamp deve ser um número (epoch millis)");
        }
    }

    private static void validateUuid(Object value) {
        if (value == null) {
            throw new UnprocessableRequestException("Cursor inválido: id não pode ser nulo");
        }
        try {
            UUID.fromString(value.toString());
        } catch (IllegalArgumentException e) {
            throw new UnprocessableRequestException("Cursor inválido: id deve ser um UUID válido");
        }
    }
}

package com.thelook.ms_feed.validation;

import com.thelook.exceptions.UnprocessableRequestException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SearchAfterValidatorTest {

    // =========================================================
    // Valid / no-op cases
    // =========================================================

    @Test
    void validate_nullList_doesNotThrow() {
        assertThatCode(() -> SearchAfterValidator.validate(null))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_emptyList_doesNotThrow() {
        assertThatCode(() -> SearchAfterValidator.validate(List.of()))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_validCursorWithCurrentTimestamp_doesNotThrow() {
        String ts = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString();

        assertThatCode(() -> SearchAfterValidator.validate(List.of(ts, uuid)))
                .doesNotThrowAnyException();
    }

    @Test
    void validate_zeroTimestamp_doesNotThrow() {
        String uuid = UUID.randomUUID().toString();

        assertThatCode(() -> SearchAfterValidator.validate(List.of("0", uuid)))
                .doesNotThrowAnyException();
    }

    // =========================================================
    // Wrong size
    // =========================================================

    @Test
    void validate_singleElement_throwsWithExpectedSizeMessage() {
        assertThatThrownBy(() -> SearchAfterValidator.validate(List.of("12345")))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("2");
    }

    @Test
    void validate_threeElements_throwsWithExpectedSizeMessage() {
        assertThatThrownBy(() -> SearchAfterValidator.validate(
                List.of("12345", UUID.randomUUID().toString(), "extra")))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("2");
    }

    // =========================================================
    // Invalid timestamp
    // =========================================================

    @Test
    void validate_nonNumericTimestamp_throwsWithTimestampMessage() {
        assertThatThrownBy(() -> SearchAfterValidator.validate(
                List.of("not-a-number", UUID.randomUUID().toString())))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void validate_negativeTimestamp_throwsWithTimestampMessage() {
        assertThatThrownBy(() -> SearchAfterValidator.validate(
                List.of("-1", UUID.randomUUID().toString())))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("timestamp");
    }

    @Test
    void validate_nullTimestamp_throwsWithTimestampMessage() {
        // Arrays.asList allows null elements unlike List.of
        assertThatThrownBy(() -> SearchAfterValidator.validate(
                Arrays.asList(null, UUID.randomUUID().toString())))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("timestamp");
    }

    // =========================================================
    // Invalid UUID
    // =========================================================

    @Test
    void validate_invalidUuid_throwsWithIdMessage() {
        String ts = String.valueOf(System.currentTimeMillis());

        assertThatThrownBy(() -> SearchAfterValidator.validate(List.of(ts, "not-a-uuid")))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("id");
    }

    @Test
    void validate_nullUuid_throwsWithIdMessage() {
        String ts = String.valueOf(System.currentTimeMillis());

        assertThatThrownBy(() -> SearchAfterValidator.validate(
                Arrays.asList(ts, null)))
                .isInstanceOf(UnprocessableRequestException.class)
                .hasMessageContaining("id");
    }
}
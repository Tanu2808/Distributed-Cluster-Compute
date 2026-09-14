package com.cluster.worker.model.cluster;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JoinCodeTest {

    @Test
    void testValidJoinCode() {
        JoinCode code = new JoinCode("ABCD-1234-EFGH-5678");
        assertEquals("ABCD1234EFGH5678", code.getCode());
        assertEquals("ABCD-1234-EFGH-5678", code.getFormattedCode());
    }

    @Test
    void testWhitespaceAndCaseNormalization() {
        JoinCode code = new JoinCode("  abcd - 1234 - eFGH-5678 \t");
        assertEquals("ABCD1234EFGH5678", code.getCode());
        assertEquals("ABCD-1234-EFGH-5678", code.getFormattedCode());
    }

    @Test
    void testInvalidLength() {
        assertThrows(IllegalArgumentException.class, () -> new JoinCode("ABCD-1234"));
        assertThrows(IllegalArgumentException.class, () -> new JoinCode("ABCD-1234-EFGH-5678-9012"));
    }

    @Test
    void testInvalidCharacters() {
        assertThrows(IllegalArgumentException.class, () -> new JoinCode("ABCD-1234-EFGH-567*")); // Special char
    }

    @Test
    void testEmptyCode() {
        assertThrows(IllegalArgumentException.class, () -> new JoinCode(null));
        assertThrows(IllegalArgumentException.class, () -> new JoinCode(""));
        assertThrows(IllegalArgumentException.class, () -> new JoinCode("   "));
    }
}

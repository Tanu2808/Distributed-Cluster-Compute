package com.cluster.worker.model.cluster;

import java.util.Objects;
import java.util.regex.Pattern;

public class JoinCode {

    // E.g., 4 segments of 4 alphanumeric chars: XXXX-XXXX-XXXX-XXXX
    private static final Pattern NORMALIZED_PATTERN = Pattern.compile("^[A-Z0-9]{16}$");

    private final String code;

    public JoinCode(String rawCode) {
        if (rawCode == null || rawCode.isBlank()) {
            throw new IllegalArgumentException("Join code cannot be empty");
        }
        String normalized = normalize(rawCode);
        if (!NORMALIZED_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Invalid join code format. Must be 16 alphanumeric characters.");
        }
        this.code = normalized;
    }

    private String normalize(String input) {
        // Remove all whitespace and hyphens, convert to uppercase
        return input.replaceAll("[\\s-]", "").toUpperCase();
    }

    public String getCode() {
        return code;
    }

    public String getFormattedCode() {
        return code.substring(0, 4)
                + "-"
                + code.substring(4, 8)
                + "-"
                + code.substring(8, 12)
                + "-"
                + code.substring(12, 16);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JoinCode joinCode = (JoinCode) o;
        return Objects.equals(code, joinCode.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }

    @Override
    public String toString() {
        // Obfuscate in toString so it's not accidentally logged entirely, as requested "no use of
        // join code as permanent auth credential"
        // It's still a temporary secret.
        return "JoinCode{code='****-****-****-" + code.substring(12, 16) + "'}";
    }
}

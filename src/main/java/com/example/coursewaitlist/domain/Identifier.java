package com.example.coursewaitlist.domain;

public final class Identifier {
    private Identifier() {
    }

    public static boolean isValid(String value) {
        if (value == null || value.isBlank() || value.length() > 100) {
            return false;
        }
        return !isWhitespace(value.codePointAt(0)) && !isWhitespace(value.codePointBefore(value.length()));
    }

    private static boolean isWhitespace(int codePoint) {
        return Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint);
    }
}

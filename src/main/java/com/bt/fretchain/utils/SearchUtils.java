package com.bt.fretchain.utils;

import java.util.Locale;

/** Helpers for building safe SQL/JPQL LIKE patterns from user input. */
public final class SearchUtils {

    /** Escape character used with every LIKE built from {@link #containsPattern}. */
    public static final char LIKE_ESCAPE = '!';

    private SearchUtils() {
    }

    /**
     * "Dal_Makhani 50%" -> "%dal!_makhani 50!%%".
     * Returns null for blank input so callers can skip the predicate.
     */
    public static String containsPattern(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }

        String escaped = input.trim()
                .toLowerCase(Locale.ROOT)
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");

        return "%" + escaped + "%";
    }
}

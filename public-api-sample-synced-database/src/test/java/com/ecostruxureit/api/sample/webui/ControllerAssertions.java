/* Copyright (c) 2025 Schneider Electric. All Rights Reserved. */
package com.ecostruxureit.api.sample.webui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ControllerAssertions {

    private ControllerAssertions() {}

    static void assertContentInFirstPreElement(String html, String expectedLineStarts) {

        String preText = html.substring(html.indexOf("<pre>") + "<pre>".length(), html.indexOf("</pre>"))
                .trim();
        String[] actual = preText.split("\\R");
        String[] expected = expectedLineStarts.trim().split("\\R");
        assertEquals(expected.length, actual.length, "Unexpected line count");
        for (int lineNo = 0; lineNo < actual.length; lineNo++) {

            String expectedLineStart = expected[lineNo];
            String currentLine = actual[lineNo];
            assertTrue(
                    currentLine.startsWith(expectedLineStart),
                    "Line " + lineNo + " expected to start with [" + expectedLineStart + "] but line was ["
                            + currentLine + "]");
        }
    }

    static void assertContainsSubstring(String string, String substring) {
        assertTrue(string.contains(substring), "Expected \"" + substring + "\" to be present in:\n" + string);
    }

    static void assertOccurrencesOfSubstring(String string, String substring, int expectedOccurrences) {
        int actualOccurrences = countOccurrences(string, substring);
        assertEquals(
                expectedOccurrences,
                actualOccurrences,
                "Expected " + expectedOccurrences + " (but found " + actualOccurrences + ") occurrences of \""
                        + substring + "\" in:\n" + string);
    }

    private static int countOccurrences(String stringThatMayContainSubstring, String substring) {
        if (stringThatMayContainSubstring.isEmpty() || substring.isEmpty()) {
            throw new IllegalArgumentException("Neither argument may have length 0");
        }
        int occurrences = 0;
        for (int i = 0; (i = stringThatMayContainSubstring.indexOf(substring, i)) != -1; i += substring.length()) {
            ++occurrences;
        }
        return occurrences;
    }
}

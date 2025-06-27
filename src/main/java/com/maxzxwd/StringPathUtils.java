package com.maxzxwd;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class StringPathUtils {

    private static final char[] HEX = new char[] {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D',
            'E', 'F'};

    public StringPathUtils() {}

    @NotNull
    public static String escapeSegment(@NotNull String pathSegment) {

        var result = new StringBuilder(pathSegment.length() * 3);

        var bytes = pathSegment.getBytes(StandardCharsets.UTF_8);

        for (var b : bytes) {
            var value = b & 0xFF;

            if (isUnreserved(value)) {
                result.append((char) value);
            } else {
                result.append('%');
                result.append(HEX[(value >> 4) & 0xF]);
                result.append(HEX[value & 0xF]);
            }
        }
        return result.toString();
    }

    private static boolean isUnreserved(int b) {
        return (b >= 'A' && b <= 'Z') ||
                (b >= 'a' && b <= 'z') ||
                (b >= '0' && b <= '9') ||
                b == '-' || b == '_' || b == '.';
    }

    @NotNull
    public static String unescapeSegment(@NotNull String input) {

        var length = input.length();
        var buffer = new byte[length];
        var bufferPos = 0;

        for (var i = 0; i < length; i++) {
            var c = input.charAt(i);

            if (c == '%' && i + 2 < length) {
                var hi = hexToInt(input.charAt(i + 1));
                var lo = hexToInt(input.charAt(i + 2));

                if (hi >= 0 && lo >= 0) {
                    buffer[bufferPos++] = (byte) ((hi << 4) + lo);
                    i += 2;
                } else {
                    buffer[bufferPos++] = (byte) c;
                }
            } else {
                buffer[bufferPos++] = (byte) c;
            }
        }

        return new String(buffer, 0, bufferPos, StandardCharsets.UTF_8);
    }

    private static int hexToInt(char ch) {

        if (ch >= '0' && ch <= '9') {
            return ch - '0';
        }
        if (ch >= 'A' && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if (ch >= 'a' && ch <= 'f') {
            return ch - 'a' + 10;
        }
        return -1;
    }

    @NotNull
    public static List<String> extractUnescapedSegments(@Nullable String path) {

        if (path == null) {
            return List.of();
        } else {
            var normalizedPath = path;

            if (normalizedPath.startsWith("/")) {
                normalizedPath = normalizedPath.substring(1);
            }
            if (normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
            }

            if (normalizedPath.isEmpty()) {
                return List.of();
            } else {
                var pathSegments = Arrays.asList(normalizedPath.split("/"));
                return CollectionUtils.mapIntoList(pathSegments, StringPathUtils::unescapeSegment);
            }
        }
    }
}

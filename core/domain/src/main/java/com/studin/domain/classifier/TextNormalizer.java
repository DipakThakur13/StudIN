package com.studin.domain.classifier;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Java utility (roadmap: Java for small, low-level compatibility helpers).
 * Normalises untrusted, accessibility-exposed titles before classification:
 * strips control characters, emoji and markup noise, folds case, keeps Latin and
 * Devanagari letters/digits so Hindi-English (Hinglish) titles tokenize well.
 */
public final class TextNormalizer {

    /** Hard cap on characters we ever inspect – bounds CPU on hostile input. */
    public static final int MAX_INPUT_CHARS = 2_000;
    public static final int MAX_TOKENS = 256;

    private TextNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        String s = raw.length() > MAX_INPUT_CHARS ? raw.substring(0, MAX_INPUT_CHARS) : raw;
        s = Normalizer.normalize(s, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(s.length());
        boolean lastSpace = true;
        for (int i = 0; i < s.length(); ) {
            int cp = s.codePointAt(i);
            i += Character.charCount(cp);
            if (isKept(cp)) {
                out.appendCodePoint(cp);
                lastSpace = false;
            } else if (!lastSpace) {
                out.append(' ');
                lastSpace = true;
            }
        }
        int len = out.length();
        if (len > 0 && out.charAt(len - 1) == ' ') out.setLength(len - 1);
        return out.toString();
    }

    public static List<String> tokenize(String raw) {
        String n = normalize(raw);
        if (n.isEmpty()) return Collections.emptyList();
        String[] parts = n.split(" ");
        List<String> tokens = new ArrayList<>(Math.min(parts.length, MAX_TOKENS));
        for (String p : parts) {
            if (p.isEmpty()) continue;
            tokens.add(p);
            if (tokens.size() >= MAX_TOKENS) break;
        }
        return tokens;
    }

    /** True when the text contains Devanagari script (Hindi, Marathi …). */
    public static boolean hasDevanagari(String raw) {
        if (raw == null) return false;
        for (int i = 0; i < raw.length(); ) {
            int cp = raw.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.DEVANAGARI) return true;
        }
        return false;
    }

    private static boolean isKept(int cp) {
        if (cp == '+' || cp == '#') return true; // c++, c#
        if (Character.isLetterOrDigit(cp)) return true;
        // Devanagari combining vowel signs / virama are not "letters" in Java but matter.
        return Character.UnicodeBlock.of(cp) == Character.UnicodeBlock.DEVANAGARI
                && Character.getType(cp) != Character.OTHER_PUNCTUATION;
    }
}

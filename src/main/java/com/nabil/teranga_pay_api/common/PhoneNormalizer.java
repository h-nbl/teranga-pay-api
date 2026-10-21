package com.nabil.teranga_pay_api.common;

import java.util.regex.Pattern;

public final class PhoneNormalizer {

    private static final String SENEGAL_CODE = "221";
    private static final Pattern SEPARATORS = Pattern.compile("[\\s.\\-()]");
    private static final Pattern ALLOWED_CHARS = Pattern.compile("\\+?[0-9]+");
    private static final Pattern NATIONAL = Pattern.compile("[0-9]{9}");
    private static final Pattern E164 = Pattern.compile("\\+[1-9][0-9]{7,14}");

    private PhoneNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            throw invalid();
        }
        String cleaned = SEPARATORS.matcher(raw.strip()).replaceAll("");
        if (!ALLOWED_CHARS.matcher(cleaned).matches()) {
            throw invalid();
        }
        String candidate;
        if (cleaned.startsWith("+")) {
            candidate = cleaned;
        } else if (cleaned.startsWith("00")) {
            candidate = "+" + cleaned.substring(2);
        } else if (NATIONAL.matcher(cleaned).matches()) {
            candidate = "+" + SENEGAL_CODE + cleaned;
        } else if (cleaned.length() == 12 && cleaned.startsWith(SENEGAL_CODE)) {
            candidate = "+" + cleaned;
        } else {
            throw invalid();
        }
        if (!E164.matcher(candidate).matches()) {
            throw invalid();
        }
        return candidate;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException("Numéro de téléphone invalide");
    }
}
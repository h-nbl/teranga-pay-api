package com.nabil.teranga_pay_api.common;

public final class PhoneMasker {

    private PhoneMasker() {
    }

    public static String mask(String raw) {
        if (raw == null) {
            return "***";
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() < 4) {
            return "***";
        }
        return "***" + digits.substring(digits.length() - 2);
    }
}
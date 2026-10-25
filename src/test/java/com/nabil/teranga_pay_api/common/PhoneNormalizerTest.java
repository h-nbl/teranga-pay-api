package com.nabil.teranga_pay_api.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PhoneNormalizerTest {

    @Test
    void acceptedFormatsAreAllNormalizedToInternationalFormat() {
        String expected = "+221771234567";

        assertEquals(expected, PhoneNormalizer.normalize("771234567"));
        assertEquals(expected, PhoneNormalizer.normalize("77 123 45 67"));
        assertEquals(expected, PhoneNormalizer.normalize("+221771234567"));
        assertEquals(expected, PhoneNormalizer.normalize("+221 77 123 45 67"));
        assertEquals(expected, PhoneNormalizer.normalize("00221771234567"));
        assertEquals(expected, PhoneNormalizer.normalize("221771234567"));
        assertEquals(expected, PhoneNormalizer.normalize("  77-123-45-67  "));
    }

    @Test
    void foreignNumbersWithPlusAreKept() {
        assertEquals("+33612345678", PhoneNormalizer.normalize("+33 6 12 34 56 78"));
    }

    @Test
    void invalidNumbersAreRejected() {
        String[] invalid = {"", "   ", "abc", "77 123", "12345678", "+0771234567",
                "77123456a", "++221771234567", "+", "0771234567"};

        for (String value : invalid) {
            assertThrows(IllegalArgumentException.class,
                    () -> PhoneNormalizer.normalize(value),
                    "devrait être refusé : " + value);
        }
    }

    @Test
    void nullIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> PhoneNormalizer.normalize(null));
    }
}
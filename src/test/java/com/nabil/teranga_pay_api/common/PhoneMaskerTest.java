package com.nabil.teranga_pay_api.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class PhoneMaskerTest {

    @Test
    void keepsOnlyTheLastTwoDigitsWhateverTheFormat() {
        assertEquals("***67", PhoneMasker.mask("+221771234567"));
        assertEquals("***67", PhoneMasker.mask("77 123 45 67"));
        assertEquals("***67", PhoneMasker.mask("00221-77-123-45-67"));
    }

    @Test
    void revealsNothingForShortOrMissingInput() {
        assertEquals("***", PhoneMasker.mask(null));
        assertEquals("***", PhoneMasker.mask(""));
        assertEquals("***", PhoneMasker.mask("abc"));
        assertEquals("***", PhoneMasker.mask("123"));
        assertFalse(PhoneMasker.mask("+221771234567").contains("1234"));
    }
}
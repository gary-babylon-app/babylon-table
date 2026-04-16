package app.babylon.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class BigDecimalsTest
{
    @Test
    void prepareShouldNormalizePercentAndBracketNegativeValues()
    {
        BigDecimals.PreparedDecimal prepared = BigDecimals.prepare("$1,234.50%");

        assertNotNull(prepared);
        assertEquals("1234.50", prepared.normalizedNumberText());
        assertTrue(prepared.isPercent());
        assertFalse(prepared.isNegativeBracket());

        BigDecimals.PreparedDecimal negativeBracket = BigDecimals.prepare("(1,234.50)");
        assertNotNull(negativeBracket);
        assertEquals("1234.50", negativeBracket.normalizedNumberText());
        assertFalse(negativeBracket.isPercent());
        assertTrue(negativeBracket.isNegativeBracket());
    }

    @Test
    void prepareShouldRejectInvalidShapes()
    {
        assertNull(BigDecimals.prepare("1  23"));
        assertNull(BigDecimals.prepare(null));
        assertNull(BigDecimals.prepare("abc"));
    }

    @Test
    void isExtractableDecimalWordShouldRecognizeSimpleExtractableWords()
    {
        assertTrue(BigDecimals.isExtractableDecimalWord("$1,234.50"));
        assertTrue(BigDecimals.isExtractableDecimalWord("(1,234.50)"));
        assertTrue(BigDecimals.isExtractableDecimalWord(" 12.5% "));
        assertFalse(BigDecimals.isExtractableDecimalWord("12.5 and 7.5"));
        assertFalse(BigDecimals.isExtractableDecimalWord("abc"));
        assertFalse(BigDecimals.isExtractableDecimalWord(null));
        assertFalse(BigDecimals.isExtractableDecimalWord("   "));
    }

    @Test
    void parseAndExtractShouldStillHandleNormalizedInputs()
    {
        assertEquals(0, new BigDecimal("12.345").compareTo(BigDecimals.parse("12.345")));
        assertEquals(12.5d, BigDecimals.extractDouble("USD 12.50").doubleValue(), 1e-12);
        assertNull(BigDecimals.extractDouble("12.5 and 7.5"));
    }
}

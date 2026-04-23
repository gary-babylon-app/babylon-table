package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class DateValueFactsTest
{
    @Test
    void shouldDescribeNumericAlphaAndSplitForms()
    {
        DateValueFacts numeric = DateValueFacts.from("2026-03-01");
        DateValueFacts alpha = DateValueFacts.from("01-Mar-2026");

        assertEquals("2026-03-01", numeric.text());
        assertEquals(10, numeric.size());
        assertFalse(numeric.onlyDigits());
        assertFalse(numeric.isDecimal());
        assertEquals(0, numeric.alphaCount());
        assertFalse(numeric.hasCommas());
        assertFalse(numeric.hasPeriods());
        assertFalse(numeric.invalidForDateTokens());
        assertArrayEquals(new String[]
        {"2026", "03", "01"}, numeric.naturalDateSplit());
        assertArrayEquals(new int[]
        {2026, 3, 1}, numeric.parseThreeNumberGroups());

        assertEquals(3, alpha.alphaCount());
        assertArrayEquals(new String[]
        {"01", "Mar", "2026"}, alpha.naturalDateSplit());
    }

    @Test
    void shouldConvertToLocalDateForDateFormatsAndExcelValues()
    {
        assertEquals(LocalDate.of(2026, 3, 1), DateValueFacts.from("2026-03-01").toLocalDate(DateFormat.YMD));
        assertEquals(LocalDate.of(2026, 3, 1), DateValueFacts.from("01-Mar-2026").toLocalDate(DateFormat.DMY));
        assertEquals(LocalDate.of(2024, 5, 24), DateValueFacts.from("45436").toLocalDate(DateFormat.YMD));
        assertEquals(LocalDate.of(2024, 5, 24), DateValueFacts.from("45436.25").toLocalDate(DateFormat.YMD));
        assertEquals(null, DateValueFacts.from("2026.03.01").toLocalDate(DateFormat.YMD));
        assertEquals(null, DateValueFacts.from("202603").toLocalDate(DateFormat.YMD));
    }

    @Test
    void shouldRecogniseDecimalAndExcelCharacteristics()
    {
        DateValueFacts decimal = DateValueFacts.from("+123.45");
        DateValueFacts excel = DateValueFacts.from("25569");

        assertTrue(decimal.isDecimal());
        assertTrue(decimal.hasPeriods());
        assertTrue(decimal.invalidForDateTokens());
        assertTrue(excel.isExcelLocalDate());
        assertTrue(decimal.isExcelLocalDateTime());
        assertEquals(null, DateValueFacts.from(" "));
    }
}

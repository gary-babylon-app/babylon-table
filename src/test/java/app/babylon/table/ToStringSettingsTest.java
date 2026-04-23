package app.babylon.table;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

class ToStringSettingsTest
{
    @Test
    void shouldUseConfiguredFormattersAndFlags()
    {
        ToStringSettings settings = new ToStringSettings();
        DateTimeFormatter dateFormatter = DateTimeFormatter.BASIC_ISO_DATE;
        DecimalFormat decimalFormat = new DecimalFormat("0.000");

        assertSame(settings, settings.withDateFormatter(dateFormatter));
        assertSame(settings, settings.withStripTrailingZeros(false));
        assertSame(settings, settings.withDecimalFormatter(decimalFormat));

        assertSame(dateFormatter, settings.getDateFormatter(DateTimeFormatter.ISO_DATE));
        assertSame(decimalFormat, settings.getDecimalFormatter(ToStringSettings.STANDARD_DECIMAL_FORMAT));
        assertFalse(settings.isStripTrailingZeros());
    }

    @Test
    void shouldFallbackWhenFormattersAreNull()
    {
        ToStringSettings settings = new ToStringSettings().withDateFormatter(null).withDecimalFormatter(null);
        DateTimeFormatter fallbackDateFormatter = DateTimeFormatter.ISO_DATE_TIME;
        DecimalFormat fallbackDecimalFormat = new DecimalFormat("0.0");

        assertSame(fallbackDateFormatter, settings.getDateFormatter(fallbackDateFormatter));
        assertSame(fallbackDecimalFormat, settings.getDecimalFormatter(fallbackDecimalFormat));
        assertTrue(ToStringSettings.standard().isStripTrailingZeros());
    }
}

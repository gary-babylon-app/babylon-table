package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableDescription;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class TransformToLocalDateTest
{
    @Test
    void shouldInferSharedDmyFormatAcrossAllSelectedColumns()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TRADE_DATE");
        final ColumnName SETTLE_DATE = ColumnName.of("SETTLE_DATE");
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        tradeDates.add("15/02/2026");
        tradeDates.add("16/02/2026");

        ColumnObject.Builder<String> settleDates = ColumnObject.builder(SETTLE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        settleDates.add("17/02/2026");
        settleDates.add("18/02/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t"), description, (Column) tradeDates.build(),
                (Column) settleDates.build());
        TableColumnar transformed = table.apply(new TransformToLocalDate(TRADE_DATE, SETTLE_DATE));

        assertEquals(LocalDate.of(2026, 2, 15), transformed.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 2, 16), transformed.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(LocalDate.of(2026, 2, 17), transformed.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 2, 18), transformed.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(1));
    }

    @Test
    void shouldThrowWhenAllSelectedColumnsRemainUnknownAndNoFallbackIsProvided()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TRADE_DATE");
        final ColumnName SETTLE_DATE = ColumnName.of("SETTLE_DATE");
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        tradeDates.add("01/02/2026");
        tradeDates.add("03/04/2026");

        ColumnObject.Builder<String> settleDates = ColumnObject.builder(SETTLE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        settleDates.add("02/03/2026");
        settleDates.add("04/05/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t2"), description, (Column) tradeDates.build(),
                (Column) settleDates.build());

        assertThrows(IllegalArgumentException.class,
                () -> table.apply(new TransformToLocalDate(TRADE_DATE, SETTLE_DATE)));
    }

    @Test
    void shouldUseFallbackDateFormatWhenAllSelectedColumnsAreAmbiguous()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TRADE_DATE");
        final ColumnName SETTLE_DATE = ColumnName.of("SETTLE_DATE");
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        tradeDates.add("01/02/2026");
        tradeDates.add("03/04/2026");

        ColumnObject.Builder<String> settleDates = ColumnObject.builder(SETTLE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        settleDates.add("05/06/2026");
        settleDates.add("07/08/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t3"), description, (Column) tradeDates.build(),
                (Column) settleDates.build());
        TableColumnar transformed = table.apply(new TransformToLocalDate(DateFormat.DMY, TRADE_DATE, SETTLE_DATE));

        assertEquals(LocalDate.of(2026, 2, 1), transformed.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 3), transformed.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(LocalDate.of(2026, 6, 5), transformed.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 8, 7), transformed.getObject(SETTLE_DATE, ColumnTypes.LOCALDATE).get(1));
    }

    @Test
    void shouldBackfillAmbiguousColumnFromDominantFormatWhenAnotherColumnUsesDifferentIdentifiableFormat()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TRADE_DATE");
        final ColumnName SETTLE_DATE = ColumnName.of("SETTLE_DATE");
        final ColumnName END_DATE = ColumnName.of("END_DATE");
        final ColumnName BOOKING_DATE = ColumnName.of("BOOKING_DATE");
        ColumnObject.Builder<String> dominantTradeDates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        dominantTradeDates.add("15/02/2026");
        dominantTradeDates.add("16/02/2026");

        ColumnObject.Builder<String> dominantSettleDates = ColumnObject.builder(SETTLE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        dominantSettleDates.add("17/02/2026");
        dominantSettleDates.add("18/02/2026");

        ColumnObject.Builder<String> isoEndDates = ColumnObject.builder(END_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        isoEndDates.add("2026-03-01");
        isoEndDates.add("2026-03-02");

        ColumnObject.Builder<String> ambiguousDates = ColumnObject.builder(BOOKING_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        ambiguousDates.add("01/02/2026");
        ambiguousDates.add("03/02/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t4"), description, (Column) dominantTradeDates.build(),
                (Column) dominantSettleDates.build(), (Column) isoEndDates.build(), (Column) ambiguousDates.build());
        TableColumnar transformed = table
                .apply(new TransformToLocalDate(TRADE_DATE, SETTLE_DATE, END_DATE, BOOKING_DATE));

        assertEquals(LocalDate.of(2026, 2, 1), transformed.getObject(BOOKING_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 2, 3), transformed.getObject(BOOKING_DATE, ColumnTypes.LOCALDATE).get(1));
        assertEquals(LocalDate.of(2026, 3, 1), transformed.getObject(END_DATE, ColumnTypes.LOCALDATE).get(0));
    }

    @Test
    void shouldParseIsoYmdDates()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TRADE_DATE");
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        tradeDates.add("2026-02-01");
        tradeDates.add("2026-04-03");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t5"), description, (Column) tradeDates.build());
        TableColumnar transformed = table.apply(new TransformToLocalDate(TRADE_DATE));

        assertEquals(LocalDate.of(2026, 2, 1), transformed.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 4, 3), transformed.getObject(TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
    }

    @Test
    void shouldTransformEmptySelectedDateColumnWithoutFallback()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TRADE_DATE");
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t6"), description, (Column) tradeDates.build());
        TableColumnar transformed = table.apply(new TransformToLocalDate(TRADE_DATE));

        assertEquals(0, transformed.getRowCount());
        assertEquals(LocalDate.class, transformed.get(TRADE_DATE).getType().getValueClass());
    }

    @Test
    void shouldCreateTransformFromThreeFactoryParams()
    {
        final ColumnName TRADE_DATE = ColumnName.of("TRADE_DATE");
        final ColumnName PARSED_TRADE_DATE = ColumnName.of("PARSED_TRADE_DATE");
        TransformToLocalDate transform = TransformToLocalDate.of("trade_date", "parsed_trade_date", "DMY");

        assertNotNull(transform);

        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(TRADE_DATE,
                app.babylon.table.column.ColumnTypes.STRING);
        tradeDates.add("15/02/2026");
        tradeDates.add("16/02/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t7"), description, (Column) tradeDates.build());
        TableColumnar transformed = table.apply(transform);

        assertEquals(LocalDate.of(2026, 2, 15), transformed.getObject(PARSED_TRADE_DATE, ColumnTypes.LOCALDATE).get(0));
        assertEquals(LocalDate.of(2026, 2, 16), transformed.getObject(PARSED_TRADE_DATE, ColumnTypes.LOCALDATE).get(1));
    }
}

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

class TransformToLocalDateFormatInferenceTest
{
    @Test
    void shouldInferSharedDmyFormatAcrossAllSelectedColumns()
    {
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        tradeDates.add("15/02/2026");
        tradeDates.add("16/02/2026");

        ColumnObject.Builder<String> settleDates = ColumnObject.builder(ColumnName.of("settle_date"), String.class);
        settleDates.add("17/02/2026");
        settleDates.add("18/02/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t"), description, (Column) tradeDates.build(),
                (Column) settleDates.build());
        TableColumnar transformed = table
                .apply(new TransformToLocalDate(ColumnName.of("trade_date"), ColumnName.of("settle_date")));

        assertEquals(LocalDate.of(2026, 2, 15),
                transformed.getTyped(ColumnName.of("trade_date"), LocalDate.class).get(0));
        assertEquals(LocalDate.of(2026, 2, 16),
                transformed.getTyped(ColumnName.of("trade_date"), LocalDate.class).get(1));
        assertEquals(LocalDate.of(2026, 2, 17),
                transformed.getTyped(ColumnName.of("settle_date"), LocalDate.class).get(0));
        assertEquals(LocalDate.of(2026, 2, 18),
                transformed.getTyped(ColumnName.of("settle_date"), LocalDate.class).get(1));
    }

    @Test
    void shouldThrowWhenAllSelectedColumnsRemainUnknownAndNoFallbackIsProvided()
    {
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        tradeDates.add("01/02/2026");
        tradeDates.add("03/04/2026");

        ColumnObject.Builder<String> settleDates = ColumnObject.builder(ColumnName.of("settle_date"), String.class);
        settleDates.add("02/03/2026");
        settleDates.add("04/05/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t2"), description, (Column) tradeDates.build(),
                (Column) settleDates.build());

        assertThrows(IllegalArgumentException.class,
                () -> table.apply(new TransformToLocalDate(ColumnName.of("trade_date"), ColumnName.of("settle_date"))));
    }

    @Test
    void shouldUseFallbackDateFormatWhenAllSelectedColumnsAreAmbiguous()
    {
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        tradeDates.add("01/02/2026");
        tradeDates.add("03/04/2026");

        ColumnObject.Builder<String> settleDates = ColumnObject.builder(ColumnName.of("settle_date"), String.class);
        settleDates.add("05/06/2026");
        settleDates.add("07/08/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t3"), description, (Column) tradeDates.build(),
                (Column) settleDates.build());
        TableColumnar transformed = table.apply(
                new TransformToLocalDate(DateFormat.DMY, ColumnName.of("trade_date"), ColumnName.of("settle_date")));

        assertEquals(LocalDate.of(2026, 2, 1),
                transformed.getTyped(ColumnName.of("trade_date"), LocalDate.class).get(0));
        assertEquals(LocalDate.of(2026, 4, 3),
                transformed.getTyped(ColumnName.of("trade_date"), LocalDate.class).get(1));
        assertEquals(LocalDate.of(2026, 6, 5),
                transformed.getTyped(ColumnName.of("settle_date"), LocalDate.class).get(0));
        assertEquals(LocalDate.of(2026, 8, 7),
                transformed.getTyped(ColumnName.of("settle_date"), LocalDate.class).get(1));
    }

    @Test
    void shouldBackfillAmbiguousColumnFromDominantFormatWhenAnotherColumnUsesDifferentIdentifiableFormat()
    {
        ColumnObject.Builder<String> dominantTradeDates = ColumnObject.builder(ColumnName.of("trade_date"),
                String.class);
        dominantTradeDates.add("15/02/2026");
        dominantTradeDates.add("16/02/2026");

        ColumnObject.Builder<String> dominantSettleDates = ColumnObject.builder(ColumnName.of("settle_date"),
                String.class);
        dominantSettleDates.add("17/02/2026");
        dominantSettleDates.add("18/02/2026");

        ColumnObject.Builder<String> isoEndDates = ColumnObject.builder(ColumnName.of("end_date"), String.class);
        isoEndDates.add("2026-03-01");
        isoEndDates.add("2026-03-02");

        ColumnObject.Builder<String> ambiguousDates = ColumnObject.builder(ColumnName.of("booking_date"), String.class);
        ambiguousDates.add("01/02/2026");
        ambiguousDates.add("03/02/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t4"), description, (Column) dominantTradeDates.build(),
                (Column) dominantSettleDates.build(), (Column) isoEndDates.build(), (Column) ambiguousDates.build());
        TableColumnar transformed = table.apply(new TransformToLocalDate(ColumnName.of("trade_date"),
                ColumnName.of("settle_date"), ColumnName.of("end_date"), ColumnName.of("booking_date")));

        assertEquals(LocalDate.of(2026, 2, 1),
                transformed.getTyped(ColumnName.of("booking_date"), LocalDate.class).get(0));
        assertEquals(LocalDate.of(2026, 2, 3),
                transformed.getTyped(ColumnName.of("booking_date"), LocalDate.class).get(1));
        assertEquals(LocalDate.of(2026, 3, 1), transformed.getTyped(ColumnName.of("end_date"), LocalDate.class).get(0));
    }

    @Test
    void shouldParseIsoYmdDates()
    {
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        tradeDates.add("2026-02-01");
        tradeDates.add("2026-04-03");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t5"), description, (Column) tradeDates.build());
        TableColumnar transformed = table.apply(new TransformToLocalDate(ColumnName.of("trade_date")));

        assertEquals(LocalDate.of(2026, 2, 1),
                transformed.getTyped(ColumnName.of("trade_date"), LocalDate.class).get(0));
        assertEquals(LocalDate.of(2026, 4, 3),
                transformed.getTyped(ColumnName.of("trade_date"), LocalDate.class).get(1));
    }

    @Test
    void shouldTransformEmptySelectedDateColumnWithoutFallback()
    {
        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(ColumnName.of("trade_date"), String.class);

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t6"), description, (Column) tradeDates.build());
        TableColumnar transformed = table.apply(new TransformToLocalDate(ColumnName.of("trade_date")));

        assertEquals(0, transformed.getRowCount());
        assertEquals(LocalDate.class, transformed.get(ColumnName.of("trade_date")).getType().getValueClass());
    }

    @Test
    void shouldCreateTransformFromThreeFactoryParams()
    {
        TransformToLocalDate transform = TransformToLocalDate.of("trade_date", "parsed_trade_date", "DMY");

        assertNotNull(transform);

        ColumnObject.Builder<String> tradeDates = ColumnObject.builder(ColumnName.of("trade_date"), String.class);
        tradeDates.add("15/02/2026");
        tradeDates.add("16/02/2026");

        TableDescription description = new TableDescription("Description here...");
        TableColumnar table = Tables.newTable(TableName.of("t7"), description, (Column) tradeDates.build());
        TableColumnar transformed = table.apply(transform);

        assertEquals(LocalDate.of(2026, 2, 15),
                transformed.getTyped(ColumnName.of("parsed_trade_date"), LocalDate.class).get(0));
        assertEquals(LocalDate.of(2026, 2, 16),
                transformed.getTyped(ColumnName.of("parsed_trade_date"), LocalDate.class).get(1));
    }
}

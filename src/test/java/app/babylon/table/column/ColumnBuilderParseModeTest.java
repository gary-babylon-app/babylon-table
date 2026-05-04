package app.babylon.table.column;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import app.babylon.text.Sentence.ParseMode;

class ColumnBuilderParseModeTest
{
    @Test
    void primitiveBuilderShouldUseFirstInParseMode()
    {
        Column.Builder builder = Columns.newBuilder(ColumnName.of("Quantity"), ColumnTypes.INT);

        String description = "Buy 100 AAPL";
        builder.add(ParseMode.FIRST_IN, description);

        ColumnInt quantity = (ColumnInt) builder.build();
        assertEquals(ColumnName.of("Quantity"), quantity.getName());
        assertEquals(100, quantity.get(0));
    }

    @Test
    void primitiveBuilderShouldUseLastInParseMode()
    {
        Column.Builder builder = Columns.newBuilder(ColumnName.of("Quantity"), ColumnTypes.INT);

        String description = "Buy 100 AAPL split into 25";
        builder.add(ParseMode.LAST_IN, description, 0, description.length());

        ColumnInt quantity = (ColumnInt) builder.build();
        assertEquals(25, quantity.get(0));
    }

    @Test
    void primitiveBuilderShouldUseOnlyInParseMode()
    {
        Column.Builder builder = Columns.newBuilder(ColumnName.of("Quantity"), ColumnTypes.INT);

        String singleQuantity = "Sell 75 MSFT";
        String multipleQuantities = "Buy 100 AAPL split into 25";
        builder.add(ParseMode.ONLY_IN, singleQuantity, 0, singleQuantity.length());
        builder.add(ParseMode.ONLY_IN, multipleQuantities, 0, multipleQuantities.length());

        ColumnInt quantity = (ColumnInt) builder.build();
        assertEquals(75, quantity.get(0));
        assertFalse(quantity.isSet(1));
    }

    @Test
    void shouldExtractQuantityFromDescriptionRows()
    {
        ColumnName description = ColumnName.of("Description");
        ColumnName quantity = ColumnName.of("Quantity");
        ColumnObject.Builder<String> descriptions = ColumnObject.builder(description, ColumnTypes.STRING);
        descriptions.add("Buy 100 AAPL");
        descriptions.add("Sell 75 MSFT");
        descriptions.add("Hold AAPL");

        Column.Builder quantities = Columns.newBuilder(quantity, ColumnTypes.INT);
        ColumnObject<String> descriptionColumn = descriptions.build();
        for (int i = 0; i < descriptionColumn.size(); ++i)
        {
            String row = descriptionColumn.get(i);
            quantities.add(ParseMode.FIRST_IN, row, 0, row.length());
        }

        ColumnInt quantityColumn = (ColumnInt) quantities.build();
        assertEquals(quantity, quantityColumn.getName());
        assertEquals(100, quantityColumn.get(0));
        assertEquals(75, quantityColumn.get(1));
        assertFalse(quantityColumn.isSet(2));
    }
}

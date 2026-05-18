package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

public class TransformClassifyTest
{
    @Test
    public void shouldClassifyPlainStringColumn()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName INDICATOR = ColumnName.of("Indicator");

        ColumnObject.Builder<String> strings = ColumnObject.builder(DESCRIPTION, ColumnTypes.STRING);
        strings.add("ABC (VEVE)");
        strings.add("No match");
        strings.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(new TransformClassify(DESCRIPTION, INDICATOR, Pattern.compile("\\([^)]+\\)"), "Y", "N"));

        ColumnObject<String> classified = transformed.getString(INDICATOR);
        assertEquals("Y", classified.get(0));
        assertEquals("N", classified.get(1));
        assertFalse(classified.isSet(2));
    }

    @Test
    public void shouldPreserveCategoricalShape()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName INDICATOR = ColumnName.of("Indicator");

        ColumnCategorical.Builder<String> strings = ColumnCategorical.builder(DESCRIPTION, ColumnTypes.STRING);
        strings.add("ABC (VEVE)");
        strings.add("ABC (VEVE)");
        strings.add("No match");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(new TransformClassify(DESCRIPTION, INDICATOR, Pattern.compile("\\([^)]+\\)"), "Y", "N"));

        assertTrue(transformed.get(INDICATOR) instanceof ColumnCategorical<?>);
        ColumnCategorical<String> classified = transformed.getCategorical(INDICATOR);
        assertEquals("Y", classified.get(0));
        assertEquals("Y", classified.get(1));
        assertEquals("N", classified.get(2));
        assertEquals(classified.getCategoryCode(0), classified.getCategoryCode(1));
    }

    @Test
    public void shouldClassifyByAnchorTokens()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName FEE_KIND = ColumnName.of("FeeKind");

        ColumnObject.Builder<String> strings = ColumnObject.builder(DESCRIPTION, ColumnTypes.STRING);
        strings.add("Broker Commission SAGB R187 10.5% 21/12/26 @ 0.25%");
        strings.add("Settlement and administration");
        strings.add("Value Added Tax on costs (VAT)");
        strings.add("Value was updated in the account after withholding tax");
        strings.add("(VAT)");

        TableColumnar table = Tables.newTable(TableName.of("t"), strings.build());

        TableColumnar transformed = table
                .apply(TransformClassify.anchor(DESCRIPTION, FEE_KIND, "commission", "Commission", null),
                        TransformClassify.anchor(DESCRIPTION, FEE_KIND, "settlement administration", "SettlementAdmin",
                                null),
                        TransformClassify.anchor(DESCRIPTION, FEE_KIND, "value tax", "VAT", null),
                        TransformClassify.anchor(DESCRIPTION, FEE_KIND, "vat", "VATOnly", null));

        ColumnObject<String> classified = transformed.getString(FEE_KIND);
        assertEquals("Commission", classified.get(0));
        assertEquals("SettlementAdmin", classified.get(1));
        assertEquals("VAT", classified.get(2));
        assertFalse(classified.isSet(3));
        assertEquals("VATOnly", classified.get(4));
    }

    @Test
    public void shouldPreserveExistingClassifications()
    {
        final ColumnName DESCRIPTION = ColumnName.of("Description");
        final ColumnName FEE_KIND = ColumnName.of("FeeKind");

        ColumnObject.Builder<String> descriptions = ColumnObject.builder(DESCRIPTION, ColumnTypes.STRING);
        descriptions.add("Broker Commission");
        descriptions.add("Settlement and administration");

        ColumnObject.Builder<String> feeKinds = ColumnObject.builder(FEE_KIND, ColumnTypes.STRING);
        feeKinds.add("Existing");
        feeKinds.addNull();

        TableColumnar table = Tables.newTable(TableName.of("t"), descriptions.build(), feeKinds.build());

        TableColumnar transformed = table.apply(
                TransformClassify.anchor(DESCRIPTION, FEE_KIND, "commission", "Commission", null),
                TransformClassify.anchor(DESCRIPTION, FEE_KIND, "settlement administration", "SettlementAdmin", null));

        ColumnObject<String> classified = transformed.getString(FEE_KIND);
        assertEquals("Existing", classified.get(0));
        assertEquals("SettlementAdmin", classified.get(1));
    }

    @Test
    public void shouldBeAvailableFromBaseRegistry()
    {
        Transform transform = Transforms.registry().create("Classify", "Description", "Indicator", "\\([^)]+\\)", "Y",
                "N");

        assertTrue(transform instanceof TransformClassify);
    }
}

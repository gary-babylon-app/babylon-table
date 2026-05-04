package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnBoolean;
import app.babylon.table.column.ColumnInt;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;
import app.babylon.table.dsl.ComparisonCondition;
import app.babylon.table.dsl.ConditionExpression;
import app.babylon.table.dsl.Conditions;

class TransformFlagTest
{
    @Test
    void shouldCreateBooleanFlagFromStringComparison()
    {
        ColumnName side = ColumnName.of("Side");
        ColumnName isBuy = ColumnName.of("IsBuy");
        ColumnObject.Builder<String> sides = ColumnObject.builder(side, ColumnTypes.STRING);
        sides.add("Buy");
        sides.add("Sell");
        sides.addNull();
        TableColumnar table = Tables.newTable(TableName.of("t"), sides.build());

        TableColumnar transformed = table.apply(TransformFlag
                .builder(new ComparisonCondition(side, app.babylon.table.column.Column.Operator.EQUAL, "Buy"))
                .withNewColumnName(isBuy).build());

        ColumnBoolean flag = transformed.getBoolean(isBuy);
        assertEquals(true, flag.get(0));
        assertEquals(false, flag.get(1));
        assertEquals(false, flag.get(2));
    }

    @Test
    void shouldCreateBooleanFlagFromPrimitiveComparison()
    {
        ColumnName quantity = ColumnName.of("Quantity");
        ColumnName isLarge = ColumnName.of("IsLarge");
        ColumnInt.Builder quantities = ColumnInt.builder(quantity);
        quantities.add(99);
        quantities.add(100);
        quantities.add(101);
        TableColumnar table = Tables.newTable(TableName.of("t"), quantities.build());

        TableColumnar transformed = table.apply(TransformFlag
                .builder(new ComparisonCondition(quantity,
                        app.babylon.table.column.Column.Operator.GREATER_THAN_OR_EQUAL, "100"))
                .withNewColumnName(isLarge).build());

        ColumnBoolean flag = transformed.getBoolean(isLarge);
        assertEquals(false, flag.get(0));
        assertEquals(true, flag.get(1));
        assertEquals(true, flag.get(2));
    }

    @Test
    void shouldCreateBooleanFlagFromInAndNotIn()
    {
        ColumnName side = ColumnName.of("Side");
        ColumnName isOther = ColumnName.of("IsOther");
        ColumnObject.Builder<String> sides = ColumnObject.builder(side, ColumnTypes.STRING);
        sides.add("Buy");
        sides.add("Hold");
        sides.add("Sell");
        TableColumnar table = Tables.newTable(TableName.of("t"), sides.build());

        TableColumnar transformed = table.apply(TransformFlag
                .builder(new ComparisonCondition(side, app.babylon.table.column.Column.Operator.NOT_IN, "Buy", "Sell"))
                .withNewColumnName(isOther).build());

        ColumnBoolean flag = transformed.getBoolean(isOther);
        assertEquals(false, flag.get(0));
        assertEquals(true, flag.get(1));
        assertEquals(false, flag.get(2));
    }

    @Test
    void shouldCreateBooleanFlagFromLogicalConditions()
    {
        ColumnName side = ColumnName.of("Side");
        ColumnName quantity = ColumnName.of("Quantity");
        ColumnName isLargeBuy = ColumnName.of("IsLargeBuy");
        ColumnObject.Builder<String> sides = ColumnObject.builder(side, ColumnTypes.STRING);
        sides.add("Buy");
        sides.add("Buy");
        sides.add("Sell");
        ColumnInt.Builder quantities = ColumnInt.builder(quantity);
        quantities.add(99);
        quantities.add(100);
        quantities.add(200);
        TableColumnar table = Tables.newTable(TableName.of("t"), sides.build(), quantities.build());
        ConditionExpression condition = Conditions.column(side).is("Buy").and(Conditions.column(quantity).gte("100"));

        TableColumnar transformed = table.apply(TransformFlag.builder(condition).withNewColumnName(isLargeBuy).build());

        ColumnBoolean flag = transformed.getBoolean(isLargeBuy);
        assertEquals(false, flag.get(0));
        assertEquals(true, flag.get(1));
        assertEquals(false, flag.get(2));
    }

    @Test
    void shouldBuildConditionsFluently()
    {
        ColumnName side = ColumnName.of("Side");
        ColumnName quantity = ColumnName.of("Quantity");

        ConditionExpression condition = Conditions.column(side).in("Buy", "Sell")
                .or(Conditions.column(quantity).lt("10"));

        assertEquals("Side in Buy, Sell or Quantity < 10", condition.toDsl());
    }

}

package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.Column;
import app.babylon.table.ColumnName;
import app.babylon.table.ColumnObject;
import app.babylon.table.Is;
import app.babylon.text.Split;

class TransformDecimalBinaryOperator extends TransformBase
{
    protected enum OPERATOR
    {
        Add, Subtract, Multiply, Divide
    };

    private final ColumnName newColumnName;
    private final ColumnName leftColumnName;
    private final ColumnName rightColumnName;
    private final OPERATOR operator;

    public TransformDecimalBinaryOperator(ColumnName leftColumnName, OPERATOR operator, ColumnName rightColumnName,
            ColumnName newColumnName)
    {
        super(operator.toString());
        this.leftColumnName = ArgumentChecks.nonNull(leftColumnName);
        this.operator = ArgumentChecks.nonNull(operator);
        this.rightColumnName = ArgumentChecks.nonNull(rightColumnName);
        this.newColumnName = ArgumentChecks.nonNull(newColumnName);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> leftColumn = (ColumnObject<BigDecimal>) columnsByName.get(this.leftColumnName);
        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> rightColumn = (ColumnObject<BigDecimal>) columnsByName.get(this.rightColumnName);

        if (leftColumn == null)
        {
            throw new RuntimeException("Left column invalid " + leftColumn);
        }
        if (rightColumn == null)
        {
            throw new RuntimeException("Right column invalid " + rightColumn);
        }

        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(newColumnName);
        for (int i = 0; i < leftColumn.size(); ++i)
        {
            BigDecimal left = leftColumn.get(i);
            BigDecimal right = rightColumn.get(i);
            if (left != null && right != null)
            {
                BigDecimal newValue = null;
                switch (this.operator)
                {
                    case Subtract :
                        newValue = left.subtract(right, MathContext.DECIMAL64);
                        break;
                    case Add :
                        newValue = left.add(right, MathContext.DECIMAL64);
                        break;
                    case Multiply :
                        newValue = left.multiply(right, MathContext.DECIMAL64);
                        break;
                    case Divide :
                        newValue = left.divide(right, MathContext.DECIMAL64);
                        break;
                    default :
                        throw new RuntimeException("Unknown operator. " + this.operator);
                }
                newColumn.add(newValue);
            } else
            {
                newColumn.addNull();
            }
        }

        columnsByName.put(newColumnName, (ColumnObject<BigDecimal>) newColumn.build());
    }

    public static TransformDecimalBinaryOperator of(String s)
    {
        int indexOf = s.indexOf('(');
        if (indexOf < 1)
        {
            throw new RuntimeException("Invalid function name " + s);
        }

        final String FUNCTION_NAME = s.substring(0, indexOf);

        if (Strings.isEmpty(s) || s.length() < FUNCTION_NAME.length() + 3)
        {
            throw new RuntimeException("Invalid string for " + FUNCTION_NAME + " construction.");
        }

        s = s.substring(FUNCTION_NAME.length() + 1, s.length() - 1);

        String[] params = Split.commaSeparatedParams(s);

        if (params.length < 3)
        {
            throw new RuntimeException("Invalid parameter list count for " + FUNCTION_NAME + " construction " + s);
        }
        int index = 0;
        ColumnName leftColumnName = ColumnName.of(params[index++]);
        ColumnName rightColumnName = ColumnName.of(params[index++]);
        ColumnName newColumnName = ColumnName.of(params[index++]);

        OPERATOR operator = OPERATOR.valueOf(FUNCTION_NAME);

        return new TransformDecimalBinaryOperator(leftColumnName, operator, rightColumnName, newColumnName);
    }

    @Override
    public String toString()
    {
        return this.operator.toString() + "(" + leftColumnName + ", " + rightColumnName + ", " + newColumnName + ")";
    }

}

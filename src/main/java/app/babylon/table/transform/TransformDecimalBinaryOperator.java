package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.text.Strings;

public class TransformDecimalBinaryOperator extends TransformBase
{
    protected enum OPERATOR
    {
        Add, Subtract, Multiply, Divide
    };

    public static record Operand(ColumnName columnName, BigDecimal value)
    {
        public Operand
        {
            if (columnName == null && value == null)
            {
                throw new RuntimeException("Decimal operand requires a column or literal value.");
            }
            if (columnName != null && value != null)
            {
                throw new RuntimeException("Decimal operand can not be both a column and literal value.");
            }
        }

        public static Operand column(ColumnName columnName)
        {
            return new Operand(ArgumentCheck.nonNull(columnName), null);
        }

        public static Operand value(BigDecimal value)
        {
            return new Operand(null, canonical(ArgumentCheck.nonNull(value)));
        }

        public boolean isColumn()
        {
            return this.columnName != null;
        }
    }

    private final ColumnName newColumnName;
    private final Operand left;
    private final Operand right;
    private final OPERATOR operator;

    public TransformDecimalBinaryOperator(ColumnName leftColumnName, OPERATOR operator, ColumnName rightColumnName,
            ColumnName newColumnName)
    {
        this(Operand.column(leftColumnName), operator, Operand.column(rightColumnName), newColumnName);
    }

    public TransformDecimalBinaryOperator(Operand left, OPERATOR operator, Operand right, ColumnName newColumnName)
    {
        super(operator.toString());
        this.left = ArgumentCheck.nonNull(left);
        this.operator = ArgumentCheck.nonNull(operator);
        this.right = ArgumentCheck.nonNull(right);
        if (!this.left.isColumn() && !this.right.isColumn())
        {
            throw new RuntimeException("Decimal binary operator requires at least one column operand.");
        }
        this.newColumnName = ArgumentCheck.nonNull(newColumnName);
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> leftColumn = this.left.isColumn()
                ? (ColumnObject<BigDecimal>) columnsByName.get(this.left.columnName())
                : null;
        @SuppressWarnings("unchecked")
        ColumnObject<BigDecimal> rightColumn = this.right.isColumn()
                ? (ColumnObject<BigDecimal>) columnsByName.get(this.right.columnName())
                : null;

        if (this.left.isColumn() && leftColumn == null)
        {
            throw new RuntimeException("Left column invalid " + this.left.columnName());
        }
        if (this.right.isColumn() && rightColumn == null)
        {
            throw new RuntimeException("Right column invalid " + this.right.columnName());
        }

        ColumnObject.Builder<BigDecimal> newColumn = ColumnObject.builderDecimal(newColumnName);
        int size = leftColumn == null ? rightColumn.size() : leftColumn.size();
        for (int i = 0; i < size; ++i)
        {
            BigDecimal left = value(leftColumn, this.left, i);
            BigDecimal right = value(rightColumn, this.right, i);
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
            }
            else
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

        String[] params = Strings.split(s);

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

    public ColumnName leftColumnName()
    {
        return this.left.columnName();
    }

    public ColumnName rightColumnName()
    {
        return this.right.columnName();
    }

    public Operand left()
    {
        return this.left;
    }

    public Operand right()
    {
        return this.right;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    private BigDecimal value(ColumnObject<BigDecimal> column, Operand operand, int row)
    {
        return operand.isColumn() ? column.get(row) : operand.value();
    }

    @Override
    public String toString()
    {
        return this.operator.toString() + "(" + operand(this.left) + ", " + operand(this.right) + ", " + newColumnName
                + ")";
    }

    private String operand(Operand operand)
    {
        return operand.isColumn() ? operand.columnName().toString() : operand.value().toPlainString();
    }

    private static BigDecimal canonical(BigDecimal value)
    {
        return TransformNormalise.normalise(value);
    }
}

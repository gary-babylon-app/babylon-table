package app.babylon.table.transform;

import app.babylon.table.column.ColumnName;

public class TransformMultiply extends TransformDecimalBinaryOperator
{
    public static final String FUNCTION_NAME = "Multiply";

    public TransformMultiply(ColumnName leftColumnName, ColumnName rightColumnName, ColumnName newColumnName)
    {
        super(leftColumnName, OPERATOR.Multiply, rightColumnName, newColumnName);
    }

    public static TransformMultiply of(ColumnName leftColumnName, ColumnName rightColumnName, ColumnName newColumnName)
    {
        return new TransformMultiply(leftColumnName, rightColumnName, newColumnName);
    }

    public static TransformMultiply of(Operand left, Operand right, ColumnName newColumnName)
    {
        return new TransformMultiply(left, right, newColumnName);
    }

    private TransformMultiply(Operand left, Operand right, ColumnName newColumnName)
    {
        super(left, OPERATOR.Multiply, right, newColumnName);
    }
}

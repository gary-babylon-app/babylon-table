package app.babylon.table.transform;

import app.babylon.table.ColumnName;

public class TransformMultiply extends TransformDecimalBinaryOperator
{
    public static final String FUNCTION_NAME = "Multiply";

    public TransformMultiply(ColumnName leftColumnName, ColumnName rightColumnName, ColumnName newColumnName)
    {
        super(leftColumnName, OPERATOR.Multiply, rightColumnName, newColumnName);
    }
}

package app.babylon.table.transform;

import app.babylon.table.column.ColumnName;

public class TransformSubtract extends TransformDecimalBinaryOperator
{
    public static final String FUNCTION_NAME = "Subtract";

    public TransformSubtract(ColumnName leftColumnName, ColumnName rightColumnName, ColumnName newColumnName)
    {
        super(leftColumnName, OPERATOR.Subtract, rightColumnName, newColumnName);
    }
}

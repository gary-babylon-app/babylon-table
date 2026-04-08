package app.babylon.table.transform;

import app.babylon.table.ColumnName;

public class TransformDivide extends TransformDecimalBinaryOperator
{
    public static final String FUNCTION_NAME = "Divide";

    public TransformDivide(ColumnName leftColumnName, ColumnName rightColumnName, ColumnName newColumnName)
    {
        super(leftColumnName, OPERATOR.Divide, rightColumnName, newColumnName);
    }
}

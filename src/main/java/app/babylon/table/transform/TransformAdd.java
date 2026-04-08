package app.babylon.table.transform;

import app.babylon.table.ColumnName;

public class TransformAdd extends TransformDecimalBinaryOperator
{
    public static final String FUNCTION_NAME = "Add";

    public TransformAdd(ColumnName leftColumnName, ColumnName rightColumnName, ColumnName newColumnName)
    {
        super(leftColumnName, OPERATOR.Add, rightColumnName, newColumnName);
    }
}

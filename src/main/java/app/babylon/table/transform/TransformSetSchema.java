package app.babylon.table.transform;

import app.babylon.table.column.ColumnName;

/**
 * Column names for the legacy table-backed transform registry format.
 *
 * @deprecated Store transform DSL statements instead. The fixed Param1-Param5
 *             schema belongs to the deprecated {@link TransformRegistry} path
 *             and is expected to be removed in a future release.
 */
@Deprecated(since = "0.3.22", forRemoval = true)
public class TransformSetSchema
{
    public static final String BABYLON = "Babylon";

    public static final String TABLE_NAME = "transform_sets";

    public static final ColumnName TYPE = ColumnName.of("Type");
    public static final ColumnName SET_NAME = ColumnName.of("SetName");
    public static final ColumnName STEP_ORDER = ColumnName.of("StepOrder");
    public static final ColumnName TRANSFORM = ColumnName.of("Transform");
    public static final ColumnName PARAM1 = ColumnName.of("Param1");
    public static final ColumnName PARAM2 = ColumnName.of("Param2");
    public static final ColumnName PARAM3 = ColumnName.of("Param3");
    public static final ColumnName PARAM4 = ColumnName.of("Param4");
    public static final ColumnName PARAM5 = ColumnName.of("Param5");
}

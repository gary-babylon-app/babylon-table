package app.babylon.table.transform;

import java.util.Locale;
import java.util.Map;

import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnTypes;

public class TransformMetadataConstant extends TransformBase
{
    public static final String FUNCTION_NAME = "MetadataConstant";

    public enum Key
    {
        TABLE_NAME("metadata.tableName"), DESCRIPTION("metadata.description");

        private final String dslName;

        Key(String dslName)
        {
            this.dslName = dslName;
        }

        public String dslName()
        {
            return this.dslName;
        }

        public static Key parse(String value)
        {
            String normalised = value == null ? "" : value.toLowerCase(Locale.ROOT);
            if ("metadata.tablename".equals(normalised))
            {
                return TABLE_NAME;
            }
            if ("metadata.description".equals(normalised))
            {
                return DESCRIPTION;
            }
            return null;
        }
    }

    private final Key key;
    private final ColumnName newColumnName;

    public TransformMetadataConstant(Key key, ColumnName newColumnName)
    {
        super(FUNCTION_NAME);
        this.key = key;
        this.newColumnName = newColumnName;
    }

    public static TransformMetadataConstant of(Key key, ColumnName newColumnName)
    {
        return new TransformMetadataConstant(key, newColumnName);
    }

    public Key key()
    {
        return this.key;
    }

    public ColumnName newColumnName()
    {
        return this.newColumnName;
    }

    @Override
    public void apply(SourceMetadata metadata, Map<ColumnName, Column> columnsByName)
    {
        if (columnsByName == null)
        {
            return;
        }
        SourceMetadata source = metadata == null ? new SourceMetadata("", "") : metadata;
        int rowCount = rowCount(columnsByName);
        columnsByName.put(this.newColumnName,
                ColumnCategorical.constant(this.newColumnName, value(source), rowCount, ColumnTypes.STRING));
    }

    @Override
    public void apply(Map<ColumnName, Column> columnsByName)
    {
        apply(null, columnsByName);
    }

    private String value(SourceMetadata metadata)
    {
        return switch (this.key)
        {
            case TABLE_NAME -> metadata.tableName();
            case DESCRIPTION -> metadata.description();
        };
    }
}

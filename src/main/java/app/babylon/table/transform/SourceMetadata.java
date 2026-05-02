package app.babylon.table.transform;

public record SourceMetadata(String tableName, String description)
{
    public SourceMetadata
    {
        tableName = tableName == null ? "" : tableName;
        description = description == null ? "" : description;
    }
}

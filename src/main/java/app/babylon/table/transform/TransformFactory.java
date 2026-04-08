package app.babylon.table.transform;

@FunctionalInterface
public interface TransformFactory
{
    Transform create(String... params);
}
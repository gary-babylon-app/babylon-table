package app.babylon.table.transform;

import app.babylon.table.Transform;

@FunctionalInterface
public interface TransformFactory
{
    Transform create(String... params);
}

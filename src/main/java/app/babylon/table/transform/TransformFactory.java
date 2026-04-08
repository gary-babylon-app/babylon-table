package app.babylon.table.transform;

import app.babylon.table.transform.Transform;

@FunctionalInterface
public interface TransformFactory
{
    Transform create(String... params);
}

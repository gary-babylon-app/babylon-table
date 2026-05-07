package app.babylon.table.transform;

import java.util.Arrays;
import java.util.Collection;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.TableColumnar;

public final class TransformSet
{
    private final Transform[] transforms;

    private TransformSet(Transform[] transforms)
    {
        this.transforms = Arrays.copyOf(ArgumentCheck.nonNull(transforms), transforms.length);
        for (Transform transform : this.transforms)
        {
            ArgumentCheck.nonNull(transform);
        }
    }

    public static TransformSet of(Transform... transforms)
    {
        return new TransformSet(transforms == null ? new Transform[0] : transforms);
    }

    public static TransformSet of(Collection<? extends Transform> transforms)
    {
        return transforms == null ? of() : new TransformSet(transforms.toArray(Transform[]::new));
    }

    public Transform[] transforms()
    {
        return Arrays.copyOf(this.transforms, this.transforms.length);
    }

    public <C extends Collection<? super Transform>> C transforms(C transforms)
    {
        C destination = ArgumentCheck.nonNull(transforms);
        for (Transform transform : this.transforms)
        {
            destination.add(transform);
        }
        return destination;
    }

    public TableColumnar apply(TableColumnar table)
    {
        return ArgumentCheck.nonNull(table).apply(this.transforms);
    }
}

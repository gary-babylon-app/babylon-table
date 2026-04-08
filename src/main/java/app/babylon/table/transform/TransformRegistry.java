package app.babylon.table.transform;

import app.babylon.text.Strings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import app.babylon.table.ArgumentChecks;
import app.babylon.table.ColumnObject;
import app.babylon.table.Is;
import app.babylon.table.TableColumnar;
import app.babylon.table.Transform;

public final class TransformRegistry
{
    private final Map<String, TransformFactory> factories;

    private TransformRegistry(Map<String, TransformFactory> factories)
    {
        this.factories = Map.copyOf(factories);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public Builder toBuilder()
    {
        return new Builder(this.factories);
    }

    public Transform create(String functionName, String... params)
    {
        if (Strings.isEmpty(functionName))
        {
            return null;
        }
        TransformFactory factory = factories.get(functionName);
        if (factory == null)
        {
            return null;
        }
        return factory.create(params);
    }

    public TableColumnar apply(TableColumnar table, TableColumnar transformSet)
    {
        Collection<Transform> transforms = new ArrayList<>();

        ColumnObject<String> transformNames = transformSet.getString(TransformSetSchema.TRANSFORM);
        ColumnObject<String> param1s = transformSet.getString(TransformSetSchema.PARAM1);
        ColumnObject<String> param2s = transformSet.getString(TransformSetSchema.PARAM2);
        ColumnObject<String> param3s = transformSet.getString(TransformSetSchema.PARAM3);
        ColumnObject<String> param4s = transformSet.getString(TransformSetSchema.PARAM4);
        ColumnObject<String> param5s = transformSet.getString(TransformSetSchema.PARAM5);

        for (int i = 0; i < transformSet.getRowCount(); ++i)
        {
            String functionName = transformNames.get(i);
            String param1 = param1s.get(i);
            String param2 = param2s.get(i);
            String param3 = param3s.get(i);
            String param4 = param4s.get(i);
            String param5 = param5s.get(i);

            Transform t = create(functionName, param1, param2, param3, param4, param5);
            if (t != null)
            {
                transforms.add(t);
            } else
            {
                System.out.println(functionName + " not applied, couldnt create transform");
            }
        }

        return table.apply(transforms);
    }

    public static final class Builder
    {
        private final Map<String, TransformFactory> factories;

        private Builder()
        {
            this.factories = new HashMap<>();
        }

        private Builder(Map<String, TransformFactory> factories)
        {
            this.factories = new HashMap<>(factories);
        }

        public Builder register(String functionName, TransformFactory factory)
        {
            factories.put(ArgumentChecks.nonEmpty(functionName), ArgumentChecks.nonNull(factory));
            return this;
        }

        public Builder registerAll(TransformRegistry registry)
        {
            if (registry != null)
            {
                factories.putAll(registry.factories);
            }
            return this;
        }

        public TransformRegistry build()
        {
            return new TransformRegistry(factories);
        }
    }
}

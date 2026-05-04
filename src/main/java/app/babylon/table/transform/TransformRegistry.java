package app.babylon.table.transform;

import app.babylon.lang.ArgumentCheck;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import app.babylon.table.TableColumnar;
import app.babylon.table.column.ColumnObject;
import app.babylon.text.Strings;

/**
 * Registry for the legacy table-backed transform factory format.
 *
 * @deprecated Use {@link app.babylon.table.transform.QuickTransforms} to parse
 *             persisted transform statements. The DSL replaces this fixed
 *             function-name/parameter registry and this type is expected to be
 *             removed in a future release.
 */
@Deprecated(since = "0.3.22", forRemoval = true)
public final class TransformRegistry
{
    private final Map<String, TransformFactory> factories;

    private TransformRegistry(Map<String, TransformFactory> factories)
    {
        this.factories = Map.copyOf(factories);
    }

    /**
     * @deprecated Use {@link app.babylon.table.transform.QuickTransforms} extension
     *             methods instead.
     */
    @Deprecated(since = "0.3.22", forRemoval = true)
    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * @deprecated Use {@link app.babylon.table.transform.QuickTransforms} extension
     *             methods instead.
     */
    @Deprecated(since = "0.3.22", forRemoval = true)
    public Builder toBuilder()
    {
        return new Builder(this.factories);
    }

    /**
     * @deprecated Parse a DSL statement with
     *             {@link app.babylon.table.transform.QuickTransforms} instead.
     */
    @Deprecated(since = "0.3.22", forRemoval = true)
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

    /**
     * @deprecated Store and apply DSL statements instead of the fixed
     *             {@link TransformSetSchema} table shape.
     */
    @Deprecated(since = "0.3.22", forRemoval = true)
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
            }
            else
            {
                System.out.println(functionName + " not applied, couldnt create transform");
            }
        }

        return table.apply(transforms);
    }

    /**
     * @deprecated Use {@link app.babylon.table.transform.QuickTransforms} extension
     *             methods instead.
     */
    @Deprecated(since = "0.3.22", forRemoval = true)
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

        /**
         * @deprecated Register DSL command parsers with
         *             {@link app.babylon.table.transform.QuickTransforms} instead.
         */
        @Deprecated(since = "0.3.22", forRemoval = true)
        public Builder register(String functionName, TransformFactory factory)
        {
            factories.put(ArgumentCheck.nonEmpty(functionName), ArgumentCheck.nonNull(factory));
            return this;
        }

        /**
         * @deprecated Use parser extension methods instead.
         */
        @Deprecated(since = "0.3.22", forRemoval = true)
        public Builder registerAll(TransformRegistry registry)
        {
            if (registry != null)
            {
                factories.putAll(registry.factories);
            }
            return this;
        }

        /**
         * @deprecated Use {@link app.babylon.table.transform.QuickTransforms} directly.
         */
        @Deprecated(since = "0.3.22", forRemoval = true)
        public TransformRegistry build()
        {
            return new TransformRegistry(factories);
        }
    }
}

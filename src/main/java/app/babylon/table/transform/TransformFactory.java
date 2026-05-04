package app.babylon.table.transform;

/**
 * Factory callback for the legacy {@link TransformRegistry}.
 *
 * @deprecated Use
 *             {@link app.babylon.table.transform.dsl.TransformCommandParser}
 *             and {@link app.babylon.table.transform.QuickTransforms} for
 *             custom persisted transform commands.
 */
@Deprecated(since = "0.3.22", forRemoval = true)
@FunctionalInterface
public interface TransformFactory
{
    /**
     * @deprecated Parse DSL commands with
     *             {@link app.babylon.table.transform.dsl.TransformCommandParser}
     *             instead.
     */
    @Deprecated(since = "0.3.22", forRemoval = true)
    Transform create(String... params);
}

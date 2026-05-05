package app.babylon.table.transform;

public final class Transforms
{
    @Deprecated(since = "0.3.22", forRemoval = true)
    private static volatile TransformRegistry REGISTRY;

    private Transforms()
    {
    }

    /**
     * Returns the legacy function-name/parameter registry.
     *
     * @deprecated Use {@link app.babylon.table.transform.QuickTransforms} for
     *             persisted transform definitions. The DSL is the preferred
     *             transform configuration format and this registry is expected to
     *             be removed in a future release.
     */
    @Deprecated(since = "0.3.22", forRemoval = true)
    public static TransformRegistry registry()
    {
        TransformRegistry x = REGISTRY;
        if (x == null)
        {
            synchronized (Transforms.class)
            {
                x = REGISTRY;
                if (x == null)
                {
                    x = TransformRegistry.builder().register(TransformAfter.FUNCTION_NAME, TransformAfter::of)
                            .register(TransformBefore.FUNCTION_NAME, TransformBefore::of)
                            .register(TransformClassify.FUNCTION_NAME, TransformClassify::of)
                            .register(TransformClean.FUNCTION_NAME, TransformClean::of)
                            .register(/* backward compat */"CleanWhitespace", TransformClean::of)
                            .register(TransformCoalesce.FUNCTION_NAME, TransformCoalesce::of)
                            .register(TransformConcat.FUNCTION_NAME, TransformConcat::of)
                            .register(TransformCopy.FUNCTION_NAME, TransformCopy::of)
                            .register(TransformConstant.FUNCTION_NAME, TransformConstant::of)
                            .register(/* backward compat */TransformConstant.LEGACY_FUNCTION_NAME,
                                    TransformConstant::of)
                            .register(/* backward compat */"NewConstant", TransformConstant::of)
                            .register(TransformExtract.FUNCTION_NAME, TransformExtract::of)
                            .register(TransformLeft.FUNCTION_NAME, TransformLeft::of)
                            .register(TransformNormalise.FUNCTION_NAME, TransformNormalise::of)
                            .register(TransformPrefix.FUNCTION_NAME, TransformPrefix::of)
                            .register(/* backward compat */"PrependPrefix", TransformPrefix::of)
                            .register(TransformNegate.FUNCTION_NAME, TransformNegate::of)
                            .register(TransformRemove.FUNCTION_NAME, TransformRemove::of)
                            .register(TransformRetain.FUNCTION_NAME, TransformRetain::of)
                            .register(TransformRound.FUNCTION_NAME, TransformRound::of)
                            .register(TransformSplit.FUNCTION_NAME, TransformSplit::of)
                            .register(TransformStrip.FUNCTION_NAME, TransformStrip::of)
                            .register(TransformSubstring.FUNCTION_NAME, TransformSubstring::of)
                            .register(TransformSuffix.FUNCTION_NAME, TransformSuffix::of)
                            .register(/* backward compat */"AppendSuffix", TransformSuffix::of)
                            .register(TransformToLowerCase.FUNCTION_NAME, TransformToLowerCase::of)
                            .register(TransformToLocalDate.FUNCTION_NAME, TransformToLocalDate::of)
                            .register(TransformAnyToString.FUNCTION_NAME, TransformAnyToString::of)
                            .register(TransformToUpperCase.FUNCTION_NAME, TransformToUpperCase::of)
                            .register(TransformRight.FUNCTION_NAME, TransformRight::of).build();
                    REGISTRY = x;
                }
            }
        }
        return x;
    }
}

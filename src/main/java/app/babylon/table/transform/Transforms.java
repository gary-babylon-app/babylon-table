package app.babylon.table.transform;

public final class Transforms
{
    private static volatile TransformRegistry REGISTRY;

    private Transforms()
    {
    }

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
                    x = TransformRegistry.builder()
                            .register(TransformAfter.FUNCTION_NAME,          TransformAfter::of)
                            .register(TransformBefore.FUNCTION_NAME,         TransformBefore::of)
                            .register(TransformClassify.FUNCTION_NAME,       TransformClassify::of)
                            .register(TransformCleanWhitespace.FUNCTION_NAME, TransformCleanWhitespace::of)
                            .register(TransformCoalesce.FUNCTION_NAME,       TransformCoalesce::of)
                            .register(TransformConcat.FUNCTION_NAME,         TransformConcat::of)
                            .register(TransformCopy.FUNCTION_NAME,           TransformCopy::of)
                            .register(TransformCreateConstant.FUNCTION_NAME, TransformCreateConstant::of)
                            .register(/*bacward compat*/"NewConstant",       TransformCreateConstant::of)
                            .register(TransformExtract.FUNCTION_NAME,        TransformExtract::of)
                            .register(TransformLeft.FUNCTION_NAME,           TransformLeft::of)
                            .register(TransformPrefix.FUNCTION_NAME,         TransformPrefix::of)
                            .register(/*backward compat*/"PrependPrefix",    TransformPrefix::of)
                            .register(TransformSplit.FUNCTION_NAME,          TransformSplit::of)
                            .register(TransformStrip.FUNCTION_NAME,          TransformStrip::of)
                            .register(TransformSubstring.FUNCTION_NAME,      TransformSubstring::of)
                            .register(TransformSuffix.FUNCTION_NAME,         TransformSuffix::of)
                            .register(/*backward compat*/"AppendSuffix",     TransformSuffix::of)
                            .register(TransformToDecimal.FUNCTION_NAME,      TransformToDecimal::of)
                            .register(TransformToDouble.FUNCTION_NAME,       TransformToDouble::of)
                            .register(TransformToInt.FUNCTION_NAME,          TransformToInt::of)
                            .register(TransformToLowerCase.FUNCTION_NAME,    TransformToLowerCase::of)
                            .register(TransformToLocalDate.FUNCTION_NAME,    TransformToLocalDate::of)
                            .register(TransformToLong.FUNCTION_NAME,         TransformToLong::of)
                            .register(TransformToString.FUNCTION_NAME,       TransformToString::of)
                            .register(TransformToUpperCase.FUNCTION_NAME,    TransformToUpperCase::of)
                            .register(TransformRight.FUNCTION_NAME,          TransformRight::of)
                            .build();
                    REGISTRY = x;
                }
            }
        }
        return x;
    }
}

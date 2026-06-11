package app.babylon.table.transform;

import app.babylon.io.DataResource;
import app.babylon.io.DataResources;
import app.babylon.lang.ArgumentCheck;

public final class QuickTransformScript
{
    private final String text;

    private QuickTransformScript(String text)
    {
        this.text = ArgumentCheck.nonNull(text);
    }

    public static QuickTransformScript of(CharSequence script)
    {
        return new QuickTransformScript(ArgumentCheck.nonNull(script).toString());
    }

    public static QuickTransformScript read(DataResource source)
    {
        return of(DataResources.getAsString(ArgumentCheck.nonNull(source)));
    }

    public String text()
    {
        return this.text;
    }

    public String prettyPrint(QuickTransforms quickTransforms)
    {
        QuickTransforms checkedQuickTransforms = ArgumentCheck.nonNull(quickTransforms);
        return String.join("\n", checkedQuickTransforms.writeAll(checkedQuickTransforms.parse(this).transforms()));
    }

    public StringBuilder appendTo(StringBuilder builder)
    {
        return ArgumentCheck.nonNull(builder).append(this.text);
    }
}

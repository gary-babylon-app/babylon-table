package app.babylon.table.transform;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import app.babylon.lang.ArgumentCheck;
import app.babylon.table.column.Column;
import app.babylon.table.dsl.TokenStream;
import app.babylon.table.transform.dsl.TransformCommandParser;
import app.babylon.table.transform.dsl.TransformDslParser;
import app.babylon.table.transform.dsl.TransformDslWriter;

public final class QuickTransforms
{
    private final TransformDslParser parser;
    private final TransformDslWriter writer;

    private QuickTransforms(TransformDslParser parser, TransformDslWriter writer)
    {
        this.parser = ArgumentCheck.nonNull(parser);
        this.writer = ArgumentCheck.nonNull(writer);
    }

    public static QuickTransforms standard()
    {
        return StandardHolder.INSTANCE;
    }

    private static final class StandardHolder
    {
        private static final QuickTransforms INSTANCE = new QuickTransforms(TransformDslParser.standard(),
                TransformDslWriter.standard());
    }

    public QuickTransforms with(String command, TransformCommandParser parser)
    {
        return new QuickTransforms(this.parser.with(command, parser), this.writer);
    }

    public QuickTransforms with(Map<String, TransformCommandParser> parsers)
    {
        return new QuickTransforms(this.parser.with(parsers), this.writer);
    }

    public QuickTransforms withType(String name, Column.Type type)
    {
        return new QuickTransforms(this.parser.withType(name, type), this.writer);
    }

    public QuickTransforms withTypes(Map<String, Column.Type> types)
    {
        return new QuickTransforms(this.parser.withTypes(types), this.writer);
    }

    public <T> QuickTransforms withRoundScale(Class<T> type, ToIntFunction<T> roundScale)
    {
        return new QuickTransforms(this.parser.withRoundScale(type, roundScale), this.writer);
    }

    public QuickTransforms withWriter(Class<? extends Transform> transformClass, Function<Transform, String> writer)
    {
        return new QuickTransforms(this.parser, this.writer.with(transformClass, writer));
    }

    public TransformSet parse(QuickTransformScript script)
    {
        return TransformSet.of(this.parser.parseScript(ArgumentCheck.nonNull(script).text()));
    }

    public Transform parse(TokenStream tokens)
    {
        return this.parser.parse(tokens);
    }

    public String write(Transform transform)
    {
        return this.writer.write(transform);
    }

    public List<String> writeAll(Iterable<? extends Transform> transforms)
    {
        return this.writer.writeAll(transforms);
    }

    public List<String> writeAll(Transform... transforms)
    {
        return transforms == null
                ? writeAll((Iterable<? extends Transform>) null)
                : writeAll(Arrays.asList(transforms));
    }

}

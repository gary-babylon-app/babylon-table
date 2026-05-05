package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.babylon.table.transform.Transform;
import app.babylon.table.transform.TransformClean;
import app.babylon.table.transform.TransformCopy;
import app.babylon.table.transform.TransformRound;
import app.babylon.table.transform.TransformStringToType;

class QuickTransformsTest
{
    @Test
    void shouldParseAndWriteStatement()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        Transform transform = quickTransforms.parse("clean Name");

        assertInstanceOf(TransformClean.class, transform);
        assertEquals("clean Name", quickTransforms.write(transform));
    }

    @Test
    void shouldParseAndWriteAllStatements()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        Iterable<String> statements = List.of("clean Name", "copy Symbol into DisplaySymbol");
        List<Transform> transforms = quickTransforms.parseAll(statements);

        assertInstanceOf(TransformClean.class, transforms.get(0));
        assertInstanceOf(TransformCopy.class, transforms.get(1));
        assertEquals(List.of("clean Name", "copy Symbol into DisplaySymbol"), quickTransforms.writeAll(transforms));
    }

    @Test
    void shouldParseAllVarargsStatements()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        List<Transform> transforms = quickTransforms.parseAll("convert Currency to Currency by exact",
                "round Amount using Currency into Rounded");

        assertInstanceOf(TransformStringToType.class, transforms.get(0));
        assertInstanceOf(TransformRound.class, transforms.get(1));
        assertEquals(List.of("convert Currency to Currency", "round Amount using Currency into Rounded"),
                quickTransforms.writeAll(transforms));
    }

    @Test
    void shouldWriteAllVarargsTransforms()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        List<String> lines = quickTransforms.writeAll(quickTransforms.parse("clean Name"),
                quickTransforms.parse("copy Symbol into DisplaySymbol"));

        assertEquals(List.of("clean Name", "copy Symbol into DisplaySymbol"), lines);
    }

    @Test
    void shouldFormatWithSameParserAndWriterConfiguration()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        assertEquals("constant metadata.description into SourceDescription",
                quickTransforms.format("constant METADATA.DESCRIPTION into SourceDescription"));
    }
}

package app.babylon.table.transform.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.babylon.table.transform.Transform;
import app.babylon.table.transform.TransformCleanWhitespace;
import app.babylon.table.transform.TransformCopy;

class QuickTransformsTest
{
    @Test
    void shouldParseAndWriteStatement()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        Transform transform = quickTransforms.parse("clean Name");

        assertInstanceOf(TransformCleanWhitespace.class, transform);
        assertEquals("clean Name", quickTransforms.write(transform));
    }

    @Test
    void shouldParseAndWriteAllStatements()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        List<Transform> transforms = quickTransforms.parseAll(List.of("clean Name", "copy Symbol into DisplaySymbol"));

        assertInstanceOf(TransformCleanWhitespace.class, transforms.get(0));
        assertInstanceOf(TransformCopy.class, transforms.get(1));
        assertEquals(List.of("clean Name", "copy Symbol into DisplaySymbol"), quickTransforms.writeAll(transforms));
    }

    @Test
    void shouldFormatWithSameParserAndWriterConfiguration()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        assertEquals("constant metadata.description into SourceDescription",
                quickTransforms.format("constant METADATA.DESCRIPTION into SourceDescription"));
    }
}

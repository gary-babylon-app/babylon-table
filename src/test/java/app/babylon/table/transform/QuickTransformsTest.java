package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

class QuickTransformsTest
{
    @Test
    void standardShouldReturnLazySingleton()
    {
        assertSame(QuickTransforms.standard(), QuickTransforms.standard());
    }

    @Test
    void shouldParseAndWriteStatement()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        Transform transform = quickTransforms.parse(QuickTransformScript.of("clean Name")).transforms()[0];

        assertInstanceOf(TransformClean.class, transform);
        assertEquals("clean Name", quickTransforms.write(transform));
    }

    @Test
    void shouldParseTransformSet()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        Transform[] transforms = quickTransforms.parse(QuickTransformScript.of("""
                # cleanup
                clean Name into CleanName # trailing comment
                constant 'A
                B' into Multiline

                uppercase CleanName
                """)).transforms();

        assertInstanceOf(TransformClean.class, transforms[0]);
        assertInstanceOf(TransformConstant.class, transforms[1]);
        assertInstanceOf(TransformToUpperCase.class, transforms[2]);
    }

    @Test
    void shouldParseTransformSetIntoCollection()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();
        List<Transform> transforms = new java.util.ArrayList<>();

        assertSame(transforms, quickTransforms.parse(QuickTransformScript.of("clean Name")).transforms(transforms));
        assertInstanceOf(TransformClean.class, transforms.get(0));
    }

    @Test
    void shouldApplyTransformSet()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        assertEquals(1, quickTransforms.parse(QuickTransformScript.of("clean Name")).transforms().length);
    }

    @Test
    void shouldWriteAllVarargsTransforms()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        List<String> lines = quickTransforms.writeAll(
                quickTransforms.parse(QuickTransformScript.of("clean Name")).transforms()[0],
                quickTransforms.parse(QuickTransformScript.of("copy Symbol into DisplaySymbol")).transforms()[0]);

        assertEquals(List.of("clean Name", "copy Symbol into DisplaySymbol"), lines);
    }

    @Test
    void shouldFormatWithSameParserAndWriterConfiguration()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        Transform transform = quickTransforms
                .parse(QuickTransformScript.of("constant METADATA.DESCRIPTION into SourceDescription")).transforms()[0];

        assertEquals("constant metadata.description into SourceDescription", quickTransforms.write(transform));
    }
}

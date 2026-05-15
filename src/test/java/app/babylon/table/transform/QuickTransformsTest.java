package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;

import org.junit.jupiter.api.Test;

import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.Column;
import app.babylon.table.column.ColumnCategorical;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

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

    @Test
    void shouldApplyConstantWithRegisteredCustomType()
    {
        Column.Type codeType = Column.Type.of(Code.class, Code::parse);
        QuickTransforms quickTransforms = QuickTransforms.standard().withType("code", codeType);
        ColumnName name = ColumnName.of("Name");
        ColumnName code = ColumnName.of("Code");
        ColumnObject.Builder<String> names = ColumnObject.builder(name, ColumnTypes.STRING);
        names.add("A");
        names.add("B");
        TableColumnar table = Tables.newTable(TableName.of("t"), names.build());

        TableColumnar transformed = quickTransforms.parse(QuickTransformScript.of("constant 'ABC' as code into Code"))
                .apply(table);

        ColumnCategorical<Code> codes = transformed.getCategorical(code, codeType);
        assertEquals(Code.class, codes.getType().getValueClass());
        assertEquals(Code.parse("ABC"), codes.get(0));
        assertEquals(Code.parse("ABC"), codes.get(1));
    }

    private record Code(String value)
    {
        private static Code parse(CharSequence chars)
        {
            return new Code(chars.toString());
        }

        private static Code parse(CharSequence chars, int offset, int length)
        {
            return parse(chars.subSequence(offset, offset + length));
        }
    }
}

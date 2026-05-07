package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import app.babylon.io.StreamSources;
import app.babylon.table.TableColumnar;
import app.babylon.table.TableName;
import app.babylon.table.Tables;
import app.babylon.table.column.ColumnName;
import app.babylon.table.column.ColumnObject;
import app.babylon.table.column.ColumnTypes;

class QuickTransformScriptTest
{
    private static final ColumnName NAME = ColumnName.of("Name");
    private static final ColumnName CLEAN_NAME = ColumnName.of("CleanName");

    @Test
    void shouldCreateScript()
    {
        QuickTransformScript script = QuickTransformScript.of("clean Name into CleanName");

        assertEquals("clean Name into CleanName", script.text());
        assertEquals("quick clean Name into CleanName", script.appendTo(new StringBuilder("quick ")).toString());
    }

    @Test
    void shouldReadScriptFromStreamSource()
    {
        QuickTransformScript script = QuickTransformScript
                .read(StreamSources.fromString("clean Name into CleanName\nuppercase CleanName\n", "cleanup.qt"));

        assertEquals("clean Name into CleanName\nuppercase CleanName\n", script.text());
    }

    @Test
    void shouldParseScript()
    {
        QuickTransforms quickTransforms = QuickTransforms.standard();

        TransformSet statements = quickTransforms.parse(QuickTransformScript.of("""
                # cleanup
                clean Name into CleanName # trailing comment
                constant 'A
                B' into Multiline

                uppercase CleanName
                """));

        assertInstanceOf(TransformClean.class, statements.transforms()[0]);
        assertInstanceOf(TransformConstant.class, statements.transforms()[1]);
        assertInstanceOf(TransformToUpperCase.class, statements.transforms()[2]);
    }

    @Test
    void shouldPrettyPrintScript()
    {
        QuickTransformScript script = QuickTransformScript.of("""
                # cleanup
                CLEAN Name INTO CleanName
                constant METADATA.DESCRIPTION into SourceDescription
                """);

        assertEquals("clean Name into CleanName\nconstant metadata.description into SourceDescription",
                script.prettyPrint(QuickTransforms.standard()));
    }

    @Test
    void shouldReturnDefensiveTransformArrays()
    {
        TransformSet statements = TransformSet.of(new TransformClean(NAME, CLEAN_NAME));

        assertNotSame(statements.transforms(), statements.transforms());
        assertEquals(1, statements.transforms().length);
    }

    @Test
    void shouldAddTransformsToCollection()
    {
        TransformSet statements = TransformSet.of(new TransformClean(NAME, CLEAN_NAME));
        List<Transform> transforms = new ArrayList<>();

        assertSame(transforms, statements.transforms(transforms));
        assertEquals(1, transforms.size());
        assertInstanceOf(TransformClean.class, transforms.get(0));
    }

    @Test
    void shouldApplyParsedTransformSet()
    {
        ColumnObject.Builder<String> names = ColumnObject.builder(NAME, ColumnTypes.STRING);
        names.add("  Alice   Smith  ");
        TableColumnar table = Tables.newTable(TableName.of("people"), names.build());

        TableColumnar transformed = QuickTransforms.standard()
                .parse(QuickTransformScript.of("clean Name into CleanName")).apply(table);

        assertEquals("Alice Smith", transformed.getString(CLEAN_NAME).get(0));
    }
}

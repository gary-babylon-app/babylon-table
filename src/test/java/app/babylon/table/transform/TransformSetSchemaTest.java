package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TransformSetSchemaTest
{
    @Test
    void shouldExposeExpectedConstants()
    {
        assertEquals("Babylon", TransformSetSchema.BABYLON);
        assertEquals("transform_sets", TransformSetSchema.TABLE_NAME);
        assertEquals("Transform", TransformSetSchema.TRANSFORM.toString());
        assertEquals("Param5", TransformSetSchema.PARAM5.toString());
    }
}

package app.babylon.table.transform;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TransformsTest
{
    @Test
    void registryShouldBeSingletonAndContainKnownTransforms()
    {
        TransformRegistry first = Transforms.registry();
        TransformRegistry second = Transforms.registry();

        assertSame(first, second);
        assertTrue(first.create("After", "Code", "After", "-") instanceof TransformAfter);
        assertTrue(first.create("NewConstant", "Value", "X") instanceof TransformCreateConstant);
        assertTrue(first.create("AppendSuffix", "Code", "Out", "_X") instanceof TransformSuffix);
    }
}

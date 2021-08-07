package iamdev.fx.common;

import org.junit.Assert;
import org.junit.Test;

public class IntegerSerializerTest {

    @Test
    public void test() {
        for (int i = -1000; i < 1000; i++) {
            byte[] bytes = IntegerSerializer.serialize(i);
            int value = IntegerSerializer.deserialize(bytes);
            Assert.assertEquals(i, value);
        }
    }
}

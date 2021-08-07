package iamdev.fx.common;

import org.junit.Assert;
import org.junit.Test;

import static iamdev.fx.common.Constants.PRIME_FLAG_TRUE;

public class PrimeResultSerializerTest {

    @Test
    public void test() {
        PrimeResult primeResult = new PrimeResult(100, PRIME_FLAG_TRUE);
        byte[] bytes = PrimeResultSerializer.serialize(primeResult);
        PrimeResult primeResultDes = PrimeResultSerializer.deserialize(bytes);
        Assert.assertEquals(primeResult, primeResultDes);
    }
}

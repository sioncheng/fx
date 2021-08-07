package iamdev.fx.common;

public class PrimeResultSerializer {

    public static byte[] serialize(PrimeResult primeResult) {
        byte[] bytes1 = IntegerSerializer.serialize(primeResult.getInteger());
        byte[] bytes2 = IntegerSerializer.serialize(primeResult.getPrimeFlag());
        byte[] result = new byte[8];
        System.arraycopy(bytes1, 0, result, 0, 4);
        System.arraycopy(bytes2, 0, result, 4, 4);
        return result;
    }

    public static PrimeResult deserialize(byte[] bytes) {
        int val = IntegerSerializer.deserialize(bytes, 0);
        int flag = IntegerSerializer.deserialize(bytes, 4);
        return new PrimeResult(val, flag);
    }
}

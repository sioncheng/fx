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
        return deserialize(bytes, 0);
    }

    public static PrimeResult deserialize(byte[] bytes, int offset) {
        int val = IntegerSerializer.deserialize(bytes, offset);
        int flag = IntegerSerializer.deserialize(bytes, offset + 4);
        return new PrimeResult(val, flag);
    }
}

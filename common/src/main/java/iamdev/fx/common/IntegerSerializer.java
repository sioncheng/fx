package iamdev.fx.common;

import java.nio.ByteBuffer;

import static iamdev.fx.common.Constants.INTEGER_BYTES_SIZE;

public class IntegerSerializer {

    public static byte[] serialize(Integer integer) {
        return ByteBuffer.allocate(INTEGER_BYTES_SIZE).putInt(integer).array();
    }

    public static int deserialize(byte[] bytes) {
        return deserialize(bytes, 0);
    }

    public static int deserialize(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, INTEGER_BYTES_SIZE).getInt();
    }
}

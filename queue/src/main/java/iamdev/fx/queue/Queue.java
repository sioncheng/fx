package iamdev.fx.queue;

import com.sun.tools.javac.util.Assert;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import static iamdev.fx.common.Constants.INTEGER_BYTES_SIZE;

public class Queue implements Closeable {

    private static final int MIN_CAPACITY = INTEGER_BYTES_SIZE * 128;

    private File dataFile;

    private int capacity;

    private FileChannel dataFileChannel;

    private MappedByteBuffer dataMappedByteBuffer;

    private int head;

    private int tail;

    private int remains;

    private int space;

    private Queue(File dataFile, int capacity) throws IOException {
        this.dataFile = dataFile;
        this.capacity = capacity;
        dataFileChannel = new RandomAccessFile(dataFile, "rw").getChannel();
        int size = (int) dataFileChannel.size();
        if (size < this.capacity) {
            byte bytes[] = new byte[this.capacity - size];
            ByteBuffer bf = ByteBuffer.wrap(bytes);
            dataFileChannel.position(size);
            dataFileChannel.write(bf);
            dataFileChannel.force(false);
        }
        this.dataMappedByteBuffer = dataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, this.capacity);
        this.head = 0;
        this.tail = 0;
        this.remains = 0;
        this.space = this.capacity - this.tail;
    }

    public int getRemains() {
        return remains;
    }

    public int getSpace() {
        return space;
    }

    public int getHead() {
        return head;
    }

    public int getTail() {
        return tail;
    }

    @Override
    public void close() throws IOException {
        if (null != this.dataFileChannel) {
            this.dataFileChannel.close();
        }
    }

    public static Queue create(String dataFilePath, int capacity) throws IOException {
        Assert.check(capacity >= MIN_CAPACITY , String.format("capacity is too small, should at least be %d", MIN_CAPACITY));

        File dataFile = new File(dataFilePath);
        if (!dataFile.exists()) {
            dataFile.createNewFile();
        }

        return new Queue(dataFile, capacity);
    }

    public int put(byte[] bytes) {
        int written = 0;
        FileLock fileLock = null;
        try {
            fileLock = dataFileChannel.tryLock(0, 1, false);
            if (null != fileLock && this.space >= bytes.length ) {

                if (this.tail + bytes.length < this.capacity) {
                    dataMappedByteBuffer.position(this.tail);
                    dataMappedByteBuffer.put(bytes);
                    this.tail += bytes.length;
                } else {
                    int l = this.capacity - this.tail;
                    dataMappedByteBuffer.position(this.tail);
                    dataMappedByteBuffer.put(bytes, 0, l);
                    dataMappedByteBuffer.position(0);
                    dataMappedByteBuffer.put(bytes, l, bytes.length - l);
                    this.tail = 0 + (bytes.length - l);
                }

                this.space -= bytes.length;
                this.remains += bytes.length;
                written = bytes.length;
            }
        } catch (IOException ex) {
        } finally {
            if (null != fileLock) {
                try {
                    fileLock.release();
                } catch (IOException ioe) {
                }
            }
        }
        return written;

    }

    public byte[] get(int min) {

        byte[] result = null;
        FileLock fileLock = null;
        try {
            fileLock = dataFileChannel.tryLock(0, 1, false);
            if (null != fileLock && this.remains >= min ) {
                result = new byte[this.remains];
                if (this.head < this.tail) {
                    dataMappedByteBuffer.position(this.head);
                    dataMappedByteBuffer.get(result);
                    this.head += this.remains;
                } else {
                    int l = this.capacity - this.head;
                    dataMappedByteBuffer.position(this.head);
                    dataMappedByteBuffer.get(result, 0, l);
                    dataMappedByteBuffer.position(0);
                    dataMappedByteBuffer.get(result, l, this.remains - l);
                    this.head = this.remains - l;
                }

                this.space += remains;
                this.remains = 0;
            }
        } catch (IOException ex) {
        } finally {
            if (null != fileLock) {
                try {
                    fileLock.release();
                } catch (IOException ioe) {}
            }
        }

        return result;
    }
}

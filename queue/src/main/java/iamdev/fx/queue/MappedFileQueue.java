package iamdev.fx.queue;

import com.sun.tools.javac.util.Assert;
import iamdev.fx.common.IntegerSerializer;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static iamdev.fx.common.Constants.INTEGER_BYTES_SIZE;

public class MappedFileQueue implements Queue {

    private static final byte[] MAGIC_BYTES = new byte[]{0x01, 0x02, 0x03, 0x04};

    private static final int MIN_CAPACITY = INTEGER_BYTES_SIZE * 128;

    private static final int META_FILE_SIZE = 20;

    private static final int META_MAGIC_OFFSET = 0;

    private static final int META_HEAD_OFFSET = 4;

    private static final int META_TAIL_OFFSET = 8;

    private static final int META_REMAINS_OFFSET = 12;

    private static final int META_SPACE_OFFSET = 16;

    private File metaFile;

    private File dataFile;

    private int capacity;

    private FileChannel metaFileChannel;

    private MappedByteBuffer metaMappedByteBuffer;

    private FileChannel dataFileChannel;

    private MappedByteBuffer dataMappedByteBuffer;

    private int head;

    private int tail;

    private int remains;

    private int space;

    public static MappedFileQueue create(String directory, int capacity) throws IOException {
        Assert.check(capacity >= MappedFileQueue.MIN_CAPACITY, String.format("capacity is too small, should at least be %d", MappedFileQueue.MIN_CAPACITY));

        File path = new File(directory);
        if (!path.exists()) {
            Path paths = Paths.get(directory);
            Files.createDirectories(paths);
        }

        File metaFile = new File(path.getAbsolutePath() + "/meta");
        if (!metaFile.exists()) {
            metaFile.createNewFile();
        }

        File dataFile = new File(path.getAbsolutePath() + "/data");
        if (!dataFile.exists()) {
            dataFile.createNewFile();
        }

        return new MappedFileQueue(metaFile, dataFile, capacity);
    }

    private MappedFileQueue(File metaFile, File dataFile, int capacity) throws IOException {
        this.metaFile = metaFile;
        this.dataFile = dataFile;
        this.capacity = capacity;

        this.head = 0;
        this.tail = 0;
        this.remains = 0;
        this.space = this.capacity - this.tail;

        this.metaFileChannel = new RandomAccessFile(metaFile, "rw").getChannel();
        int metaSize = (int) this.metaFileChannel.size();
        if (metaSize < META_FILE_SIZE) {
            byte[] bytes = new byte[META_FILE_SIZE - metaSize];
            ByteBuffer bf = ByteBuffer.wrap(bytes);
            this.metaFileChannel.position(metaSize);
            this.metaFileChannel.write(bf);
            this.metaFileChannel.force(true);
            this.metaMappedByteBuffer = this.metaFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, META_FILE_SIZE);
            this.saveMetaInfo();
        } else {
            this.metaMappedByteBuffer = this.metaFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, META_FILE_SIZE);
            this.loadMetaInfo();
        }


        this.dataFileChannel = new RandomAccessFile(dataFile, "rw").getChannel();
        int size = (int) this.dataFileChannel.size();
        if (size < this.capacity) {
            byte bytes[] = new byte[this.capacity - size];
            ByteBuffer bf = ByteBuffer.wrap(bytes);
            this.dataFileChannel.position(size);
            this.dataFileChannel.write(bf);
            this.dataFileChannel.force(true);
        }
        this.dataMappedByteBuffer = this.dataFileChannel.map(FileChannel.MapMode.READ_WRITE, 0, this.capacity);
    }

    @Override
    public int getRemains() {
        return remains;
    }

    @Override
    public int getSpace() {
        return space;
    }

    @Override
    public int getHead() {
        return head;
    }

    @Override
    public int getTail() {
        return tail;
    }

    @Override
    public void close() {
        if (null != this.metaFileChannel) {
            try {
                this.metaFileChannel.close();
            } catch (Exception ex) {}
        }
        if (null != this.dataFileChannel) {
            try {
                this.dataFileChannel.close();
            } catch (Exception ex) {}
        }
    }

    @Override
    public void clear() {
        if (null != this.metaFile) {
            this.metaFile.delete();
        }
        if (null != this.dataFile) {
            this.dataFile.delete();
        }
    }

    @Override
    public int put(byte[] bytes) {
        int written = 0;
        FileLock fileLock = null;
        try {
            fileLock = metaFileChannel.tryLock(0, META_FILE_SIZE, false);
            if (null != fileLock) {

                this.loadMetaInfo();

                if (this.space >= bytes.length) {

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

                    this.saveMetaInfo();

                }
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

    @Override
    public byte[] get(int min) {

        byte[] result = null;
        FileLock fileLock = null;
        try {
            fileLock = metaFileChannel.tryLock(0, META_FILE_SIZE, false);
            if (null != fileLock) {
                this.loadMetaInfo();

                if (this.remains >= min) {
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

                    this.saveMetaInfo();
                }
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

    private void saveMetaInfo() {
        this.metaMappedByteBuffer.position(META_MAGIC_OFFSET);
        this.metaMappedByteBuffer.put(MAGIC_BYTES);
        this.metaMappedByteBuffer.position(META_HEAD_OFFSET);
        this.metaMappedByteBuffer.put(IntegerSerializer.serialize(this.head));
        this.metaMappedByteBuffer.position(META_TAIL_OFFSET);
        this.metaMappedByteBuffer.put(IntegerSerializer.serialize(this.tail));
        this.metaMappedByteBuffer.position(META_REMAINS_OFFSET);
        this.metaMappedByteBuffer.put(IntegerSerializer.serialize(this.remains));
        this.metaMappedByteBuffer.position(META_SPACE_OFFSET);
        this.metaMappedByteBuffer.put(IntegerSerializer.serialize(this.space));
    }

    private void loadMetaInfo() {
        byte[] bytes = new byte[INTEGER_BYTES_SIZE];
        this.metaMappedByteBuffer.position(META_HEAD_OFFSET);
        this.metaMappedByteBuffer.get(bytes);
        this.head = IntegerSerializer.deserialize(bytes);
        this.metaMappedByteBuffer.position(META_TAIL_OFFSET);
        this.metaMappedByteBuffer.get(bytes);
        this.tail = IntegerSerializer.deserialize(bytes);
        this.metaMappedByteBuffer.position(META_REMAINS_OFFSET);
        this.metaMappedByteBuffer.get(bytes);
        this.remains = IntegerSerializer.deserialize(bytes);
        this.metaMappedByteBuffer.position(META_SPACE_OFFSET);
        this.metaMappedByteBuffer.get(bytes);
        this.space = IntegerSerializer.deserialize(bytes);
    }
}

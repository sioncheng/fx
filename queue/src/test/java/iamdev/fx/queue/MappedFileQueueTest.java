package iamdev.fx.queue;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author cyq
 * @create 2021-08-07 5:50 PM
 */
public class MappedFileQueueTest {

    @Test
    public void testPutGet() throws IOException {

        Path path = Paths.get("/tmp/q1");
        Files.deleteIfExists(path);
        Files.createDirectories(path);

        MappedFileQueue queue = MappedFileQueue.create("/tmp/q1", 1024);

        Assert.assertNotNull(queue);

        byte[] bytes = new byte[1023];
        int w = queue.put(bytes);

        Assert.assertEquals(1023, w);
        Assert.assertEquals(1, queue.getSpace());
        Assert.assertEquals(0, queue.getHead());
        Assert.assertEquals(1023, queue.getTail());

        w = queue.put(bytes);
        Assert.assertEquals(0, w);
        Assert.assertEquals(1, queue.getSpace());
        Assert.assertEquals(0, queue.getHead());
        Assert.assertEquals(1023, queue.getTail());

        byte[] result = queue.get(0);
        Assert.assertNotNull(result);
        Assert.assertEquals(1023, result.length);
        Assert.assertEquals(1023, queue.getHead());
        Assert.assertEquals(0, queue.getRemains());

        queue.close();
        queue.clear();
    }


    @Test
    public void testPutGetMulti() throws IOException {

        Path path = Paths.get("/tmp/q2");
        Files.deleteIfExists(path);
        Files.createDirectories(path);

        int capacity = 512;
        int block = 8;
        int n = capacity / 8;

        MappedFileQueue queue = MappedFileQueue.create("/tmp/q2/", capacity);

        Assert.assertNotNull(queue);

        byte[] bytes = new byte[block];
        //put
        for (int i = 0; i < n - 2; i++) {
            int w = queue.put(bytes);
            Assert.assertEquals(block, w);
        }
        //get
        byte[] result = queue.get(block);
        Assert.assertEquals(block * (n - 2), result.length);

        //put
        for (int i = 0; i < n - 2; i++) {
            int w = queue.put(bytes);
            Assert.assertEquals(block, w);
        }
        //get
        result = queue.get(block);
        Assert.assertEquals(block * (n - 2), result.length);

        Assert.assertEquals(capacity, queue.getSpace());
        Assert.assertEquals(0, queue.getRemains());


        queue.close();
        queue.clear();
    }

}

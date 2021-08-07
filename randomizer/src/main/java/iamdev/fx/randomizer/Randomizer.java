package iamdev.fx.randomizer;

import iamdev.fx.common.IntegerSerializer;
import iamdev.fx.common.PrimeResult;
import iamdev.fx.common.PrimeResultSerializer;
import iamdev.fx.queue.Queue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

import static iamdev.fx.common.Constants.INTEGER_BYTES_SIZE;
import static iamdev.fx.common.Constants.PRIME_RESULT_BYTES_SIZE;

public class Randomizer {


    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("please enter integer queue file and prime result queue file");
            Runtime.getRuntime().exit(1);
        }

        int capacity = 1024;
        int cacheLine = 64;
        int batch = cacheLine / INTEGER_BYTES_SIZE;

        Queue integerQueue = Queue.create(args[0], capacity);
        Queue primeResultQueue = Queue.create(args[1], capacity);

        Thread t2 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    byte[] result = primeResultQueue.get(PRIME_RESULT_BYTES_SIZE);
                    if (null == result) {
                        try {
                            Thread.sleep(10);
                        } catch (Exception ex) {}
                        continue;
                    }
                    int prs = result.length / PRIME_RESULT_BYTES_SIZE;
                    int offset = 0;
                    for (int i = 0; i< prs; i++) {
                        PrimeResult primeResult = PrimeResultSerializer.deserialize(result, offset);
                        offset += PRIME_RESULT_BYTES_SIZE;
                        System.out.println(primeResult);
                    }
                }
            }
        });
        t2.setDaemon(true);
        t2.start();

        Thread t1 = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    byte[] result = new byte[cacheLine];
                    int offset = 0;
                    for (int i = 0; i < batch; i++) {
                        int r = ThreadLocalRandom.current().nextInt(Integer.MAX_VALUE);
                        byte[] bytes = IntegerSerializer.serialize(r);
                        System.arraycopy(bytes, 0, result, offset, INTEGER_BYTES_SIZE);
                        offset += INTEGER_BYTES_SIZE;
                    }
                    int w = 0;
                    while (w <= 0) {
                        w = integerQueue.put(result);
                        System.out.println(String.format("put %d", w));
                        try {
                            Thread.sleep(10);
                        } catch (Exception ex) {}
                    }
                }
            }
        });
        t1.setDaemon(true);
        t1.start();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    integerQueue.close();
                } catch (Exception e) {}
                try {
                    primeResultQueue.close();
                } catch (Exception e) {}
            }
        }));

        CountDownLatch countDownLatch = new CountDownLatch(1);
        countDownLatch.await();
    }
}

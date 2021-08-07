package iamdev.fx.prime;

import iamdev.fx.common.IntegerSerializer;
import iamdev.fx.common.PrimeResult;
import iamdev.fx.common.PrimeResultSerializer;
import iamdev.fx.queue.Queue;

import java.io.IOException;
import java.util.concurrent.*;

import static iamdev.fx.common.Constants.INTEGER_BYTES_SIZE;
import static iamdev.fx.common.Constants.PRIME_RESULT_BYTES_SIZE;

public class Prime {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("please enter integer queue file and prime result queue file");
            Runtime.getRuntime().exit(1);
        }

        int capacity = 1024;

        Queue integerQueue = Queue.create(args[0], capacity);
        Queue primeResultQueue = Queue.create(args[1], capacity);

        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    byte[] bytes = integerQueue.get(INTEGER_BYTES_SIZE);
                    if (null == bytes) {
                        try {
                            Thread.sleep(10);
                        } catch (Exception ex) {}
                        continue;
                    }

                    int numbers = bytes.length / INTEGER_BYTES_SIZE;
                    System.out.println(String.format("numbers %d", numbers));
                    CountDownLatch countDownLatch = new CountDownLatch(numbers);
                    ConcurrentSkipListSet<PrimeResult> primeResults = new ConcurrentSkipListSet<>();
                    int offset = 0;
                    for (int i = 0; i < numbers; i++) {
                        int val = IntegerSerializer.deserialize(bytes, offset);
                        offset += INTEGER_BYTES_SIZE;

                        CompletableFuture.supplyAsync(()->{
                            return checkPrime(val);
                        }, executorService).thenAccept(r -> {
                            primeResults.add(r);
                            countDownLatch.countDown();
                        });
                    }

                    try {
                        countDownLatch.await();
                    } catch (Exception ex) {}

                    byte[] results = new byte[primeResults.size() * PRIME_RESULT_BYTES_SIZE];
                    int offsetPr = 0;
                    for (PrimeResult pr :
                            primeResults) {
                        byte[] prBytes = PrimeResultSerializer.serialize(pr);
                        System.arraycopy(prBytes, 0, results, offsetPr, PRIME_RESULT_BYTES_SIZE);
                        offsetPr += PRIME_RESULT_BYTES_SIZE;
                    }

                    int w = 0;
                    while(w <= 0) {
                        w = primeResultQueue.put(results);
                        try {
                            Thread.sleep(10);
                        } catch (Exception ex) {}
                    }
                }
            }
        });
        t.setDaemon(true);
        t.start();


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

    private static PrimeResult checkPrime(int v) {
        return null;
    }
}

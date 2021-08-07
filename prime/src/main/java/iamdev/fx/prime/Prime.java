package iamdev.fx.prime;

import iamdev.fx.common.IntegerSerializer;
import iamdev.fx.common.PrimeResult;
import iamdev.fx.common.PrimeResultSerializer;
import iamdev.fx.common.ThreadUtil;
import iamdev.fx.queue.Queue;

import java.util.concurrent.*;

import static iamdev.fx.common.Constants.*;

public class Prime {

    public static void main(String[] args) throws Exception {

        if (args.length < 2) {
            System.out.println("please enter integer queue directory and prime result queue directory");
            Runtime.getRuntime().exit(1);
        }

        int capacity = 1024;

        Queue integerQueue = Queue.create(args[0], capacity);
        Queue primeResultQueue = Queue.create(args[1], capacity);


        Thread t = new Thread(new Runnable() {

            @Override
            public void run() {

                ExecutorService executorService = Executors.newWorkStealingPool();

                while (true) {
                    byte[] bytes = integerQueue.get(INTEGER_BYTES_SIZE);
                    if (null == bytes) {
                        ThreadUtil.safeYield();
                        continue;
                    }

                    int numbers = bytes.length / INTEGER_BYTES_SIZE;
                    CountDownLatch countDownLatch = new CountDownLatch(numbers);

                    ConcurrentHashMap<PrimeResult, Boolean> primeResults = new ConcurrentHashMap<>(numbers);
                    int offset = 0;
                    for (int i = 0; i < numbers; i++) {
                        int val = IntegerSerializer.deserialize(bytes, offset);
                        offset += INTEGER_BYTES_SIZE;

                        executorService.execute(() -> {
                            boolean isP = isPrime(val);
                            PrimeResult result = new PrimeResult(val, isP ? PRIME_FLAG_TRUE : PRIME_FLAG_FALSE);
                            primeResults.put(result, true);
                            countDownLatch.countDown();
                        });

                    }

                    try {
                        countDownLatch.await();
                    } catch (Exception ex) {
                    }

                    byte[] results = new byte[primeResults.size() * PRIME_RESULT_BYTES_SIZE];
                    int offsetPr = 0;
                    for (PrimeResult pr :
                            primeResults.keySet()) {
                        byte[] prBytes = PrimeResultSerializer.serialize(pr);
                        System.arraycopy(prBytes, 0, results, offsetPr, PRIME_RESULT_BYTES_SIZE);
                        offsetPr += PRIME_RESULT_BYTES_SIZE;
                    }

                    int w = 0;
                    while (w <= 0) {
                        w = primeResultQueue.put(results);
                        ThreadUtil.safeYield();
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
                } catch (Exception e) {
                }
                try {
                    primeResultQueue.close();
                } catch (Exception e) {
                }
            }
        }));

        CountDownLatch runWait = new CountDownLatch(1);
        runWait.await();
    }

    private static boolean isPrime(int src) {
        double sqrt = Math.sqrt(src);

        if (src < 2) {
            return false;
        }

        if (src == 2 || src == 3) {
            return true;
        }

        if (src % 2 == 0) {
            return false;
        }

        for (int i = 3; i <= sqrt; i += 2) {
            if (src % i == 0) {
                return false;
            }
        }

        return true;
    }

}

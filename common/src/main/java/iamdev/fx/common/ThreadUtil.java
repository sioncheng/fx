package iamdev.fx.common;

/**
 * @author cyq
 * @create 2021-08-08 12:42 AM
 */
public class ThreadUtil {

    public static void safeYield() {
        try {
            Thread.sleep(10);
        } catch (Exception ex) {}
    }
}

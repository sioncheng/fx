package iamdev.fx.queue;

import com.sun.tools.javac.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author cyq
 * @create 2021-08-08 1:14 AM
 */
public interface Queue {


    int getRemains();

    int getSpace();

    int getHead();

    int getTail();

    void close();

    void clear();

    int put(byte[] bytes);

    byte[] get(int min);
}

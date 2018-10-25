package pers.cz.chaoxing.util;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * @author 橙子
 * @date 2018/10/25
 */
public final class IOLock {
    private static final ReadWriteLock lock = new ReentrantReadWriteLock();

    public static void input(Runnable runnable) {
        lock.writeLock().lock();
        runnable.run();
        lock.writeLock().unlock();
    }

    public static void output(Runnable runnable) {
        lock.readLock().lock();
        runnable.run();
        lock.readLock().unlock();
    }
}

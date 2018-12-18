package pers.cz.chaoxing.thread;

/**
 * @author 橙子
 * @since 2018/11/27
 */
public class PauseThread extends Thread {
    private volatile boolean pauseFlag;

    public PauseThread() {
    }

    public PauseThread(Runnable target) {
        super(target);
    }

    public PauseThread(ThreadGroup group, Runnable target) {
        super(group, target);
    }

    public PauseThread(String name) {
        super(name);
    }

    public PauseThread(ThreadGroup group, String name) {
        super(group, name);
    }

    public PauseThread(Runnable target, String name) {
        super(target, name);
    }

    public PauseThread(ThreadGroup group, Runnable target, String name) {
        super(group, target, name);
    }

    public PauseThread(ThreadGroup group, Runnable target, String name, long stackSize) {
        super(group, target, name, stackSize);
    }

    public void pause() {
        if (this != Thread.currentThread())
            checkAccess();
        pause0(true);
    }

    public void process() {
        if (this != Thread.currentThread())
            checkAccess();
        pause0(false);
    }

    public static PauseThread currentThread() {
        return (PauseThread) Thread.currentThread();
    }

    public static boolean paused() {
        return currentThread().isPaused(true);
    }

    public boolean isPaused() {
        return isPaused(false);
    }

    private boolean isPaused(boolean ClearPaused) {
        try {
            return pauseFlag;
        } finally {
            if (ClearPaused)
                pauseFlag = false;
        }
    }

    private void pause0(boolean pause) {
        try {
            synchronized (this) {
                this.pauseFlag = pause;
                if (!this.pauseFlag)
                    this.notify();
                if (this == Thread.currentThread() && Thread.currentThread().isAlive())
                    while (this.pauseFlag)
                        this.wait();
            }
        } catch (InterruptedException e) {
            this.interrupt();
        } catch (Exception ignored) {
        }
    }
}

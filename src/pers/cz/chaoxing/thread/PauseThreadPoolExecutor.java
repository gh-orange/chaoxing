package pers.cz.chaoxing.thread;

import java.util.HashSet;
import java.util.concurrent.*;

/**
 * @author 橙子
 * @since 2018/11/26
 */
public class PauseThreadPoolExecutor extends ThreadPoolExecutor {
    private volatile boolean isPaused;
    private HashSet<PauseThread> workers = new HashSet<>();
    private final Object blockerLock = new Object();

    public PauseThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new PauseThreadFactory());
    }

    public PauseThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, PauseThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public PauseThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, new PauseThreadFactory(), handler);
    }

    public PauseThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, PauseThreadFactory threadFactory, RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        workers.add((PauseThread) t);
        super.beforeExecute(t, r);
        pausedIfSate((PauseThread) t);
        if (t.isInterrupted())
            this.getRejectedExecutionHandler().rejectedExecution(r, this);
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        workers.remove(t);
        super.afterExecute(r, t);
    }

    private void pausedIfSate(PauseThread thread) {
        if (isPaused && !thread.isPaused())
            try {
                thread.pause();
            } catch (SecurityException ignored) {
            }
    }

    public void pause() {
        synchronized (blockerLock) {
            isPaused = true;
        }
    }

    public void pauseNow() {
        if (!isPaused)
            synchronized (blockerLock) {
                isPaused = true;
                this.workers.forEach(this::pausedIfSate);
            }
    }

    public void resume() {
        if (isPaused)
            synchronized (blockerLock) {
                isPaused = false;
                for (PauseThread thread : this.workers) {
                    if (thread.isPaused())
                        try {
                            thread.process();
                        } catch (SecurityException ignored) {
                        }
                }
            }
    }

    public boolean isPaused() {
        return isPaused;
    }
}

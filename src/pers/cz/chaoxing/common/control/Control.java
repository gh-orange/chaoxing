package pers.cz.chaoxing.common.control;

import pers.cz.chaoxing.callback.PauseCallBack;
import pers.cz.chaoxing.thread.PauseThread;
import pers.cz.chaoxing.util.CompleteStyle;
import pers.cz.chaoxing.util.Try;

import java.util.Optional;
import java.util.concurrent.Semaphore;

/**
 * control task's behavior
 *
 * @author 橙子
 * @since 2018/11/29
 */
public class Control {
    private volatile boolean isSleep;
    private volatile boolean isReview;
    private volatile CompleteStyle completeStyle;
    private Semaphore semaphore;

    public Control() {
        this.semaphore = new Semaphore(4);
    }

    public boolean isSleep() {
        return isSleep;
    }

    public void setSleep(boolean sleep) {
        this.isSleep = sleep;
    }

    public boolean isReview() {
        return isReview;
    }

    public void setReview(boolean review) {
        this.isReview = review;
    }

    public CompleteStyle getCompleteStyle() {
        return completeStyle;
    }

    public void setCompleteStyle(CompleteStyle completeStyle) {
        this.completeStyle = completeStyle;
    }

    public void acquire() throws InterruptedException {
        if (this.isSleep)
            Optional.ofNullable(semaphore).ifPresent(Try.once(Semaphore::acquire));
    }

    public void release() {
        if (this.isSleep)
            Optional.ofNullable(semaphore).ifPresent(Semaphore::release);
    }

    public void checkState(PauseCallBack callBack) throws InterruptedException {
        if (Thread.currentThread().isInterrupted())
            throw new InterruptedException();
        try {
            synchronized (PauseThread.currentThread()) {
                while (PauseThread.currentThread().isPaused()) {
                    callBack.beforePause();
                    PauseThread.currentThread().wait();
                }
                callBack.afterPause();
            }
        } catch (ClassCastException ignored) {
        }
    }
}

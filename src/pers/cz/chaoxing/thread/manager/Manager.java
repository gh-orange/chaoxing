package pers.cz.chaoxing.thread.manager;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.thread.LimitedBlockingQueue;
import pers.cz.chaoxing.util.IOLock;
import pers.cz.chaoxing.util.Try;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * @author 橙子
 * @date 2018/9/29
 */
public abstract class Manager implements Runnable, Closeable {
    protected String baseUri;
    String uriModel;
    int threadCount;
    boolean hasSleep;
    boolean skipReview;
    boolean autoComplete;
    private int threadPoolSize;
    private ExecutorService threadPool;
    List<Map<String, String>> paramsList;
    CompletionService<Boolean> completionService;
    Semaphore semaphore;
    CallBack<?> customCallBack;

    Manager(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        if (this.threadPoolSize > 0) {
            this.threadPool = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS, new LimitedBlockingQueue<>(1));
            this.completionService = new ExecutorCompletionService<>(threadPool);
        }
        threadCount = 0;
    }

    @Override
    public final void run() {
        if (this.threadPoolSize > 0)
            try {
                doJob();
            } catch (RequestsException e) {
                String message = e.getLocalizedMessage();
                String begin = ":";
                int beginIndex = message.indexOf(begin);
                if (-1 != beginIndex)
                    message = message.substring(beginIndex + begin.length()).trim();
                String finalMessage = message;
                IOLock.output(() -> System.out.println("Net connection error: " + finalMessage));
                release();
            } catch (Exception ignored) {
                release();
            }
    }

    public abstract void doJob() throws Exception;

    void acquire() throws InterruptedException {
        Optional.ofNullable(semaphore).ifPresent(Try.once(Semaphore::acquire));
    }

    void release() {
        Optional.ofNullable(semaphore).ifPresent(Semaphore::release);
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public void setUriModel(String uriModel) {
        this.uriModel = uriModel;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setSkipReview(boolean skipReview) {
        this.skipReview = skipReview;
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }

    public void setParamsList(List<Map<String, String>> paramsList) {
        this.paramsList = paramsList;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void setCustomCallBack(CallBack<?> customCallBack) {
        this.customCallBack = customCallBack;
    }

    @Override
    public void close() {
        try {
            for (int i = 0; i < threadCount; i++)
                completionService.take().get();
        } catch (Exception ignored) {
        }
        if (this.threadPoolSize > 0)
            threadPool.shutdown();
    }
}

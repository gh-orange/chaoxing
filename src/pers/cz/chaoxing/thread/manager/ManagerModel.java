package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.common.control.Control;
import pers.cz.chaoxing.thread.LimitedBlockingQueue;
import pers.cz.chaoxing.callback.PauseCallBack;
import pers.cz.chaoxing.thread.PauseThreadPoolExecutor;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.io.StringUtil;
import net.dongliu.requests.exception.RequestsException;

import java.io.Closeable;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.LongStream;

/**
 * @author 橙子
 * @since 2018/9/29
 */
public abstract class ManagerModel implements PauseCallBack, Runnable, Closeable {
    long threadCount;
    long successCount;
    private int threadPoolSize;
    private PauseThreadPoolExecutor threadPool;
    Control control;
    List<List<String>> urls;
    CompletionService<Boolean> completionService;

    ManagerModel(int threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
        if (this.threadPoolSize > 0) {
            this.threadPool = new PauseThreadPoolExecutor(threadPoolSize, threadPoolSize, 0L, TimeUnit.MILLISECONDS, new LimitedBlockingQueue<>(1));
            this.completionService = new ExecutorCompletionService<>(threadPool);
        }
        threadCount = successCount = 0;
    }

    @Override
    public final void run() {
        if (this.threadPoolSize > 0) {
            try {
                doJob();
            } catch (RequestsException e) {
                String message = StringUtil.subStringAfterFirst(e.getLocalizedMessage(), ":").trim();
                IOUtil.println("Net connection error: " + message);
            } catch (RejectedExecutionException | InterruptedException e) {
                if (!threadPool.isShutdown())
                    threadPool.shutdownNow();
            } catch (Exception ignored) {
            }
            LongStream.range(0, threadPool.getTaskCount()).mapToObj(i -> {
                try {
                    return completionService.take().get();
                } catch (Exception e) {
                    return false;
                }
            });
        }
    }

    @Override
    public void beforePause() {
        threadPool.pauseNow();
    }

    @Override
    public void afterPause() {
        if (threadPool.isPaused())
            threadPool.resume();
    }

    public abstract void doJob() throws Exception;

    public void setControl(Control control) {
        this.control = control;
    }

    public void setUrls(List<List<String>> urls) {
        this.urls = urls;
    }

    @Override
    public void close() {
        if (this.threadPoolSize > 0) {
            threadPool.shutdown();
            successCount = LongStream.range(0, threadPool.getTaskCount()).mapToObj(i -> {
                try {
                    return completionService.take().get();
                } catch (Exception e) {
                    return false;
                }
            }).filter(result -> result).count();
        }
    }
}

package pers.cz.chaoxing.thread;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.task.PlayerData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.thread.task.PlayTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.InfoType;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * @author p_chncheng
 * @create 2018/9/4
 */
public class PlayerManager implements Runnable {
    private Semaphore semaphore;
    private int playerThreadPoolCount;
    private ExecutorService playerThreadPool;
    private CompletionService<Boolean> playerCompletionService;
    private int playerThreadCount = 0;
    private List<Map<String, String>> paramsList;
    private int clickCount;
    private boolean hasSleep;
    private String baseUri;
    private String cardUriModel;
    private CallBack<?> customCallBack;

    public PlayerManager(int playerThreadPoolCount) {
        this.playerThreadPoolCount = playerThreadPoolCount;
        if (this.playerThreadPoolCount > 0) {
            this.playerThreadPool = new ThreadPoolExecutor(playerThreadPoolCount, playerThreadPoolCount, 0L, TimeUnit.MILLISECONDS, new LimitedBlockingQueue<>(1));
            this.playerCompletionService = new ExecutorCompletionService<>(playerThreadPool);
        }
        this.clickCount = 0;
    }

    @Override
    public void run() {
        if (this.playerThreadPoolCount > 0)
            try {
                for (Map<String, String> params : paramsList) {
                    acquire();
                    while (true)
                        try {
                            TaskInfo<PlayerData> playerInfo = CXUtil.getTaskInfo(baseUri, cardUriModel, params, InfoType.Video);
                            if (playerInfo.getAttachments().length > 0 && !playerInfo.getAttachments()[0].isPassed()) {
                                if (CXUtil.startRecord(baseUri, params)) {
                                    VideoInfo videoInfo = CXUtil.getVideoInfo(baseUri, "/ananas/status", playerInfo.getAttachments()[0].getObjectId(), playerInfo.getDefaults().getFid());
                                    String videoName = videoInfo.getFilename();
                                    try {
                                        videoName = URLDecoder.decode(videoName, "utf-8");
                                    } catch (UnsupportedEncodingException ignored) {
                                    }
                                    System.out.println("Video did not pass:" + videoName);
                                    char[] charArray = playerInfo.getAttachments()[0].getType().toCharArray();
                                    charArray[0] -= 32;
                                    playerInfo.getAttachments()[0].setType(String.valueOf(charArray));
                                    PlayTask playTask = new PlayTask(playerInfo, videoInfo, baseUri);
                                    playTask.setCheckCodeCallBack(customCallBack);
                                    playTask.setHasSleep(hasSleep);
                                    playTask.setSemaphore(semaphore);
                                    playerCompletionService.submit(playTask);
                                    playerThreadCount++;
                                    System.out.println("Added playTask to ThreadPool:" + videoName);
                                } else
                                    release();
                            } else
                                release();
                        /*
                        imitate human click
                        */
                            if (hasSleep && ++clickCount % 15 == 0)
                                Thread.sleep(30 * 1000);
                            break;
                        } catch (CheckCodeException e) {
                            customCallBack.call(e.getSession(), e.getUri());
                        }
                }
                Iterator<Map<String, String>> iterator = paramsList.iterator();
                for (int i = 0; i < playerThreadCount && iterator.hasNext(); i++) {
                    try {
                        CXUtil.activeTask(baseUri, iterator.next());
                    } catch (CheckCodeException e) {
                        customCallBack.call(e.getSession(), e.getUri());
                    }
                    if (hasSleep)
                        Thread.sleep(10 * 1000);
                }
            } catch (RequestsException e) {
                System.out.println("Net connection error");
                release();
            } catch (Exception ignored) {
                release();
            }
        System.out.println("All player task has been called");
    }

    private void acquire() throws InterruptedException {
        if (null != semaphore)
            semaphore.acquire();
    }

    private void release() {
        if (null != semaphore)
            semaphore.release();
    }

    public void setParamsList(List<Map<String, String>> paramsList) {
        this.paramsList = paramsList;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public void setCardUriModel(String cardUriModel) {
        this.cardUriModel = cardUriModel;
    }

    public void setCustomCallBack(CallBack<?> customCallBack) {
        this.customCallBack = customCallBack;
    }

    public void close() {
        try {
            for (int i = 0; i < playerThreadCount; i++)
                playerCompletionService.take().get();
        } catch (Exception ignored) {
        }
        if (this.playerThreadPoolCount > 0)
            playerThreadPool.shutdown();
        System.out.println("Finished playTask count:" + playerThreadCount);
    }
}

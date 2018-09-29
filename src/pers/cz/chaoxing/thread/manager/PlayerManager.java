package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.thread.task.PlayTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * @author p_chncheng
 * @create 2018/9/4
 */
public class PlayerManager extends Manager {
    private int clickCount;

    public PlayerManager(int threadPoolCount) {
        super(threadPoolCount);
        this.clickCount = 0;
    }

    @Override
    public void doJob() throws Exception {
        paramsList.forEach(Try.once(params -> {
            acquire();
            TaskInfo<PlayerTaskData> playerInfo = Try.ever(() -> CXUtil.getTaskInfo(baseUri, uriModel, params, InfoType.Video), customCallBack);
            release();
            Arrays.stream(playerInfo.getAttachments())
                    .filter(attachment -> !attachment.isPassed())
                    .forEach(Try.once(attachment -> {
                        Try.ever(() -> {
                            if (CXUtil.startRecord(baseUri, params)) {
                                VideoInfo videoInfo = CXUtil.getVideoInfo(baseUri, "/ananas/status", attachment.getObjectId(), playerInfo.getDefaults().getFid());
                                String videoName = videoInfo.getFilename();
                                try {
                                    videoName = URLDecoder.decode(videoName, "utf-8");
                                } catch (UnsupportedEncodingException ignored) {
                                }
                                System.out.println("Video did not pass:" + videoName);
                                char[] charArray = attachment.getType().toCharArray();
                                if (charArray[0] >= 'A' && charArray[0] <= 'Z')
                                    charArray[0] -= 32;
                                attachment.setType(String.valueOf(charArray));
                                PlayTask playTask = new PlayTask(playerInfo, attachment, videoInfo, baseUri);
                                playTask.setCheckCodeCallBack(customCallBack);
                                playTask.setHasSleep(hasSleep);
                                playTask.setSemaphore(semaphore);
                                completionService.submit(playTask);
                                threadCount++;
                                System.out.println("Added playTask to ThreadPool:" + videoName);
                            }
                        }, customCallBack);
                        /*
                        imitate human click
                        */
                        if (hasSleep && ++clickCount % 15 == 0)
                            Thread.sleep(30 * 1000);
                    }));
        }));
        Iterator<Map<String, String>> iterator = paramsList.iterator();
        for (int i = 0; i < threadCount && iterator.hasNext(); i++) {
            acquire();
            Try.ever(() -> CXUtil.activeTask(baseUri, iterator.next()), customCallBack);
            if (hasSleep)
                Thread.sleep(10 * 1000);
            release();
        }
        System.out.println("All player task has been called");
    }

    public void close() {
        super.close();
        System.out.println("Finished playTask count:" + threadCount);
    }
}

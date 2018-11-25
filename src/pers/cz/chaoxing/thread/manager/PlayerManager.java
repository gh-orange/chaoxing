package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.callback.CheckCodeSingletonFactory;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.thread.task.PlayTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.IOUtil;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * @author p_chncheng
 * @since 2018/9/4
 */
public class PlayerManager extends ManagerModel {
    private int clickCount;

    public PlayerManager(int threadPoolSize) {
        super(threadPoolSize);
        this.clickCount = 0;
    }

    @Override
    public void doJob() throws Exception {
        paramsList.forEach(Try.once(params -> {
            acquire();
            TaskInfo<PlayerTaskData> playerInfo = Try.ever(() -> CXUtil.getTaskInfo(baseUri, uriModel, params, InfoType.Video), CheckCodeSingletonFactory.CUSTOM.get());
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
                                String finalVideoName = videoName;
                                IOUtil.println("Video did not pass: " + finalVideoName);
                                char[] charArray = attachment.getType().toCharArray();
                                if (charArray[0] >= 'A' && charArray[0] <= 'Z')
                                    charArray[0] -= 32;
                                attachment.setType(String.valueOf(charArray));
                                PlayTask playTask = new PlayTask(playerInfo, attachment, videoInfo, baseUri);
                                playTask.setCheckCodeCallBack(CheckCodeSingletonFactory.CUSTOM.get());
                                playTask.setHasSleep(hasSleep);
                                playTask.setSemaphore(semaphore);
                                playTask.setCompleteStyle(completeStyle);
                                completionService.submit(playTask);
                                threadCount++;
                                IOUtil.println("Added playTask to ThreadPool: " + finalVideoName);
                            }
                        }, CheckCodeSingletonFactory.CUSTOM.get());
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
            Try.ever(() -> CXUtil.activeTask(baseUri, iterator.next()), CheckCodeSingletonFactory.CUSTOM.get());
            if (hasSleep)
                Thread.sleep(10 * 1000);
            release();
        }
        IOUtil.println("All player task has been called");
    }

    public void close() {
        super.close();
        IOUtil.println("Finished playTask count: " + threadCount);
    }
}

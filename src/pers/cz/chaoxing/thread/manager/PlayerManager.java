package pers.cz.chaoxing.thread.manager;

import net.dongliu.requests.Parameter;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.thread.task.PlayTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.net.NetUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;

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
        urls.stream().flatMap(Collection::stream).forEach(Try.once(url -> {
            control.checkState(this);
            TaskInfo<PlayerTaskData> playerInfo;
            control.acquire();
            try {
                playerInfo = Try.ever(() -> CXUtil.getTaskInfo(url, InfoType.PLAYER), CheckCodeFactory.CUSTOM.get());
            } finally {
                control.release();
            }
            Arrays.stream(playerInfo.getAttachments())
                    .filter(attachment -> !attachment.isPassed())
                    .forEach(Try.once(attachment -> {
                        Try.ever(() -> {
                            if (CXUtil.startPlayer(url)) {
                                VideoInfo videoInfo = CXUtil.getVideoInfo(url, attachment.getObjectId(), playerInfo.getDefaults().getFid());
                                String videoName;
                                try {
                                    videoName = URLDecoder.decode(videoInfo.getFilename(), "utf-8");
                                } catch (UnsupportedEncodingException e) {
                                    videoName = videoInfo.getFilename();
                                }
                                IOUtil.println("Video did not pass: " + videoName);
                                char[] charArray = attachment.getType().toCharArray();
                                if (charArray[0] >= 'A' && charArray[0] <= 'Z')
                                    charArray[0] -= 32;
                                attachment.setType(String.valueOf(charArray));
                                PlayTask playTask = new PlayTask(playerInfo, attachment, videoInfo, url);
                                playTask.setControl(control);
                                playTask.setCheckCodeCallBack(CheckCodeFactory.CUSTOM.get());
                                completionService.submit(playTask);
                                threadCount++;
                                IOUtil.println("Added playTask to ThreadPool: " + videoName);
                            }
                        }, CheckCodeFactory.CUSTOM.get());
                        /*
                        imitate human click
                        */
                        if (control.isSleep() && ++clickCount % 15 == 0)
                            Thread.sleep(30 * 1000);
                    }));
        }));
        IOUtil.println("All player task has been called");
    }

    public void close() {
        super.close();
        IOUtil.println("Finished playTask count: " + successCount + "/" + threadCount);
    }
}

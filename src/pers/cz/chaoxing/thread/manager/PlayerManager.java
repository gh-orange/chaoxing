package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.callback.checkcode.CheckCodeData;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.common.ReadInfo;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.quiz.data.player.VideoQuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.TaskData;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.thread.task.LiveTask;
import pers.cz.chaoxing.thread.task.ReadTask;
import pers.cz.chaoxing.thread.task.TaskModel;
import pers.cz.chaoxing.thread.task.VideoTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

/**
 * @author p_chncheng
 * @since 2018/9/4
 */
public class PlayerManager extends ManagerModel {
    private int clickCount;
    private String utEnc;
    private CheckCodeData checkCodeData;

    public PlayerManager(int threadPoolSize) {
        super(threadPoolSize);
        this.clickCount = 0;
        this.checkCodeData = new CheckCodeData(true, "", "0");
    }

    @Override
    public void doJob() throws Exception {
        urls.forEach(Try.once(chapterURLs -> {
            this.utEnc = "";
            chapterURLs.forEach(Try.once(url -> {
                control.checkState(this);
                TaskInfo<PlayerTaskData> playerInfo;
                control.acquire();
                try {
                    playerInfo = Try.ever(() -> CXUtil.getTaskInfo(url + checkCodeData.toQueryString(), InfoType.PLAYER), CheckCodeFactory.CHAPTER.get(), checkCodeData);
                } finally {
                    control.release();
                }
                Arrays.stream(playerInfo.getAttachments())
                        .filter(attachment -> "read".equals(attachment.getType()) ? (!attachment.getProperty().getJobid().isEmpty() && (attachment.isJob() || !attachment.getProperty().getJobid().equals(attachment.getJobid()))) : !attachment.isPassed())
                        .forEach(Try.once(attachment -> {
                            Try.ever(() -> {
                                if (CXUtil.startPlayer(url)) {
                                    TaskModel<? extends TaskData, VideoQuizData> playerTask;
                                    String playerName;
                                    switch (attachment.getType()) {
                                        case "read":
                                            if (this.utEnc.isEmpty())
                                                try {
                                                    this.utEnc = getUtEnc(chapterURLs);
                                                } catch (InterruptedException ignored) {
                                                }
                                            ReadInfo readInfo = CXUtil.getReadInfo(url, playerInfo.getDefaults().getClazzId(), playerInfo.getDefaults().getCourseid(), playerInfo.getDefaults().getKnowledgeid(), attachment.getProperty().getJobid(), attachment.getEnc(), utEnc);
                                            readInfo.setCourseId(attachment.getProperty().getId());
                                            playerName = attachment.getProperty().getTitle();
                                            IOUtil.println("manager_player_read_thread_start", playerName);
                                            playerTask = new ReadTask(playerInfo, attachment, readInfo, url);
                                            break;
                                        case "live":
                                            playerName = attachment.getProperty().getTitle();
                                            IOUtil.println("manager_player_live_thread_start", playerName);
                                            playerTask = new LiveTask(playerInfo, attachment, url);
                                            break;
                                        default:
                                            VideoInfo videoInfo = CXUtil.getVideoInfo(url, attachment.getObjectId(), playerInfo.getDefaults().getFid());
                                            try {
                                                playerName = URLDecoder.decode(videoInfo.getFilename(), "utf-8");
                                            } catch (UnsupportedEncodingException e) {
                                                playerName = videoInfo.getFilename();
                                            }
                                            IOUtil.println("manager_player_video_thread_start", playerName);
                                            char[] charArray = attachment.getType().toCharArray();
                                            if (charArray[0] >= 'A' && charArray[0] <= 'Z')
                                                charArray[0] -= 32;
                                            attachment.setType(String.valueOf(charArray));
                                            playerTask = new VideoTask(playerInfo, attachment, videoInfo, url);
                                            break;
                                    }
                                    playerTask.setControl(control);
                                    playerTask.setCheckCodeCallBack(CheckCodeFactory.CUSTOM.get());
                                    completionService.submit(playerTask);
                                    threadCount++;
                                    IOUtil.println("manager_player_thread_finish", playerName);
                                }
                            }, CheckCodeFactory.CUSTOM.get());
                            /*
                            imitate human click
                            */
                            if (control.isSleep() && ++clickCount % 15 == 0)
                                Thread.sleep(30 * 1000);
                        }));
            }));
        }));
        IOUtil.println("manager_player_start");
    }

    private String getUtEnc(List<String> chapterURLs) throws InterruptedException {
        control.acquire();
        try {
            return chapterURLs.stream()
                    .map(url -> Try.ever(() -> CXUtil.getUtEnc(url), CheckCodeFactory.CUSTOM.get()))
                    .filter(str -> !str.isEmpty())
                    .findFirst().orElse("");
        } finally {
            control.release();
        }
    }

    public void close() {
        super.close();
        IOUtil.println("manager_player_finish", successCount, threadCount);
    }
}

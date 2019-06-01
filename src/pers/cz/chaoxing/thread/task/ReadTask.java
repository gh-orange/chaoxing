package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.ReadInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.player.VideoQuizData;
import pers.cz.chaoxing.common.task.ReadCardInfo;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.player.ReadTaskData;
import pers.cz.chaoxing.thread.PauseThread;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;

import java.util.*;

/**
 * @author 橙子
 * @since 2019/5/29
 */
public class ReadTask extends TaskModel<ReadTaskData, VideoQuizData> {
    private final ReadInfo readInfo;
    private int readSecond;
    private final List<ReadCardInfo> readCardInfos;

    public ReadTask(TaskInfo<? extends ReadTaskData> taskInfo, ReadTaskData attachment, ReadInfo readInfo, String url) {
        super((TaskInfo<ReadTaskData>) taskInfo, attachment, url);
        this.readInfo = readInfo;
        this.taskName = attachment.getProperty().getTitle();
        this.readSecond = readInfo.getHeadOffset();
        this.readCardInfos = new ArrayList<>();
    }

    @Override
    protected void doTask() throws Exception {
        threadPrintln("thread_player_read_start", this.taskName);
        startRefreshTask();
        taskInfo.getDefaults().setInitdataUrl(url);
        taskInfo.getDefaults().setReportTimeInterval(60);
        final Random random = new Random();
        do {
            Thread.sleep(taskInfo.getDefaults().getReportTimeInterval() * 1000);
            if (!PauseThread.currentThread().isPaused()) {
                threadPrintln("thread_player_read_process", this.taskName, (int) ((float) readSecond / readInfo.getDuration() * 100));
                readSecond += taskInfo.getDefaults().getReportTimeInterval();
            }
            control.checkState(this);
            final ReadCardInfo readCardInfo = getReadCardInfo(random.nextInt());
            Try.ever(() -> CXUtil.onReadProgress(url, readInfo, readCardInfo, readCardInfo.getStart() + random.nextInt(readCardInfo.getHeight())), checkCodeCallBack);
        } while (readSecond < readInfo.getDuration());
        threadPrintln("thread_player_read_finish", this.taskName);
    }

    private ReadCardInfo getReadCardInfo(int height) {
        ReadCardInfo readCardInfo;
        if (readCardInfos.isEmpty()) {
            readCardInfo = Try.ever(() -> CXUtil.onReadStart(taskInfo, attachment, readInfo), checkCodeCallBack);
            readInfo.setHeight(readCardInfo.getHeight());
        } else
            try {
                return readCardInfos.get(Collections.binarySearch(readCardInfos, height));
            } catch (IndexOutOfBoundsException e) {
                readCardInfo = Try.ever(() -> CXUtil.getReadCard(url, readInfo, readCardInfos.get(readCardInfos.size() - 1)), checkCodeCallBack);
            }
        readCardInfos.add(readCardInfo);
        return readCardInfo;
    }

    @Override
    protected Map<VideoQuizData, List<OptionInfo>> getAnswers(QuizInfo<VideoQuizData, ?> quizInfo) {
        return null;
    }

    @Override
    protected boolean storeQuestion(Map<VideoQuizData, List<OptionInfo>> answers) throws Exception {
        return false;
    }

    @Override
    protected boolean answerQuestion(Map<VideoQuizData, List<OptionInfo>> answers) throws Exception {
        return false;
    }
}

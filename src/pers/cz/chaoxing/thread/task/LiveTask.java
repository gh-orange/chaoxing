package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.player.VideoQuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LiveTask extends TaskModel<PlayerTaskData, VideoQuizData> {
    public LiveTask(TaskInfo<PlayerTaskData> taskInfo, PlayerTaskData attachment, String url) {
        super(taskInfo, attachment, url);
        this.taskName = attachment.getProperty().getTitle();
    }

    @Override
    public void doTask() throws Exception {
        threadPrintln("thread_player_live_start", this.taskName);
        startRefreshTask();
        threadPrintln("thread_player_live_process", this.taskName, this.url);
        throw new UnsupportedOperationException();
    }

    @Override
    protected Map<VideoQuizData, List<OptionInfo>> getAnswers(QuizInfo<VideoQuizData, ?> quizInfo) {
        return null;
    }

    @Override
    protected boolean storeQuestion(Map<VideoQuizData, List<OptionInfo>> answers) {
        return true;
    }

    @Override
    protected boolean answerQuestion(Map<VideoQuizData, List<OptionInfo>> answers) {
        answers.forEach((quizData, options) -> {
            String answerStr = options.stream().map(OptionInfo::getName).collect(Collectors.joining());
            if (!answerStr.isEmpty())
                Try.ever(() -> quizData.setAnswered(CXUtil.answerVideoQuiz(quizData.getValidationUrl(), quizData.getResourceId(), answerStr)), checkCodeCallBack);
        });
        Iterator<VideoQuizData> iterator = answers.keySet().iterator();
        if (iterator.hasNext())
            return iterator.next().isAnswered();
        return false;
    }
}

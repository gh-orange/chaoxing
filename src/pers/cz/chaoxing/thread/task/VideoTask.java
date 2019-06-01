package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.player.VideoQuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.thread.PauseThread;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

public class VideoTask extends TaskModel<PlayerTaskData, VideoQuizData> {
    private final VideoInfo videoInfo;
    private int playSecond;

    public VideoTask(TaskInfo<PlayerTaskData> taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, String url) {
        super(taskInfo, attachment, url);
        this.videoInfo = videoInfo;
        this.playSecond = (int) (this.attachment.getHeadOffset() / 1000);
        try {
            this.taskName = URLDecoder.decode(videoInfo.getFilename(), "utf-8");
        } catch (UnsupportedEncodingException e) {
            this.taskName = videoInfo.getFilename();
        }
        this.attachment.setVideoJSId((int) (Math.random() * 9999999));
    }

    @Override
    public void doTask() throws Exception {
        threadPrintln("thread_player_video_start", this.taskName);
        startRefreshTask();
        List<QuizInfo<VideoQuizData, Void>> playerQuizInfoArray = Try.ever(() -> CXUtil.getVideoQuizzes(taskInfo.getDefaults().getInitdataUrl(), attachment.getMid()), checkCodeCallBack);
        boolean isPassed = Try.ever(() -> CXUtil.onVideoStart(taskInfo, attachment, videoInfo), checkCodeCallBack);
        if (!isPassed) {
            try {
                do {
                    if (control.isSleep())
                        Thread.sleep(taskInfo.getDefaults().getReportTimeInterval() * 1000);
                    if (!PauseThread.currentThread().isPaused()) {
                        threadPrintln("thread_player_video_process", this.taskName, (int) ((float) this.playSecond / this.videoInfo.getDuration() * 100));
                        playSecond += taskInfo.getDefaults().getReportTimeInterval();
                    }
                    playerQuizInfoArray.forEach(Try.once(this::doAnswer));
                    if (playSecond > videoInfo.getDuration()) {
                        playSecond = videoInfo.getDuration();
                        break;
                    }
                    isPassed = Try.ever(() -> CXUtil.onVideoProgress(taskInfo, attachment, videoInfo, playSecond), checkCodeCallBack);
                } while (PauseThread.currentThread().isPaused() || !isPassed);
                Try.ever(() -> CXUtil.onVideoEnd(taskInfo, attachment, videoInfo), checkCodeCallBack);
                threadPrintln("thread_player_video_finish", this.taskName);
            } catch (InterruptedException e) {
                Try.ever(() -> CXUtil.onVideoPause(taskInfo, attachment, videoInfo, playSecond), checkCodeCallBack);
            }
        } else if (!playerQuizInfoArray.isEmpty()) {
            playSecond = videoInfo.getDuration();
            playerQuizInfoArray.forEach(Try.once(this::doAnswer));
        }
        threadPrintln("thread_player_video_answer_finish", this.taskName);
    }

    public void setPlaySecond(int playSecond) {
        this.playSecond = playSecond;
    }

    private void doAnswer(QuizInfo<VideoQuizData, Void> playerQuizInfo) throws InterruptedException {
        Map<VideoQuizData, List<OptionInfo>> answers = getAnswers(playerQuizInfo);
        control.checkState(this);
        if (answerQuestion(answers))
            answers.forEach((key, value) -> threadPrintln(
                    "thread_player_video_answer_success", new Object[]{this.taskName},
                    key.getDescription(),
                    StringUtil.join(value)
            ));
    }

    @Override
    protected Map<VideoQuizData, List<OptionInfo>> getAnswers(QuizInfo<VideoQuizData, ?> quizInfo) {
        Map<VideoQuizData, List<OptionInfo>> questions = new HashMap<>();
        if (quizInfo.getStyle().equals("QUIZ"))
            Arrays.stream(quizInfo.getDatas())
                    .filter(quizData -> !quizData.isAnswered() && playSecond >= quizData.getStartTime())
                    .forEach(quizData -> {
                        Arrays.stream(quizData.getOptions())
                                .filter(OptionInfo::isRight)
                                .forEach(questions.computeIfAbsent(quizData, key -> new ArrayList<>())::add);
                        if (!questions.containsKey(quizData))
                            CXUtil.getQuizAnswer(quizData).forEach(optionInfo -> questions.computeIfAbsent(quizData, key -> new ArrayList<>()).add(optionInfo));
                        if (!questions.containsKey(quizData)) {
                            threadPrintln("thread_player_video_answer_failure", new Object[]{this.taskName},
                                    quizData.toString());
                            hasFail = !completeAnswer(questions, quizData);
                        }
                        if (questions.containsKey(quizData))
                            quizData.setAnswered(false);
                    });
        return questions;
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

package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.player.PlayerQuizData;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.TaskState;
import pers.cz.chaoxing.util.Try;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

public class PlayTask extends Task<PlayerTaskData, PlayerQuizData> {
    private final VideoInfo videoInfo;
    private int playSecond;

    public PlayTask(TaskInfo<PlayerTaskData> taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, String baseUri) {
        super(taskInfo, attachment, baseUri);
        this.videoInfo = videoInfo;
        this.playSecond = (int) (this.attachment.getHeadOffset() / 1000);
        try {
            this.taskName = URLDecoder.decode(videoInfo.getFilename(), "utf-8");
        } catch (UnsupportedEncodingException ignored) {
            this.taskName = videoInfo.getFilename();
        }
        this.attachment.setVideoJSId((int) (Math.random() * 9999999));
    }

    @Override
    public void doTask() throws Exception {
        checkCodeCallBack.print(this.taskName + "[player start]");
        List<QuizInfo<PlayerQuizData, Void>> playerQuizInfoArray = Try.ever(() -> CXUtil.getPlayerQuizzes(taskInfo.getDefaults().getInitdataUrl(), attachment.getMid()), checkCodeCallBack);
        boolean isPassed = Try.ever(() -> CXUtil.onStart(taskInfo, attachment, videoInfo), checkCodeCallBack);
        if (!isPassed) {
            do {
                if (hasSleep)
                    for (int i = 0; !taskState.equals(TaskState.STOP) && i < taskInfo.getDefaults().getReportTimeInterval(); i++)
                        Thread.sleep(1000);
                if (taskState.equals(TaskState.STOP))
                    break;
                if (!taskState.equals(TaskState.PAUSE)) {
                    checkCodeCallBack.print(this.taskName + "[video play " + (int) ((float) this.playSecond / this.videoInfo.getDuration() * 100) + "%]");
                    playSecond += taskInfo.getDefaults().getReportTimeInterval();
                }
                playerQuizInfoArray.forEach(Try.once(this::doAnswer));
                if (playSecond > videoInfo.getDuration()) {
                    playSecond = videoInfo.getDuration();
                    break;
                }
                isPassed = Try.ever(() -> CXUtil.onPlayProgress(taskInfo, attachment, videoInfo, playSecond), checkCodeCallBack);
            } while (taskState.equals(TaskState.PAUSE) || !isPassed);
            Try.ever(() -> {
                if (taskState.equals(TaskState.STOP))
                    CXUtil.onPause(taskInfo, attachment, videoInfo, playSecond);
                else
                    CXUtil.onEnd(taskInfo, attachment, videoInfo);
            }, checkCodeCallBack);
            checkCodeCallBack.print(this.taskName + "[video play finish]");
        } else if (!playerQuizInfoArray.isEmpty()) {
            playSecond = videoInfo.getDuration();
            playerQuizInfoArray.forEach(Try.once(this::doAnswer));
        }
        checkCodeCallBack.print(this.taskName + "[quiz answer finish]");
    }

    public void setPlaySecond(int playSecond) {
        this.playSecond = playSecond;
    }

    private void doAnswer(QuizInfo<PlayerQuizData, Void> playerQuizInfo) throws InterruptedException {
        Map<PlayerQuizData, List<OptionInfo>> answers = getAnswers(playerQuizInfo);
        if (this.isStopState())
            return;
        if (answerQuestion(answers))
            answers.forEach((playerQuizData, options) -> checkCodeCallBack.print(
                    this.taskName + "[quiz answer success]",
                    playerQuizData.getDescription(),
                    options.stream().map(optionInfo -> optionInfo.getName() + "." + optionInfo.getDescription()).toArray(String[]::new)));
    }

    @Override
    protected Map<PlayerQuizData, List<OptionInfo>> getAnswers(QuizInfo<PlayerQuizData, ?> quizInfo) {
        Map<PlayerQuizData, List<OptionInfo>> questions = new HashMap<>();
        if (quizInfo.getStyle().equals("QUIZ"))
            Arrays.stream(quizInfo.getDatas())
                    .filter(quizData -> !quizData.isAnswered() && playSecond >= quizData.getStartTime())
                    .forEach(quizData -> {
                        Arrays.stream(quizData.getOptions())
                                .filter(OptionInfo::isRight)
                                .forEach(questions.computeIfAbsent(quizData, key -> new ArrayList<>())::add);
                        if (!questions.containsKey(quizData))
                            CXUtil.getQuizAnswer(quizData).forEach(questions.computeIfAbsent(quizData, key -> new ArrayList<>())::add);
                        if (!questions.containsKey(quizData)) {
                            checkCodeCallBack.print(this.taskName + "[quiz answer match failure]",
                                    quizData.getDescription(),
                                    Arrays.stream(quizData.getOptions()).map(optionInfo -> optionInfo.getName() + "." + optionInfo.getDescription()).toArray(String[]::new));
                            if (autoComplete)
                                questions.put(quizData, autoCompleteAnswer(quizData));
                            else
                                hasFail = true;
                        }
                        if (questions.containsKey(quizData))
                            quizData.setAnswered(false);
                    });
        return questions;
    }

    @Override
    protected boolean storeQuestion(Map<PlayerQuizData, List<OptionInfo>> answers) {
        return true;
    }

    @Override
    protected boolean answerQuestion(Map<PlayerQuizData, List<OptionInfo>> answers) {
        answers.forEach((quizData, options) -> {
            String answerStr = options.stream().map(OptionInfo::getName).collect(Collectors.joining());
            if (!answerStr.isEmpty())
                Try.ever(() -> quizData.setAnswered(CXUtil.answerPlayerQuiz(baseUri, quizData.getValidationUrl(), quizData.getResourceId(), answerStr)), checkCodeCallBack);
        });
        Iterator<PlayerQuizData> iterator = answers.keySet().iterator();
        if (iterator.hasNext())
            return iterator.next().isAnswered();
        return false;
    }
}

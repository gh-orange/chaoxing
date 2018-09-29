package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.quiz.data.player.PlayerQuizData;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

public class PlayTask extends Task<PlayerTaskData> {
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
    }

    @Override
    public void doTask() throws Exception {
        checkCodeCallBack.print(this.taskName + "[play start]");
        QuizInfo<PlayerQuizData, Void>[] playerQuizInfoArray = getQuestions(taskInfo, attachment);
        boolean isPassed = Try.ever(() -> CXUtil.onStart(taskInfo, attachment, videoInfo), checkCodeCallBack);
        if (!isPassed) {
            do {
                if (hasSleep)
                    for (int i = 0; !stop && i < taskInfo.getDefaults().getReportTimeInterval(); i++)
                        Thread.sleep(1000);
                if (stop)
                    break;
                if (!pause) {
                    checkCodeCallBack.print(this.taskName + "[play " + (int) ((float) this.playSecond / this.videoInfo.getDuration() * 100) + "%]");
                    playSecond += taskInfo.getDefaults().getReportTimeInterval();
                }
                Arrays.stream(playerQuizInfoArray).forEach(this::doAnswer);
                if (playSecond > videoInfo.getDuration()) {
                    playSecond = videoInfo.getDuration();
                    break;
                }
                isPassed = Try.ever(() -> CXUtil.onPlayProgress(taskInfo, attachment, videoInfo, playSecond), checkCodeCallBack);
            } while (pause || !isPassed);
            Try.ever(() -> {
                if (stop)
                    CXUtil.onPause(taskInfo, attachment, videoInfo, playSecond);
                else
                    CXUtil.onEnd(taskInfo, attachment, videoInfo);
            }, checkCodeCallBack);
            checkCodeCallBack.print(this.taskName + "[play finish]");
        } else if (0 != playerQuizInfoArray.length) {
            playSecond = videoInfo.getDuration();
            Arrays.stream(playerQuizInfoArray).forEach(this::doAnswer);
        }
    }

    public void setPlaySecond(int playSecond) {
        this.playSecond = playSecond;
    }

    private void doAnswer(QuizInfo<PlayerQuizData, Void> playerQuizInfo) {
        getAnswers(playerQuizInfo).entrySet().stream()
                .filter(question -> answerQuestion((PlayerQuizData) question.getKey(), question.getValue()))
                .forEach(question -> {
                    if (hasFail)
                        System.out.print("store success:");
                    else
                        System.out.print("answer success:");
                    System.out.println(question.getKey().getDescription());
                    question.getValue().forEach(optionInfo -> System.out.println(optionInfo.getName() + "." + optionInfo.getDescription()));
                });
    }

    private QuizInfo<PlayerQuizData, Void>[] getQuestions(TaskInfo taskInfo, PlayerTaskData attachment) throws Exception {
        return Try.ever(() -> CXUtil.getPlayerQuizzes(taskInfo.getDefaults().getInitdataUrl(), attachment.getMid()), checkCodeCallBack);
    }

    protected Map<QuizData, List<OptionInfo>> getAnswers(QuizInfo quizInfo) {
        Map<QuizData, List<OptionInfo>> questions = new HashMap<>();
        if (quizInfo.getStyle().equals("QUIZ"))
            Arrays.stream(quizInfo.getDatas())
                    .filter(quizData -> !quizData.isAnswered() && playSecond >= ((PlayerQuizData) quizData).getStartTime())
                    .forEach(quizData -> {
                        Arrays.stream(quizData.getOptions())
                                .filter(OptionInfo::isRight)
                                .forEach(questions.computeIfAbsent(quizData, key -> new ArrayList<>())::add);
                        if (!questions.containsKey(quizData))
                            CXUtil.getQuizAnswer(quizData).forEach(questions.computeIfAbsent(quizData, key -> new ArrayList<>())::add);
                        if (!questions.containsKey(quizData)) {
                            System.out.println(taskName + " player answer match failure:");
                            System.out.println(quizData.getDescription());
                            Arrays.stream(quizData.getOptions()).forEach(optionInfo -> System.out.println(optionInfo.getName() + "." + optionInfo.getDescription()));
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

    private boolean answerQuestion(PlayerQuizData playerQuizData, List<OptionInfo> optionInfoList) {
        try {
            Try.ever(() -> {
                StringBuilder stringBuffer = new StringBuilder();
                optionInfoList.stream().map(OptionInfo::getName).forEach(stringBuffer::append);
                playerQuizData.setAnswered(CXUtil.answerPlayerQuiz(baseUri, playerQuizData.getValidationUrl(), playerQuizData.getResourceId(), stringBuffer.toString()));
            }, checkCodeCallBack);
            return playerQuizData.isAnswered();
        } catch (Exception ignored) {
            return false;
        }
    }
}

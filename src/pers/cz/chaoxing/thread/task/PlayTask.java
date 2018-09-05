package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.common.quiz.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizConfig;
import pers.cz.chaoxing.common.task.PlayerData;
import pers.cz.chaoxing.common.quiz.PlayerQuizInfo;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.util.CXUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class PlayTask implements Runnable, Callable<Boolean> {
    private Semaphore semaphore;
    private final TaskInfo<PlayerData> taskInfo;
    private final PlayerData attachment;
    private final VideoInfo videoInfo;
    private final String baseUri;
    private String videoName;
    private int playSecond;
    private boolean pause;
    private boolean stop;
    private boolean hasSleep;
    private CallBack<?> checkCodeCallBack;

    public PlayTask(TaskInfo<PlayerData> taskInfo, PlayerData attachment, VideoInfo videoInfo, String baseUri) {
        this.taskInfo = taskInfo;
        this.attachment = attachment;
        this.videoInfo = videoInfo;
        this.baseUri = baseUri;
        this.playSecond = (int) (this.attachment.getHeadOffset() / 1000);
        this.stop = this.pause = false;
        this.hasSleep = true;
        try {
            this.videoName = URLDecoder.decode(videoInfo.getFilename(), "utf-8");
        } catch (UnsupportedEncodingException ignored) {
            this.videoName = videoInfo.getFilename();
        }
    }

    @Override
    public void run() {
        try {
            acquire();
            try {
                boolean isPassed;
                Map<QuizConfig, OptionInfo> questions = getQuestions(taskInfo, attachment);
                while (true)
                    try {
                        isPassed = CXUtil.onStart(taskInfo, attachment, videoInfo);
                        break;
                    } catch (CheckCodeException e) {
                        if (checkCodeCallBack != null)
                            checkCodeCallBack.call(e.getSession(), e.getUri());
                    }
                checkCodeCallBack.print(this.videoName + "[play start]");
                if (!isPassed) {
                    do {
                        if (hasSleep)
                            for (int i = 0; !stop && i < taskInfo.getDefaults().getReportTimeInterval(); i++)
                                Thread.sleep(1000);
                        if (stop)
                            break;
                        if (!pause) {
                            checkCodeCallBack.print(this.videoName + "[play " + (int) ((float) this.playSecond / this.videoInfo.getDuration() * 100) + "%]");
                            playSecond += taskInfo.getDefaults().getReportTimeInterval();
                        }
                        if (playSecond > videoInfo.getDuration()) {
                            playSecond = videoInfo.getDuration();
                            break;
                        }
                        for (Map.Entry<QuizConfig, OptionInfo> question : questions.entrySet())
                            if (playSecond >= question.getKey().getStartTime())
                                if (answerQuestion(question)) {
                                    questions.remove(question.getKey());
                                    System.out.println("answer success:" + question.getKey().getDescription() + "=" + question.getValue().getDescription());
                                }
                        while (true)
                            try {
                                isPassed = CXUtil.onPlayProgress(taskInfo, attachment, videoInfo, playSecond);
                                break;
                            } catch (CheckCodeException e) {
                                if (checkCodeCallBack != null)
                                    checkCodeCallBack.call(e.getSession(), e.getUri());
                            }
                    } while (pause || !isPassed);
                    while (true)
                        try {
                            if (stop)
                                CXUtil.onPause(taskInfo, attachment, videoInfo, playSecond);
                            else
                                CXUtil.onEnd(taskInfo, attachment, videoInfo);
                            break;
                        } catch (CheckCodeException e) {
                            if (checkCodeCallBack != null)
                                checkCodeCallBack.call(e.getSession(), e.getUri());
                        }
                    checkCodeCallBack.print(this.videoName + "[play finish]");
                } else if (!questions.isEmpty())
                    for (Map.Entry<QuizConfig, OptionInfo> question : questions.entrySet())
                        if (answerQuestion(question)) {
                            questions.remove(question.getKey());
                            System.out.println("answer success:" + question.getKey().getDescription() + "=" + question.getValue().getDescription());
                        }
            } catch (InterruptedException e) {
                System.out.println(e.getMessage());
            }
            release();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public Boolean call() {
        run();
        return true;
    }

    private void acquire() throws InterruptedException {
        if (null != semaphore)
            semaphore.acquire();
    }

    private void release() {
        if (null != semaphore)
            semaphore.release();
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setPlaySecond(int playSecond) {
        this.playSecond = playSecond;
    }

    public void setCheckCodeCallBack(CallBack<?> checkCodeCallBack) {
        this.checkCodeCallBack = checkCodeCallBack;
    }

    private Map<QuizConfig, OptionInfo> getQuestions(TaskInfo taskInfo, PlayerData attachment) {
        List<PlayerQuizInfo> playerQuizInfoList;
        while (true)
            try {
                playerQuizInfoList = CXUtil.getVideoQuiz(taskInfo.getDefaults().getInitdataUrl(), attachment.getMid());
                break;
            } catch (CheckCodeException e) {
                this.checkCodeCallBack.call(e.getSession(), e.getUri());
            }
        Map<QuizConfig, OptionInfo> questions = new HashMap<>();
        for (PlayerQuizInfo playerQuizInfo : playerQuizInfoList)
            if (playerQuizInfo.getStyle().equals("QUIZ"))
                for (QuizConfig quizConfig : playerQuizInfo.getDatas())
                    if (!quizConfig.isAnswered())
                        for (OptionInfo optionInfo : quizConfig.getOptions())
                            if (optionInfo.isRight()) {
                                questions.put(quizConfig, optionInfo);
                                break;
                            }
        return questions;
    }

    private boolean answerQuestion(Map.Entry<QuizConfig, OptionInfo> question) {
        boolean isPassed;
        while (true)
            try {
                isPassed = CXUtil.answerVideoQuiz(baseUri, question.getKey().getValidationUrl(), question.getKey().getResourceId(), question.getValue().getName());
                break;
            } catch (CheckCodeException e) {
                if (checkCodeCallBack != null)
                    checkCodeCallBack.call(e.getSession(), e.getUri());
            }
        return isPassed;
    }
}

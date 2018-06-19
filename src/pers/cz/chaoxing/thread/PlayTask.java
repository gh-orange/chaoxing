package pers.cz.chaoxing.thread;

import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.common.*;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.util.CXUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayTask implements Runnable {
    private final PlayerInfo playerInfo;
    private final VideoInfo videoInfo;
    private final String baseUri;
    private String videoName;
    private int playSecond;
    private boolean pause;
    private boolean stop;
    private boolean hasSleep;
    private CallBack<?> checkCodeCallBack;

    public PlayTask(PlayerInfo playerInfo, VideoInfo videoInfo, String baseUri) {
        this.playerInfo = playerInfo;
        this.videoInfo = videoInfo;
        this.baseUri = baseUri;
        this.playSecond = (int) (this.playerInfo.getAttachments()[0].getHeadOffset() / 1000);
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
            boolean isPassed;
            Map<QuestionConfig, OptionInfo> questions = getQuestions(playerInfo);
            while (true)
                try {
                    isPassed = CXUtil.onStart(playerInfo, videoInfo);
                    break;
                } catch (CheckCodeException e) {
                    if (checkCodeCallBack != null)
                        checkCodeCallBack.call(e.getUri(), e.getSession());
                }
            checkCodeCallBack.print(this.videoName + "[start]");
            if (!isPassed) {
                do {
                    if (hasSleep)
                        for (int i = 0; !stop && i < playerInfo.getDefaults().getReportTimeInterval(); i++)
                            Thread.sleep(1000);
                    if (stop)
                        break;
                    if (!pause) {
                        checkCodeCallBack.print(this.videoName + "[" + (int) ((float) this.playSecond / this.videoInfo.getDuration() * 100) + "%]");
                        playSecond += playerInfo.getDefaults().getReportTimeInterval();
                    }
                    if (playSecond > videoInfo.getDuration()) {
                        playSecond = videoInfo.getDuration();
                        break;
                    }
                    for (Map.Entry<QuestionConfig, OptionInfo> question : questions.entrySet())
                        if (playSecond >= question.getKey().getStartTime())
                            if (answerQuestion(question)) {
                                questions.remove(question.getKey());
                                System.out.println("answer success:" + question.getKey().getDescription() + "=" + question.getValue().getDescription());
                            }
                    while (true)
                        try {
                            isPassed = CXUtil.onPlayProgress(playerInfo, videoInfo, playSecond);
                            break;
                        } catch (CheckCodeException e) {
                            if (checkCodeCallBack != null)
                                checkCodeCallBack.call(e.getUri(), e.getSession());
                        }
                } while (pause || !isPassed);
                while (true)
                    try {
                        if (stop)
                            CXUtil.onPause(playerInfo, videoInfo, playSecond);
                        else
                            CXUtil.onEnd(playerInfo, videoInfo);
                        break;
                    } catch (CheckCodeException e) {
                        if (checkCodeCallBack != null)
                            checkCodeCallBack.call(e.getUri(), e.getSession());
                    }
                checkCodeCallBack.print(this.videoName + "[finish]");
            } else if (!questions.isEmpty())
                for (Map.Entry<QuestionConfig, OptionInfo> question : questions.entrySet())
                    if (answerQuestion(question)) {
                        questions.remove(question.getKey());
                        System.out.println("answer success:" + question.getKey().getDescription() + "=" + question.getValue().getDescription());
                    }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    private boolean answerQuestion(Map.Entry<QuestionConfig, OptionInfo> question) {
        boolean isPassed;
        while (true)
            try {
                isPassed = CXUtil.answerQuestion(baseUri, question.getKey().getValidationUrl(), question.getKey().getResourceId(), question.getValue().getName());
                break;
            } catch (CheckCodeException e) {
                if (checkCodeCallBack != null)
                    checkCodeCallBack.call(e.getUri(), e.getSession());
            }
        return isPassed;
    }

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setPlaySecond(int playSecond) {
        this.playSecond = playSecond;
    }

    private Map<QuestionConfig, OptionInfo> getQuestions(PlayerInfo playerInfo) {
        List<QuestionInfo> questionInfoList;
        while (true)
            try {
                questionInfoList = CXUtil.getQuestions(playerInfo.getDefaults().getInitdataUrl(), playerInfo.getAttachments()[0].getMid());
                break;
            } catch (CheckCodeException e) {
                this.checkCodeCallBack.call(e.getUri(), e.getSession());
            }
        Map<QuestionConfig, OptionInfo> questions = new HashMap<>();
        for (QuestionInfo questionInfo : questionInfoList)
            if (questionInfo.getStyle().equals("QUIZ"))
                for (QuestionConfig questionConfig : questionInfo.getDatas())
                    if (!questionConfig.isAnswered())
                        for (OptionInfo optionInfo : questionConfig.getOptions())
                            if (optionInfo.isRight()) {
                                questions.put(questionConfig, optionInfo);
                                break;
                            }
        return questions;
    }

    public void setCheckCodeCallBack(CallBack<?> checkCodeCallBack) {
        this.checkCodeCallBack = checkCodeCallBack;
    }
}

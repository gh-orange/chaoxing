package pers.cz.chaoxing.thread;

import pers.cz.chaoxing.common.*;
import pers.cz.chaoxing.util.ChaoxingUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlayTask implements Runnable {
    private PlayerInfo playerInfo;
    private VideoInfo videoInfo;
    private String baseUri;
    private int playSecond;
    private boolean pause;
    private boolean stop;
    private boolean hasSleep;

    public PlayTask(PlayerInfo playerInfo, VideoInfo videoInfo, String baseUri) {
        this.playerInfo = playerInfo;
        this.videoInfo = videoInfo;
        this.baseUri = baseUri;
        this.playSecond = (int) (this.playerInfo.getAttachments()[0].getHeadOffset() / 1000);
        this.stop = this.pause = false;
        this.hasSleep = true;
    }

    @Override
    public void run() {
        try {
            Map<QuestionConfig, OptionInfo> questions = getQuestions(playerInfo);
            if (!ChaoxingUtil.onStart(playerInfo, videoInfo)) {
                do {
                    if (hasSleep)
                        for (int i = 0; !stop && i < playerInfo.getDefaults().getReportTimeInterval(); i++)
                            Thread.sleep(1000);
                    if (stop)
                        break;
                    if (!pause)
                        playSecond += playerInfo.getDefaults().getReportTimeInterval();
                    if (playSecond > videoInfo.getDuration())
                        break;
                    for (Map.Entry<QuestionConfig, OptionInfo> question : questions.entrySet())
                        if (playSecond >= question.getKey().getStartTime())
                            if (ChaoxingUtil.answerQuestion(baseUri, question.getKey().getValidationUrl(), question.getKey().getResourceId(), question.getValue().getName())) {
                                questions.remove(question.getKey());
                                System.out.println("answer success:" + question.getKey().getDescription() + "=" + question.getValue().getDescription());
                            }
                } while (pause || !ChaoxingUtil.onPlayProgress(playerInfo, videoInfo, playSecond));
                if (!stop)
                    ChaoxingUtil.onEnd(playerInfo, videoInfo);
            } else if (!questions.isEmpty())
                for (Map.Entry<QuestionConfig, OptionInfo> question : questions.entrySet())
                    if (ChaoxingUtil.answerQuestion(baseUri, question.getKey().getValidationUrl(), question.getKey().getResourceId(), question.getValue().getName())) {
                        questions.remove(question.getKey());
                        System.out.println("answer success:" + question.getKey().getDescription() + "=" + question.getValue().getDescription());
                    }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
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

    public String getVideoName() {
        try {
            return URLDecoder.decode(videoInfo.getFilename(), "utf-8");
        } catch (UnsupportedEncodingException ignored) {
            return videoInfo.getFilename();
        }
    }

    public int getPlayedPercent() {
        return (int) ((float) this.playSecond / this.videoInfo.getDuration() * 100);
    }

    private Map<QuestionConfig, OptionInfo> getQuestions(PlayerInfo playerInfo) {
        List<QuestionInfo> questionInfoList = ChaoxingUtil.getQuestionInfos(playerInfo.getDefaults().getInitdataUrl(), playerInfo.getAttachments()[0].getMid());
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

}

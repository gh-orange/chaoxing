package pers.cz.chaoxing.thread;

import pers.cz.chaoxing.common.*;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.util.ChaoxingUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

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
            Scanner scanner = new Scanner(System.in);
            String checkCode;
            String checkCodePath = "./checkCode.jpeg";
            boolean isPassed;
            Map<QuestionConfig, OptionInfo> questions = getQuestions(playerInfo);
            while (true)
                try {
                    isPassed = ChaoxingUtil.onStart(playerInfo, videoInfo);
                    break;
                } catch (CheckCodeException e) {
                    e.saveCheckCode(checkCodePath);
                    if (ChaoxingUtil.openFile(checkCodePath))
                        System.out.println("CheckCode image path:" + checkCodePath);
                    System.out.print("Input checkCode:");
                    checkCode = scanner.nextLine();
                    e.setCheckCode(checkCode);
                }
            if (!isPassed) {
                do {
                    if (hasSleep)
                        for (int i = 0; !stop && i < playerInfo.getDefaults().getReportTimeInterval(); i++)
                            Thread.sleep(1000);
                    if (stop)
                        break;
                    if (!pause)
                        playSecond += playerInfo.getDefaults().getReportTimeInterval();
                    if (playSecond > videoInfo.getDuration()) {
                        playSecond = videoInfo.getDuration();
                        break;
                    }
                    for (Map.Entry<QuestionConfig, OptionInfo> question : questions.entrySet())
                        if (playSecond >= question.getKey().getStartTime())
                            if (ChaoxingUtil.answerQuestion(baseUri, question.getKey().getValidationUrl(), question.getKey().getResourceId(), question.getValue().getName())) {
                                questions.remove(question.getKey());
                                System.out.println("answer success:" + question.getKey().getDescription() + "=" + question.getValue().getDescription());
                            }
                    while (true)
                        try {
                            isPassed = ChaoxingUtil.onPlayProgress(playerInfo, videoInfo, playSecond);
                            break;
                        } catch (CheckCodeException e) {
                            e.saveCheckCode(checkCodePath);
                            if (ChaoxingUtil.openFile(checkCodePath))
                                System.out.println("CheckCode image path:" + checkCodePath);
                            System.out.print("Input checkCode:");
                            checkCode = scanner.nextLine();
                            e.setCheckCode(checkCode);
                        }
                } while (pause || !isPassed);
                if (!stop)
                    while (true)
                        try {
                            ChaoxingUtil.onEnd(playerInfo, videoInfo);
                            break;
                        } catch (CheckCodeException e) {
                            e.saveCheckCode(checkCodePath);
                            if (ChaoxingUtil.openFile(checkCodePath))
                                System.out.println("CheckCode image path:" + checkCodePath);
                            System.out.print("Input checkCode:");
                            checkCode = scanner.nextLine();
                            e.setCheckCode(checkCode);
                        }
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

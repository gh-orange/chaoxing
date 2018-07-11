package pers.cz.chaoxing.thread;

import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.common.quiz.HomeworkQuizInfo;
import pers.cz.chaoxing.common.quiz.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizConfig;
import pers.cz.chaoxing.common.task.HomeworkData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.CXUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class HomeworkTask implements Runnable, Callable<Boolean> {
    private final TaskInfo<HomeworkData> taskInfo;
    private final HomeworkQuizInfo homeworkQuizInfo;
    private final String baseUri;
    private String homeworkName;
    private boolean hasSleep;
    private CallBack<?> checkCodeCallBack;

    public HomeworkTask(TaskInfo<HomeworkData> taskInfo, HomeworkQuizInfo homeworkQuizInfo, String baseUri) {
        this.taskInfo = taskInfo;
        this.homeworkQuizInfo = homeworkQuizInfo;
        this.baseUri = baseUri;
        this.hasSleep = true;
        this.homeworkName = taskInfo.getAttachments()[0].getProperty().getTitle();
    }

    @Override
    public void run() {
        try {
            checkCodeCallBack.print(this.homeworkName + "[start]");
            Map<QuizConfig, List<OptionInfo>> answers = getAnswers(this.homeworkQuizInfo);
            if (answerQuestion(this.homeworkQuizInfo)) {
                for (Map.Entry<QuizConfig, List<OptionInfo>> quizConfigListEntry : answers.entrySet()) {
                    System.out.print("answer success:");
                    System.out.println(quizConfigListEntry.getKey().getDescription());
                    for (OptionInfo optionInfo : quizConfigListEntry.getValue())
                        System.out.println(optionInfo.getName() + "." + optionInfo.getDescription());
                }
                checkCodeCallBack.print(this.homeworkName + "[finish]");
            }
            if (hasSleep)
                Thread.sleep(10 * 60 * 1000);
        } catch (InterruptedException | WrongAccountException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public Boolean call() {
        run();
        return true;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setCheckCodeCallBack(CallBack<?> checkCodeCallBack) {
        this.checkCodeCallBack = checkCodeCallBack;
    }

    private Map<QuizConfig, List<OptionInfo>> getAnswers(HomeworkQuizInfo homeworkQuizInfo) {
        for (QuizConfig quizConfig : homeworkQuizInfo.getDatas())
            if (!CXUtil.getHomeworkAnswer(quizConfig)) {
                System.out.println("answer match failure:");
                System.out.println(quizConfig.getDescription());
                OptionInfo[] options = quizConfig.getOptions();
                for (int i = 0; i < options.length; i++) {
                    System.out.println(options[i].getName() + "." + options[i].getDescription());
                    if (i == 0 || !quizConfig.getQuestionType().equals("1"))
                        options[i].setRight(true);
                }
            }
        Map<QuizConfig, List<OptionInfo>> questions = new HashMap<>();
        for (QuizConfig quizConfig : homeworkQuizInfo.getDatas())
            if (!quizConfig.isAnswered())
                for (OptionInfo optionInfo : quizConfig.getOptions())
                    if (optionInfo.isRight())
                        if (!questions.containsKey(quizConfig)) {
                            questions.put(quizConfig, new ArrayList<>());
                            questions.get(quizConfig).add(optionInfo);
                        } else
                            questions.get(quizConfig).add(optionInfo);
        return questions;
    }

    private boolean answerQuestion(HomeworkQuizInfo homeworkQuizInfo) throws WrongAccountException {
        boolean isPassed;
        while (true)
            try {
                isPassed = CXUtil.answerHomeworkQuiz(baseUri, homeworkQuizInfo);
                break;
            } catch (CheckCodeException e) {
                if (checkCodeCallBack != null)
                    homeworkQuizInfo.setEnc(((CallBackData) checkCodeCallBack.call(e.getSession(), e.getUri(), taskInfo.getDefaults().getKnowledgeid(), homeworkQuizInfo.getClassId(), homeworkQuizInfo.getCourseId())).getEnc());
            }
        return isPassed;
    }
}

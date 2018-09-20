package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.common.quiz.ExamQuizInfo;
import pers.cz.chaoxing.common.quiz.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizConfig;
import pers.cz.chaoxing.common.task.data.exam.ExamData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.CXUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;

public class ExamTask implements Runnable, Callable<Boolean> {
    private Semaphore semaphore;
    private final TaskInfo<ExamData> taskInfo;
    private final ExamData attachment;
    private final ExamQuizInfo examQuizInfo;
    private final String baseUri;
    private String examName;
    private boolean hasFail;
    private boolean hasSleep;
    private boolean autoComplete;
    private CallBack<CallBackData> checkCodeCallBack;

    public ExamTask(TaskInfo<ExamData> taskInfo, ExamData attachment, ExamQuizInfo examQuizInfo, String baseUri) {
        this.taskInfo = taskInfo;
        this.attachment = attachment;
        this.examQuizInfo = examQuizInfo;
        this.baseUri = baseUri;
        this.hasFail = false;
        this.hasSleep = true;
        this.autoComplete = true;
        this.examName = this.attachment.getProperty().getTitle();
    }

    @Override
    public void run() {
        try {
            checkCodeCallBack.print(this.examName + "[exam start]");
            Map<QuizConfig, List<OptionInfo>> answers = getAnswers(this.examQuizInfo);
            acquire();
            if (hasFail) {
                if (storeQuestion(this.examQuizInfo))
                    for (Map.Entry<QuizConfig, List<OptionInfo>> quizConfigListEntry : answers.entrySet()) {
                        System.out.print("store success:");
                        System.out.println(quizConfigListEntry.getKey().getDescription());
                        for (OptionInfo optionInfo : quizConfigListEntry.getValue())
                            System.out.println(optionInfo.getName() + "." + optionInfo.getDescription());
                    }
            } else if (answerQuestion(this.examQuizInfo)) {
                for (Map.Entry<QuizConfig, List<OptionInfo>> quizConfigListEntry : answers.entrySet()) {
                    System.out.print("answer success:");
                    System.out.println(quizConfigListEntry.getKey().getDescription());
                    for (OptionInfo optionInfo : quizConfigListEntry.getValue())
                        System.out.println(optionInfo.getName() + "." + optionInfo.getDescription());
                }
            }
            if (hasSleep)
                Thread.sleep(3 * 1000);
            checkCodeCallBack.print(this.examName + "[exam finish]");
            release();
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
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

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }

    public void setCheckCodeCallBack(CallBack<CallBackData> checkCodeCallBack) {
        this.checkCodeCallBack = checkCodeCallBack;
    }

    private Map<QuizConfig, List<OptionInfo>> getAnswers(ExamQuizInfo examQuizInfo) {
        for (QuizConfig quizConfig : examQuizInfo.getDatas())
            if (null != quizConfig) {
                if (CXUtil.getQuizAnswer(quizConfig))
                    quizConfig.setAnswered(false);
                else {
                    System.out.println("answer match failure:");
                    System.out.println(quizConfig.getDescription());
                    OptionInfo[] options = quizConfig.getOptions();
                    for (int i = 0; i < options.length; i++) {
                        System.out.println(options[i].getName() + "." + options[i].getDescription());
                        if (autoComplete && !quizConfig.isAnswered())
                            if (i == 0 || !quizConfig.getQuestionType().equals("1"))
                                options[i].setRight(true);
                    }
                    if (!autoComplete)
                        hasFail = true;
                }
            }
        Map<QuizConfig, List<OptionInfo>> questions = new HashMap<>();
        for (QuizConfig quizConfig : examQuizInfo.getDatas())
            if (!quizConfig.isAnswered())
                for (OptionInfo optionInfo : quizConfig.getOptions())
                    if (optionInfo.isRight())
                        questions.computeIfAbsent(quizConfig, key -> new ArrayList<>()).add(optionInfo);
        return questions;
    }

    private boolean storeQuestion(ExamQuizInfo examQuizInfo) {
        boolean isPassed;
        while (true)
            try {
                isPassed = CXUtil.storeExamQuizzes(baseUri, examQuizInfo);
                break;
            } catch (CheckCodeException e) {
                if (checkCodeCallBack != null)
                    examQuizInfo.setEnc(checkCodeCallBack.call(e.getSession(), e.getUri(), taskInfo.getDefaults().getKnowledgeid(), examQuizInfo.getClassId(), examQuizInfo.getCourseId()).getEnc());
            }
        return isPassed;
    }

    private boolean answerQuestion(ExamQuizInfo examQuizInfo) {
        boolean isPassed;
        while (true)
            try {
                isPassed = CXUtil.answerExamQuizzes(baseUri, examQuizInfo);
                break;
            } catch (CheckCodeException e) {
                if (checkCodeCallBack != null)
                    examQuizInfo.setEnc(checkCodeCallBack.call(e.getSession(), e.getUri(), taskInfo.getDefaults().getKnowledgeid(), examQuizInfo.getClassId(), examQuizInfo.getCourseId()).getEnc());
            }
        return isPassed;
    }
}

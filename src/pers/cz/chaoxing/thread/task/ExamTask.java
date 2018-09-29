package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizConfig;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizData;
import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.task.data.exam.ExamTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExamTask extends Task<ExamTaskData> {
    public ExamTask(TaskInfo<ExamTaskData> taskInfo, ExamTaskData attachment, String baseUri) {
        super(taskInfo, attachment, baseUri);
        this.taskName = this.attachment.getProperty().getTitle();
    }

    @Override
    public void doTask() throws Exception {
        checkCodeCallBack.print(this.taskName + "[exam start]");
        QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo = loadQuizInfo(attachment);
        Try.ever(() -> CXUtil.getExamQuiz(baseUri, examQuizInfo), checkCodeCallBack);
        storeQuestion(examQuizInfo);
        IntStream.range(0, examQuizInfo.getDatas().length).forEach(i -> {
            examQuizInfo.getDefaults().setStart(i);
            Try.ever(() -> CXUtil.getExamQuiz(baseUri, examQuizInfo), checkCodeCallBack);
            Map<QuizData, List<OptionInfo>> answers = getAnswers(examQuizInfo);
            if (storeQuestion(examQuizInfo))
                answers.forEach((key, value) -> {
                    if (hasFail)
                        System.out.print("store success:");
                    else
                        System.out.print("answer success:");
                    System.out.println(key.getDescription());
                    value.forEach(optionInfo -> System.out.println(optionInfo.getName() + "." + optionInfo.getDescription()));
                });
        });
        if (!hasFail)
            answerQuestion(examQuizInfo);
        if (hasSleep)
            Thread.sleep(3 * 1000);
        checkCodeCallBack.print(this.taskName + "[exam finish]");
    }

    private QuizInfo<ExamQuizData, ExamQuizConfig> loadQuizInfo(ExamTaskData attachment) {
        QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo = new QuizInfo<>();
        examQuizInfo.getDefaults().setClassId(taskInfo.getDefaults().getClazzId());
        examQuizInfo.getDefaults().setCourseId(taskInfo.getDefaults().getCourseid());
        examQuizInfo.getDefaults().settId(attachment.getProperty().gettId());
        examQuizInfo.getDefaults().setTestUserRelationId(attachment.getProperty().getId());
        examQuizInfo.getDefaults().setExamsystem(attachment.getProperty().getExamsystem());
        examQuizInfo.getDefaults().setEnc(attachment.getEnc());
        return examQuizInfo;
    }

    protected Map<QuizData, List<OptionInfo>> getAnswers(QuizInfo quizInfo) {
        Map<QuizData, List<OptionInfo>> questions = new HashMap<>();
        QuizData quizData = quizInfo.getDatas()[((ExamQuizConfig) quizInfo.getDefaults()).getStart()];
        CXUtil.getQuizAnswer(quizData).forEach(questions.computeIfAbsent(quizData, key -> new ArrayList<>())::add);
        if (!questions.containsKey(quizData)) {
            System.out.println(taskName + " exam answer match failure:");
            System.out.println(quizData.getDescription());
            Arrays.stream(quizData.getOptions()).forEach(optionInfo -> System.out.println(optionInfo.getName() + "." + optionInfo.getDescription()));
            if (autoComplete)
                questions.put(quizData, autoCompleteAnswer(quizData));
            else
                hasFail = true;
        }
        if (questions.containsKey(quizData))
            quizData.setAnswered(false);
        else if (quizData.isAnswered())
            questions.put(quizData, Arrays.stream(quizData.getOptions()).filter(OptionInfo::isRight).collect(Collectors.toList()));
        return questions;
    }

    private boolean storeQuestion(QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo) {
        examQuizInfo.getDatas()[examQuizInfo.getDefaults().getStart()].setAnswered(Try.ever(() -> CXUtil.storeExamQuiz(examQuizInfo), checkCodeCallBack, examQuizInfo.getDefaults(), taskInfo.getDefaults().getKnowledgeid(), examQuizInfo.getDefaults().getClassId(), examQuizInfo.getDefaults().getCourseId()));
        return examQuizInfo.getDatas()[examQuizInfo.getDefaults().getStart()].isAnswered();
    }

    private boolean answerQuestion(QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo) {
        examQuizInfo.setPassed(Try.ever(() -> CXUtil.answerExamQuiz(examQuizInfo), checkCodeCallBack, examQuizInfo.getDefaults(), taskInfo.getDefaults().getKnowledgeid(), examQuizInfo.getDefaults().getClassId(), examQuizInfo.getDefaults().getCourseId()));
        return examQuizInfo.isPassed();
    }
}

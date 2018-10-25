package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.quiz.QuizInfo;
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

public class ExamTask extends Task<ExamTaskData, ExamQuizData> {
    private QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo;

    public ExamTask(TaskInfo<ExamTaskData> taskInfo, ExamTaskData attachment, String baseUri) {
        super(taskInfo, attachment, baseUri);
        this.taskName = this.attachment.getProperty().getTitle();
    }

    @Override
    public void doTask() throws Exception {
        checkCodeCallBack.print(this.taskName + "[exam start]");
        examQuizInfo = loadQuizInfo(attachment);
        Try.ever(() -> CXUtil.getExamQuiz(baseUri, examQuizInfo), checkCodeCallBack);
        Map<ExamQuizData, List<OptionInfo>> answers = new HashMap<>();
        ExamQuizData examQuizData = examQuizInfo.getDatas()[examQuizInfo.getDefaults().getStart()];
        answers.put(examQuizData, Arrays.stream(examQuizData.getOptions()).filter(OptionInfo::isRight).collect(Collectors.toList()));
        storeQuestion(answers);
        IntStream.range(0, examQuizInfo.getDatas().length).boxed().forEach(Try.once(i -> {
            examQuizInfo.getDefaults().setStart(i);
            Try.ever(() -> CXUtil.getExamQuiz(baseUri, examQuizInfo), checkCodeCallBack);
            if (this.isStopState())
                return;
            answers.clear();
            answers.putAll(getAnswers(examQuizInfo));
            if (storeQuestion(answers))
                answers.entrySet().stream()
                        .filter(entry -> !entry.getValue().isEmpty())
                        .forEach(entry -> checkCodeCallBack.print(
                                this.taskName + "[exam store success]",
                                entry.getKey().getDescription(),
                                entry.getValue().stream().map(optionInfo -> optionInfo.getName() + "." + optionInfo.getDescription()).toArray(String[]::new)));
        }));
        if (!hasFail)
            examQuizInfo.setPassed(answerQuestion(answers));
        if (hasSleep)
            Thread.sleep(3 * 1000);
        if (examQuizInfo.isPassed())
            checkCodeCallBack.print(this.taskName + "[exam answer finish]");
        else
            checkCodeCallBack.print(this.taskName + "[exam store finish]");
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

    @Override
    protected Map<ExamQuizData, List<OptionInfo>> getAnswers(QuizInfo<ExamQuizData, ?> quizInfo) {
        Map<ExamQuizData, List<OptionInfo>> questions = new HashMap<>();
        ExamQuizData quizData = quizInfo.getDatas()[((ExamQuizConfig) quizInfo.getDefaults()).getStart()];
        CXUtil.getQuizAnswer(quizData).forEach(optionInfo -> questions.computeIfAbsent(quizData, key -> new ArrayList<>()).add(optionInfo));
        if (!questions.containsKey(quizData)) {
            checkCodeCallBack.print(this.taskName + "[exam answer match failure]",
                    quizData.getDescription(),
                    Arrays.stream(quizData.getOptions()).map(optionInfo -> optionInfo.getName() + "." + optionInfo.getDescription()).toArray(String[]::new));
            if (autoComplete)
                questions.put(quizData, autoCompleteAnswer(quizData));
            else
                hasFail = true;
        }
        if (questions.containsKey(quizData))
            quizData.setAnswered(false);
        else
            questions.put(quizData, new ArrayList<>());
        return questions;
    }

    @Override
    protected boolean storeQuestion(Map<ExamQuizData, List<OptionInfo>> answers) {
        boolean isPassed = Try.ever(() -> CXUtil.storeExamQuiz(this.examQuizInfo.getDefaults(), answers), checkCodeCallBack, this.examQuizInfo.getDefaults(), taskInfo.getDefaults().getKnowledgeid(), this.examQuizInfo.getDefaults().getClassId(), this.examQuizInfo.getDefaults().getCourseId());
        if (isPassed)
            answers.keySet().forEach(examQuizData -> examQuizData.setAnswered(true));
        return isPassed;
    }

    @Override
    protected boolean answerQuestion(Map<ExamQuizData, List<OptionInfo>> answers) {
        boolean isPassed = Try.ever(() -> CXUtil.answerExamQuiz(this.examQuizInfo.getDefaults(), answers), checkCodeCallBack, this.examQuizInfo.getDefaults(), taskInfo.getDefaults().getKnowledgeid(), this.examQuizInfo.getDefaults().getClassId(), this.examQuizInfo.getDefaults().getCourseId());
        if (isPassed)
            answers.keySet().forEach(examQuizData -> examQuizData.setAnswered(true));
        return isPassed;
    }
}

package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizConfig;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.exam.ExamTaskData;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.StringUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ExamTask extends TaskModel<ExamTaskData, ExamQuizData> {
    private QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo;

    public ExamTask(TaskInfo<ExamTaskData> taskInfo, ExamTaskData attachment, String url) {
        super(taskInfo, attachment, url);
        this.taskName = this.attachment.getProperty().getTitle();
    }

    @Override
    public void doTask() throws Exception {
        threadPrintln("thread_exam_start", this.taskName);
        examQuizInfo = loadQuizInfo(attachment);
        Try.ever(() -> CXUtil.getExamQuiz(url, examQuizInfo), checkCodeCallBack);
        Map<ExamQuizData, List<OptionInfo>> answers = new HashMap<>();
        ExamQuizData examQuizData = examQuizInfo.getDatas()[examQuizInfo.getDefaults().getStart()];
        answers.put(examQuizData, Arrays.stream(examQuizData.getOptions()).filter(OptionInfo::isRight).collect(Collectors.toList()));
        storeQuestion(answers);
        IntStream.range(0, examQuizInfo.getDatas().length).boxed().forEach(Try.once(i -> {
            examQuizInfo.getDefaults().setStart(i);
            Try.ever(() -> CXUtil.getExamQuiz(url, examQuizInfo), checkCodeCallBack);
            control.checkState(this);
            answers.clear();
            answers.putAll(getAnswers(examQuizInfo));
            if (storeQuestion(answers))
                answers.entrySet().stream()
                        .filter(entry -> !entry.getValue().isEmpty())
                        .forEach(entry -> threadPrintln(
                                "thread_exam_store_success", new Object[]{this.taskName},
                                entry.getKey().getDescription(),
                                StringUtil.join(entry.getValue())
                        ));
        }));
        if (!hasFail)
            examQuizInfo.setPassed(answerQuestion(answers));
        if (control.isSleep())
            Thread.sleep(3 * 1000);
        threadPrintln(examQuizInfo.isPassed() ?
                        "thread_exam_answer_finish" : "thread_exam_store_finish",
                this.taskName);
    }

    private QuizInfo<ExamQuizData, ExamQuizConfig> loadQuizInfo(ExamTaskData attachment) {
        QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo = new QuizInfo<>();
        examQuizInfo.setDefaults(new ExamQuizConfig());
        examQuizInfo.getDefaults().setClassId(taskInfo.getDefaults().getClazzId());
        examQuizInfo.getDefaults().setCourseId(taskInfo.getDefaults().getCourseid());
        examQuizInfo.getDefaults().settId(attachment.getProperty().gettId());
        examQuizInfo.getDefaults().setTestUserRelationId(attachment.getProperty().getId());
        examQuizInfo.getDefaults().setExamsystem(attachment.getProperty().getExamsystem());
        examQuizInfo.getDefaults().setEnc(attachment.getEnc());
        examQuizInfo.getDefaults().setCpi(attachment.getProperty().getCpi());
        return examQuizInfo;
    }

    @Override
    protected Map<ExamQuizData, List<OptionInfo>> getAnswers(QuizInfo<ExamQuizData, ?> quizInfo) {
        Map<ExamQuizData, List<OptionInfo>> questions = new HashMap<>();
        ExamQuizData quizData = quizInfo.getDatas()[((ExamQuizConfig) quizInfo.getDefaults()).getStart()];
        CXUtil.getQuizAnswer(quizData).forEach(optionInfo -> questions.computeIfAbsent(quizData, key -> new ArrayList<>()).add(optionInfo));
        if (!questions.containsKey(quizData)) {
            threadPrintln("thread_exam_answer_failure", new Object[]{this.taskName},
                    quizData.toString());
            hasFail = !completeAnswer(questions, quizData);
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
        examQuizInfo.getDefaults().setTempSave(false);
        boolean isPassed = Try.ever(() -> CXUtil.answerExamQuiz(this.examQuizInfo.getDefaults(), answers), checkCodeCallBack, this.examQuizInfo.getDefaults(), taskInfo.getDefaults().getKnowledgeid(), this.examQuizInfo.getDefaults().getClassId(), this.examQuizInfo.getDefaults().getCourseId());
        if (isPassed)
            answers.keySet().forEach(examQuizData -> examQuizData.setAnswered(true));
        return isPassed;
    }
}

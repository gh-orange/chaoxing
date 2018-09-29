package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizConfig;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizData;
import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.task.data.homework.HomeworkTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;

import java.util.*;
import java.util.stream.Collectors;

public class HomeworkTask extends Task<HomeworkTaskData> {
    private final QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo;

    public HomeworkTask(TaskInfo<HomeworkTaskData> taskInfo, HomeworkTaskData attachment, QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo, String baseUri) {
        super(taskInfo, attachment, baseUri);
        this.homeworkQuizInfo = homeworkQuizInfo;
        this.taskName = this.attachment.getProperty().getTitle();
    }

    @Override
    public void doTask() throws Exception {
        checkCodeCallBack.print(this.taskName + "[homework start]");
        Map<QuizData, List<OptionInfo>> answers = getAnswers(this.homeworkQuizInfo);
        if (hasFail) {
            if (storeQuestion(this.homeworkQuizInfo))
                answers.forEach((key, value) -> {
                    System.out.print("store success:");
                    System.out.println(key.getDescription());
                    value.forEach(optionInfo -> System.out.println(optionInfo.getName() + "." + optionInfo.getDescription()));
                });
        } else if (answerQuestion(this.homeworkQuizInfo))
            answers.forEach((key, value) -> {
                System.out.print("answer success:");
                System.out.println(key.getDescription());
                value.forEach(optionInfo -> System.out.println(optionInfo.getName() + "." + optionInfo.getDescription()));
            });
        if (hasSleep)
            Thread.sleep(3 * 60 * 1000);
        checkCodeCallBack.print(this.taskName + "[homework finish]");
    }

    protected Map<QuizData, List<OptionInfo>> getAnswers(QuizInfo quizInfo) {
        Map<QuizData, List<OptionInfo>> questions = new HashMap<>();
        Arrays.stream(quizInfo.getDatas()).forEach(quizData -> {
            CXUtil.getQuizAnswer(quizData).forEach(questions.computeIfAbsent(quizData, key -> new ArrayList<>())::add);
            if (!questions.containsKey(quizData)) {
                System.out.println(taskName + " homework answer match failure:");
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
        });
        return questions;
    }

    private boolean storeQuestion(QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo) throws WrongAccountException {
        boolean isPassed = Try.ever(() -> CXUtil.storeHomeworkQuiz(baseUri, homeworkQuizInfo), checkCodeCallBack, homeworkQuizInfo.getDefaults(), taskInfo.getDefaults().getKnowledgeid(), homeworkQuizInfo.getDefaults().getClassId(), homeworkQuizInfo.getDefaults().getCourseId());
        if (isPassed)
            Arrays.stream(homeworkQuizInfo.getDatas()).forEach(homeworkQuizData -> homeworkQuizData.setAnswered(true));
        return isPassed;
    }

    private boolean answerQuestion(QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo) throws WrongAccountException {
        boolean isPassed = Try.ever(() -> CXUtil.answerHomeworkQuiz(baseUri, homeworkQuizInfo), checkCodeCallBack, homeworkQuizInfo.getDefaults(), taskInfo.getDefaults().getKnowledgeid(), homeworkQuizInfo.getDefaults().getClassId(), homeworkQuizInfo.getDefaults().getCourseId());
        if (isPassed)
            Arrays.stream(homeworkQuizInfo.getDatas()).forEach(homeworkQuizData -> homeworkQuizData.setAnswered(true));
        return isPassed;
    }
}

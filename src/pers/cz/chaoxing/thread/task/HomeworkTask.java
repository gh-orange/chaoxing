package pers.cz.chaoxing.thread.task;

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
        Map<HomeworkQuizData, List<OptionInfo>> answers = getAnswers(this.homeworkQuizInfo);
        if (hasFail) {
            if (storeQuestion(this.homeworkQuizInfo.getDefaults(), answers))
                answers.entrySet().stream()
                        .filter(entry -> !entry.getValue().isEmpty())
                        .forEach(entry -> {
                            System.out.print("store success:");
                            System.out.println(entry.getKey().getDescription());
                            entry.getValue().forEach(optionInfo -> System.out.println(optionInfo.getName() + "." + optionInfo.getDescription()));
                        });
        } else {
            this.homeworkQuizInfo.setPassed(answerQuestion(this.homeworkQuizInfo.getDefaults(), answers));
            if (this.homeworkQuizInfo.isPassed())
                answers.forEach((key, value) -> {
                    System.out.print("answer success:");
                    System.out.println(key.getDescription());
                    value.forEach(optionInfo -> System.out.println(optionInfo.getName() + "." + optionInfo.getDescription()));
                });
        }
        if (hasSleep)
            Thread.sleep(3 * 60 * 1000);
        if (this.homeworkQuizInfo.isPassed())
            checkCodeCallBack.print(this.taskName + "[homework answer finish]");
        else
            checkCodeCallBack.print(this.taskName + "[homework store finish]");
    }

    protected Map<HomeworkQuizData, List<OptionInfo>> getAnswers(QuizInfo quizInfo) {
        Map<HomeworkQuizData, List<OptionInfo>> questions = new HashMap<>();
        Arrays.stream(((HomeworkQuizData[]) quizInfo.getDatas())).forEach(quizData -> {
            CXUtil.getQuizAnswer(quizData).forEach(optionInfo -> questions.computeIfAbsent(quizData, key -> new ArrayList<>()).add(optionInfo));
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
            else
                questions.put(quizData, Arrays.stream(quizData.getOptions()).filter(OptionInfo::isRight).collect(Collectors.toList()));
        });
        return questions;
    }

    private boolean storeQuestion(HomeworkQuizConfig defaults, Map<HomeworkQuizData, List<OptionInfo>> answers) throws WrongAccountException {
        boolean isPassed = Try.ever(() -> CXUtil.storeHomeworkQuiz(baseUri, defaults, answers), checkCodeCallBack, homeworkQuizInfo.getDefaults(), taskInfo.getDefaults().getKnowledgeid(), homeworkQuizInfo.getDefaults().getClassId(), homeworkQuizInfo.getDefaults().getCourseId());
        if (isPassed)
            answers.keySet().forEach(homeworkQuizData -> homeworkQuizData.setAnswered(true));
        return isPassed;
    }

    private boolean answerQuestion(HomeworkQuizConfig defaults, Map<HomeworkQuizData, List<OptionInfo>> answers) throws WrongAccountException {
        boolean isPassed = Try.ever(() -> CXUtil.answerHomeworkQuiz(baseUri, defaults, answers), checkCodeCallBack, defaults, defaults.getKnowledgeid(), defaults.getClassId(), defaults.getCourseId());
        if (isPassed)
            answers.keySet().forEach(homeworkQuizData -> homeworkQuizData.setAnswered(true));
        return isPassed;
    }
}

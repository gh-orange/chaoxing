package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;

import java.util.List;

/**
 * different way to complete answer
 *
 * @author 橙子
 * @since 2018/11/24
 */
public interface CompleteAnswer {
    List<OptionInfo> autoCompleteAnswer(QuizData quizData);

    List<OptionInfo> manualCompleteAnswer(QuizData quizData);
}

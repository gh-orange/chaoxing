package pers.cz.chaoxing.common.quiz.data.homework;

import pers.cz.chaoxing.common.quiz.data.QuizData;

public class HomeworkQuizData extends QuizData {
    private String answerId;
    private String answerTypeId;
    private String answerCheckName;

    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    public String getAnswerTypeId() {
        return answerTypeId;
    }

    public void setAnswerTypeId(String answerTypeId) {
        this.answerTypeId = answerTypeId;
    }

    public String getAnswerCheckName() {
        return answerCheckName;
    }

    public void setAnswerCheckName(String answerCheckName) {
        this.answerCheckName = answerCheckName;
    }
}

package pers.cz.chaoxing.common.quiz.data.exam;

import pers.cz.chaoxing.common.quiz.data.QuizData;

public class ExamQuizData extends QuizData {
    private String questionId;
    private String testPaperId;
    private String paperId;
    private String subCount;
    private String questionScore;
    private boolean randomOptions;

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getTestPaperId() {
        return testPaperId;
    }

    public void setTestPaperId(String testPaperId) {
        this.testPaperId = testPaperId;
    }

    public String getPaperId() {
        return paperId;
    }

    public void setPaperId(String paperId) {
        this.paperId = paperId;
    }

    public String getSubCount() {
        return subCount;
    }

    public void setSubCount(String subCount) {
        this.subCount = subCount;
    }

    public String getQuestionScore() {
        return questionScore;
    }

    public void setQuestionScore(String questionScore) {
        this.questionScore = questionScore;
    }

    public boolean isRandomOptions() {
        return randomOptions;
    }

    public void setRandomOptions(boolean randomOptions) {
        this.randomOptions = randomOptions;
    }
}

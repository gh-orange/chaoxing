package pers.cz.chaoxing.common.quiz.data;

import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.util.io.StringUtil;

/**
 * @author 橙子
 * @since 2018/9/23
 */
public abstract class QuizData implements Cloneable {
    private boolean answered;
    private String description;
    private String questionType;
    private String validationUrl;
    private OptionInfo[] options;

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getValidationUrl() {
        return validationUrl;
    }

    public void setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
    }

    public OptionInfo[] getOptions() {
        return options;
    }

    public void setOptions(OptionInfo[] options) {
        this.options = options;
    }

    @Override
    public String toString() {
        return description + '\n' + StringUtil.join(options);
    }

    @Override
    public QuizData clone() {
        try {
            return (QuizData) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }
}

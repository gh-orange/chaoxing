package pers.cz.chaoxing.common.quiz.data.player;

import pers.cz.chaoxing.common.quiz.data.QuizData;

public class PlayerQuizData extends QuizData {
    private String resourceId;
    private String memberinfo;
    private String validationUrl;
    private String errorReportUrl;
    private long startTime;
    private long endTime;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public String getMemberinfo() {
        return memberinfo;
    }

    public void setMemberinfo(String memberinfo) {
        this.memberinfo = memberinfo;
    }

    public String getValidationUrl() {
        return validationUrl;
    }

    public void setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
    }

    public String getErrorReportUrl() {
        return errorReportUrl;
    }

    public void setErrorReportUrl(String errorReportUrl) {
        this.errorReportUrl = errorReportUrl;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
}

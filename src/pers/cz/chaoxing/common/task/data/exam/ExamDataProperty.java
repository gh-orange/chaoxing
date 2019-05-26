package pers.cz.chaoxing.common.task.data.exam;

import pers.cz.chaoxing.common.task.data.TaskDataProperty;

public class ExamDataProperty extends TaskDataProperty {
    private String id;
    private String tId;
    private String title;
    private String endTime;
    private String moocTeacherId;
    private String examsystem;
    private String examEnc;
    private String cpi;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String gettId() {
        return tId;
    }

    public void settId(String tId) {
        this.tId = tId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getMoocTeacherId() {
        return moocTeacherId;
    }

    public void setMoocTeacherId(String moocTeacherId) {
        this.moocTeacherId = moocTeacherId;
    }

    public String getExamsystem() {
        return examsystem;
    }

    public void setExamsystem(String examsystem) {
        this.examsystem = examsystem;
    }

    public String getExamEnc() {
        return examEnc;
    }

    public void setExamEnc(String examEnc) {
        this.examEnc = examEnc;
    }

    public String getCpi() {
        return cpi;
    }

    public void setCpi(String cpi) {
        this.cpi = cpi;
    }
}

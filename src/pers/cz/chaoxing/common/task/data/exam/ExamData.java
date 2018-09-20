package pers.cz.chaoxing.common.task.data.exam;

import pers.cz.chaoxing.common.task.data.TaskData;

public class ExamData extends TaskData<ExamDataProperty> {
    private String enc;
    private boolean isPassed;

    public String getEnc() {
        return enc;
    }

    public void setEnc(String enc) {
        this.enc = enc;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }
}

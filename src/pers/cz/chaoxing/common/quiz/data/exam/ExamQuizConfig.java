package pers.cz.chaoxing.common.quiz.data.exam;

/**
 * @author 橙子
 * @date 2018/9/23
 */
public class ExamQuizConfig {
    private String userId;
    private String classId;
    private String courseId;
    private String tId;
    private String testUserRelationId;
    private String examsystem;
    private String enc;
    private boolean tempSave;
    private boolean timeOver;
    private int start;
    private int remainTime;
    private int encRemainTime;
    private long encLastUpdateTime;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String gettId() {
        return tId;
    }

    public void settId(String tId) {
        this.tId = tId;
    }

    public String getTestUserRelationId() {
        return testUserRelationId;
    }

    public void setTestUserRelationId(String testUserRelationId) {
        this.testUserRelationId = testUserRelationId;
    }

    public String getExamsystem() {
        return examsystem;
    }

    public void setExamsystem(String examsystem) {
        this.examsystem = examsystem;
    }

    public String getEnc() {
        return enc;
    }

    public void setEnc(String enc) {
        this.enc = enc;
    }

    public boolean isTempSave() {
        return tempSave;
    }

    public void setTempSave(boolean tempSave) {
        this.tempSave = tempSave;
    }

    public boolean isTimeOver() {
        return timeOver;
    }

    public void setTimeOver(boolean timeOver) {
        this.timeOver = timeOver;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getRemainTime() {
        return remainTime;
    }

    public void setRemainTime(int remainTime) {
        this.remainTime = remainTime;
    }

    public int getEncRemainTime() {
        return encRemainTime;
    }

    public void setEncRemainTime(int encRemainTime) {
        this.encRemainTime = encRemainTime;
    }

    public long getEncLastUpdateTime() {
        return encLastUpdateTime;
    }

    public void setEncLastUpdateTime(long encLastUpdateTime) {
        this.encLastUpdateTime = encLastUpdateTime;
    }
}

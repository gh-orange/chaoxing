package pers.cz.chaoxing.common.quiz;

public class ExamQuizInfo {
    private QuizConfig[] datas;
    private String courseId;
    private String classId;
    private String tId;
    private String testUserRelationId;
    private String testPaperId;
    private String paperId;
    private String subCount;
    private String examsystem;
    private String enc;
    private String userId;
    private boolean tempSave;
    private boolean timeOver;
    private boolean randomOptions;
    private int remainTime;
    private int encRemainTime;
    private long encLastUpdateTime;

    public QuizConfig[] getDatas() {
        return datas;
    }

    public void setDatas(QuizConfig[] datas) {
        this.datas = datas;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getClassId() {
        return classId;
    }

    public void setClassId(String classId) {
        this.classId = classId;
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

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public boolean isRandomOptions() {
        return randomOptions;
    }

    public void setRandomOptions(boolean randomOptions) {
        this.randomOptions = randomOptions;
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

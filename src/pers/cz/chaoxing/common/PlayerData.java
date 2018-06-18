package pers.cz.chaoxing.common;

public class PlayerData {
    private long headOffset;
    private String jobid;
    private String otherInfo;
    private boolean isPassed;
    private PlayerDataProperty property;
    private long playTime;
    private String mid;
    private boolean job;
    private String type;
    private String objectId;

    public long getHeadOffset() {
        return headOffset;
    }

    public void setHeadOffset(long headOffset) {
        this.headOffset = headOffset;
    }

    public String getJobid() {
        return jobid;
    }

    public void setJobid(String jobid) {
        this.jobid = jobid;
    }

    public String getOtherInfo() {
        return otherInfo;
    }

    public void setOtherInfo(String otherInfo) {
        this.otherInfo = otherInfo;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }

    public PlayerDataProperty getProperty() {
        return property;
    }

    public void setProperty(PlayerDataProperty property) {
        this.property = property;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public boolean isJob() {
        return job;
    }

    public void setJob(boolean job) {
        this.job = job;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public long getPlayTime() {
        return playTime;
    }

    public void setPlayTime(long playTime) {
        this.playTime = playTime;
    }
}

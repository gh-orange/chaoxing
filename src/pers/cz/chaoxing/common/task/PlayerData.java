package pers.cz.chaoxing.common.task;

public class PlayerData extends TaskData<PlayerDataProperty> {
    private long headOffset;
    private boolean isPassed;
    private long playTime;
    private boolean job;
    private String objectId;

    public long getHeadOffset() {
        return headOffset;
    }

    public void setHeadOffset(long headOffset) {
        this.headOffset = headOffset;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }

    public boolean isJob() {
        return job;
    }

    public void setJob(boolean job) {
        this.job = job;
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

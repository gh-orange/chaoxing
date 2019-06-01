package pers.cz.chaoxing.common;

/**
 * @author 橙子
 * @since 2019/5/31
 */
public class ReadInfo {
    private int headOffset;
    private int duration;
    private int height;
    private String from;
    private String courseId;

    public int getHeadOffset() {
        return headOffset;
    }

    public void setHeadOffset(int headOffset) {
        this.headOffset = headOffset;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
}

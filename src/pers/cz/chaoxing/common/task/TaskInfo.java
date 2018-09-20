package pers.cz.chaoxing.common.task;

import pers.cz.chaoxing.common.task.data.TaskData;

public class TaskInfo<T extends TaskData> {
    private T[] attachments;
    private TaskConfig defaults;
    private boolean control;

    public T[] getAttachments() {
        return attachments;
    }

    public void setAttachments(T[] attachments) {
        this.attachments = attachments;
    }

    public TaskConfig getDefaults() {
        return defaults;
    }

    public void setDefaults(TaskConfig defaults) {
        this.defaults = defaults;
    }

    public boolean isControl() {
        return control;
    }

    public void setControl(boolean control) {
        this.control = control;
    }
}

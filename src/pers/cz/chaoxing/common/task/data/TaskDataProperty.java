package pers.cz.chaoxing.common.task.data;

import com.alibaba.fastjson.annotation.JSONField;

public abstract class TaskDataProperty {
    private String jobid;
    private String module;
    private String mid;
    private String _jobid;

    public String getJobid() {
        return jobid;
    }

    public void setJobid(String jobid) {
        this.jobid = jobid;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String get_jobid() {
        return _jobid;
    }

    @JSONField(name = "_jobid")
    public void set_jobid(String _jobid) {
        this._jobid = _jobid;
    }
}

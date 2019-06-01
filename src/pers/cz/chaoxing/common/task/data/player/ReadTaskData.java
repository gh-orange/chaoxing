package pers.cz.chaoxing.common.task.data.player;

import pers.cz.chaoxing.common.task.data.TaskData;

/**
 * @author 橙子
 * @since 2019/5/31
 */
public class ReadTaskData extends TaskData<ReadDataProperty> {
    private String enc;
    private String jtoken;

    public String getEnc() {
        return enc;
    }

    public void setEnc(String enc) {
        this.enc = enc;
    }

    public String getJtoken() {
        return jtoken;
    }

    public void setJtoken(String jtoken) {
        this.jtoken = jtoken;
    }
}

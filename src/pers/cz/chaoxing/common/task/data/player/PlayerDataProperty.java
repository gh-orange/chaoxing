package pers.cz.chaoxing.common.task.data.player;

import pers.cz.chaoxing.common.task.data.TaskDataProperty;

public class PlayerDataProperty extends TaskDataProperty {
    private boolean switchwindow;
    private long size;
    private boolean fastforward;
    private String hsize;
    private String name;
    private String type;
    private String objectid;

    public boolean isSwitchwindow() {
        return switchwindow;
    }

    public void setSwitchwindow(boolean switchwindow) {
        this.switchwindow = switchwindow;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public boolean isFastforward() {
        return fastforward;
    }

    public void setFastforward(boolean fastforward) {
        this.fastforward = fastforward;
    }

    public String getHsize() {
        return hsize;
    }

    public void setHsize(String hsize) {
        this.hsize = hsize;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getObjectid() {
        return objectid;
    }

    public void setObjectid(String objectid) {
        this.objectid = objectid;
    }

}

package pers.cz.chaoxing.common.other;

/**
 * @author 橙子
 * @date 2018/10/26
 */
public class SchoolData {
    private String name;
    private String domain;
    private int id;
    private int pid;
    private int dxfid;
    private boolean allowJoin;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getDxfid() {
        return dxfid;
    }

    public void setDxfid(int dxfid) {
        this.dxfid = dxfid;
    }

    public boolean isAllowJoin() {
        return allowJoin;
    }

    public void setAllowJoin(boolean allowJoin) {
        this.allowJoin = allowJoin;
    }
}

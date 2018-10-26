package pers.cz.chaoxing.common.other;

/**
 * @author 橙子
 * @date 2018/10/26
 */
public class SchoolInfo {
    private boolean result;
    private int fromNums;
    private SchoolData[] froms;

    public SchoolInfo() {
    }

    public SchoolInfo(boolean result) {
        this.result = result;
    }

    public boolean isResult() {
        return result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public int getFromNums() {
        return fromNums;
    }

    public void setFromNums(int fromNums) {
        this.fromNums = fromNums;
    }

    public SchoolData[] getFroms() {
        return froms;
    }

    public void setFroms(SchoolData[] froms) {
        this.froms = froms;
    }
}

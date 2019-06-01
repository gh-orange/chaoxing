package pers.cz.chaoxing.common.task;

/**
 * @author 橙子
 * @since 2019/6/1
 */
public class ReadCardInfo implements Comparable<Integer> {
    private String knowledgeId;
    private int start;
    private int height;
    private String nextKnowledgeId;

    public ReadCardInfo(String knowledgeId, int start, int height, String nextKnowledgeId) {
        this.knowledgeId = knowledgeId;
        this.start = start;
        this.height = height;
        this.nextKnowledgeId = nextKnowledgeId;
    }

    public String getKnowledgeId() {
        return knowledgeId;
    }

    public void setKnowledgeId(String knowledgeId) {
        this.knowledgeId = knowledgeId;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getNextKnowledgeId() {
        return nextKnowledgeId;
    }

    public void setNextKnowledgeId(String nextKnowledgeId) {
        this.nextKnowledgeId = nextKnowledgeId;
    }

    @Override
    public int compareTo(Integer height) {
        if (this.start > height)
            return 1;
        if (this.start + this.height < height)
            return -1;
        return 0;
    }
}

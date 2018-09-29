package pers.cz.chaoxing.common;

public class OptionInfo {
    private boolean isRight;
    private String name;
    private String description;

    public OptionInfo() {
    }

    public OptionInfo(OptionInfo optionInfo) {
        this.isRight = optionInfo.isRight;
        this.name = optionInfo.name;
        this.description = optionInfo.description;
    }

    public boolean isRight() {
        return isRight;
    }

    public void setRight(boolean right) {
        isRight = right;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}

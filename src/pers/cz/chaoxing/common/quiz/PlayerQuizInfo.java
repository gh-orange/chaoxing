package pers.cz.chaoxing.common.quiz;

public class PlayerQuizInfo {
    private QuizConfig[] datas;
    private String style;

    public QuizConfig[] getDatas() {
        return datas;
    }

    public void setDatas(QuizConfig[] datas) {
        this.datas = datas;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }
}

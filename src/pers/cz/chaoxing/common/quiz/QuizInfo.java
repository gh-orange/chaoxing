package pers.cz.chaoxing.common.quiz;

import pers.cz.chaoxing.common.quiz.data.QuizData;

/**
 * @author 橙子
 * @date 2018/9/23
 */
public class QuizInfo<T extends QuizData, V> {
    private T[] datas;
    private V defaults;
    private String style;
    private boolean isPassed;

    public T[] getDatas() {
        return datas;
    }

    public void setDatas(T[] datas) {
        this.datas = datas;
    }

    public V getDefaults() {
        return defaults;
    }

    public void setDefaults(V defaults) {
        this.defaults = defaults;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public boolean isPassed() {
        return isPassed;
    }

    public void setPassed(boolean passed) {
        isPassed = passed;
    }
}

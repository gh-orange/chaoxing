package pers.cz.chaoxing.exception;

/**
 * @author 橙子
 * @since 2019/5/30
 */
public class ExamStateException extends Exception {
    private int finishStandard;

    public ExamStateException(int finishStandard) {
        this.finishStandard = finishStandard;
    }

    public int getFinishStandard() {
        return finishStandard;
    }
}

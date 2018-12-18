package pers.cz.chaoxing.callback;

/**
 * thread pause callback
 *
 * @author 橙子
 * @since 2018/12/18
 */
public interface PauseCallBack {
    default void beforePause() {
    }

    default void afterPause() {
    }
}

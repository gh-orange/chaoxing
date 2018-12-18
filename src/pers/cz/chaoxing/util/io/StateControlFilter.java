package pers.cz.chaoxing.util.io;

import pers.cz.chaoxing.thread.PauseThreadPoolExecutor;

/**
 * @author 橙子
 * @since 2018/12/17
 */
public class StateControlFilter implements InputFilter {
    private PauseThreadPoolExecutor pauseThreadPool;

    public StateControlFilter(PauseThreadPoolExecutor pauseThreadPool) {
        this.pauseThreadPool = pauseThreadPool;
    }

    @Override
    public boolean doFilter(String input) {
        switch (input) {
            case "p":
            case "P":
                pauseThreadPool.pauseNow();
                return true;
            case "s":
            case "S":
                pauseThreadPool.shutdownNow();
                return true;
            default:
                if (!pauseThreadPool.isPaused())
                    break;
                pauseThreadPool.resume();
                return true;
        }
        return false;
    }
}

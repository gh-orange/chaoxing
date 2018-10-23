package pers.cz.chaoxing.thread.task;

import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.TaskData;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.Try;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * @author 橙子
 * @date 2018/9/25
 */
public abstract class Task<T extends TaskData> implements Runnable, Callable<Boolean> {
    protected final String baseUri;
    final TaskInfo<T> taskInfo;
    final T attachment;
    String taskName;
    boolean stop;
    boolean pause;
    boolean hasFail;
    boolean hasSleep;
    boolean autoComplete;
    private Semaphore semaphore;
    CallBack<?> checkCodeCallBack;

    Task(TaskInfo<T> taskInfo, T attachment, String baseUri) {
        this.taskInfo = taskInfo;
        this.attachment = attachment;
        this.baseUri = baseUri;
        this.hasSleep = this.autoComplete = true;
        this.hasFail = this.stop = this.pause = false;
    }

    @Override
    public final void run() {
        try {
            acquire();
            try {
                doTask();
            } catch (InterruptedException | WrongAccountException e) {
                System.out.println(e.getMessage());
            } catch (Exception ignored) {
            }
            release();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public final Boolean call() {
        run();
        return true;
    }

    protected abstract void doTask() throws Exception;

    public void setStop(boolean stop) {
        this.stop = stop;
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setAutoComplete(boolean autoComplete) {
        this.autoComplete = autoComplete;
    }

    public void setSemaphore(Semaphore semaphore) {
        this.semaphore = semaphore;
    }

    public void setCheckCodeCallBack(CallBack<?> checkCodeCallBack) {
        this.checkCodeCallBack = checkCodeCallBack;
    }

    private void acquire() throws InterruptedException {
        Optional.ofNullable(semaphore).ifPresent(Try.once(Semaphore::acquire));
    }

    private void release() {
        Optional.ofNullable(semaphore).ifPresent(Semaphore::release);
    }

    protected abstract Map<? extends QuizData, List<OptionInfo>> getAnswers(QuizInfo quizInfo);

    List<OptionInfo> autoCompleteAnswer(QuizData quizData) {
        ArrayList<OptionInfo> options = new ArrayList<>();
        if (quizData.getQuestionType().equals("1")) {
            List<OptionInfo> rightOptions = Arrays.stream(quizData.getOptions()).map(OptionInfo::new).collect(Collectors.toList());
            rightOptions.forEach(optionInfo -> optionInfo.setRight(true));
            return rightOptions;
        } else if (0 != quizData.getOptions().length) {
            OptionInfo optionInfo = new OptionInfo(quizData.getOptions()[0]);
            optionInfo.setRight(true);
            options.add(optionInfo);
        }
        return options;
    }
}

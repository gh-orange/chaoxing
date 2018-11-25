package pers.cz.chaoxing.thread.task;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.TaskData;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.*;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * @author 橙子
 * @since 2018/9/25
 */
public abstract class TaskModel<T extends TaskData, V extends QuizData> implements Runnable, CompleteAnswer, Callable<Boolean> {
    protected final String baseUri;
    final TaskInfo<T> taskInfo;
    final T attachment;
    String taskName;
    TaskState taskState;
    boolean hasFail;
    boolean hasSleep;
    CompleteStyle completeStyle;
    private Semaphore semaphore;
    CallBack<?> checkCodeCallBack;

    TaskModel(TaskInfo<T> taskInfo, T attachment, String baseUri) {
        this.taskInfo = taskInfo;
        this.attachment = attachment;
        this.baseUri = baseUri;
        this.hasSleep = true;
        this.taskState = TaskState.RUNNING;
        this.completeStyle = CompleteStyle.AUTO;
    }

    @Override
    public final void run() {
        try {
            acquire();
            try {
                doTask();
            } catch (RequestsException e) {
                String message = StringUtil.subStringAfterFirst(e.getLocalizedMessage(), ":").trim();
                IOUtil.println("Net connection error: " + message);
            } catch (InterruptedException | WrongAccountException e) {
                IOUtil.println(e.getLocalizedMessage());
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

    public void setTaskState(TaskState taskState) {
        this.taskState = taskState;
    }

    public void setHasSleep(boolean hasSleep) {
        this.hasSleep = hasSleep;
    }

    public void setCompleteStyle(CompleteStyle completeStyle) {
        this.completeStyle = completeStyle;
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

    protected abstract Map<? extends QuizData, List<OptionInfo>> getAnswers(QuizInfo<V, ?> quizInfo);

    protected abstract boolean storeQuestion(Map<V, List<OptionInfo>> answers) throws Exception;

    protected abstract boolean answerQuestion(Map<V, List<OptionInfo>> answers) throws Exception;

    @Override
    public List<OptionInfo> autoCompleteAnswer(QuizData quizData) {
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

    @Override
    public List<OptionInfo> manualCompleteAnswer(QuizData quizData) {
        ArrayList<OptionInfo> options = new ArrayList<>();
        IOUtil.next().chars()
                .mapToObj(i -> Character.toString((char) i))
                .forEach(c -> {
                    List<OptionInfo> rightOptions = Arrays.stream(quizData.getOptions())
                            .filter(optionInfo -> optionInfo.getName().equals(c))
                            .map(OptionInfo::new)
                            .collect(Collectors.toList());
                    rightOptions.forEach(optionInfo -> optionInfo.setRight(true));
                    if (quizData.getQuestionType().equals("1"))
                        options.addAll(rightOptions);
                    else if (!rightOptions.isEmpty() && options.isEmpty())
                        options.add(rightOptions.get(0));
                });
        return options;
    }

    boolean isStopState() throws InterruptedException {
        while (true)
            switch (taskState) {
                case PAUSE:
                    Thread.sleep(60 * 1000);
                    break;
                case STOP:
                    return true;
                default:
                    return false;
            }
    }

    void threadPrintln(String first, String... more) {
        IOUtil.println(Thread.currentThread().getName() + ": " + first, more);
    }
}

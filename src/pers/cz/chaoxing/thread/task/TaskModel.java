package pers.cz.chaoxing.thread.task;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.checkcode.CheckCodeCallBack;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.control.Control;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.TaskData;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.callback.PauseCallBack;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.io.StringUtil;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author 橙子
 * @since 2018/9/25
 */
public abstract class TaskModel<T extends TaskData, K extends QuizData> implements Runnable, CompleteAnswer, PauseCallBack, Callable<Boolean> {
    final String url;
    final TaskInfo<T> taskInfo;
    final T attachment;
    String taskName;
    boolean hasFail;
    Control control;
    CheckCodeCallBack<?> checkCodeCallBack;
    private Thread refreshTask;

    TaskModel(TaskInfo<T> taskInfo, T attachment, String url) {
        this.url = url;
        this.taskInfo = taskInfo;
        this.attachment = attachment;
    }

    @Override
    public final void run() {
        call();
    }

    @Override
    public final Boolean call() {
        try {
            control.acquire();
            try {
                doTask();
                return true;
            } finally {
                control.release();
            }
        } catch (RequestsException e) {
            String message = StringUtil.subStringAfterFirst(e.getLocalizedMessage(), ":").trim();
            IOUtil.println("Net connection error: " + message);
        } catch (WrongAccountException e) {
            Optional.ofNullable(e.getLocalizedMessage()).ifPresent(IOUtil::println);
        } catch (Exception ignored) {
        }
        Optional.ofNullable(refreshTask).ifPresent(Thread::interrupt);
        return false;
    }

    protected abstract void doTask() throws Exception;

    public void setControl(Control control) {
        this.control = control;
    }

    public void setCheckCodeCallBack(CheckCodeCallBack<?> checkCodeCallBack) {
        this.checkCodeCallBack = checkCodeCallBack;
    }

    protected abstract Map<K, List<OptionInfo>> getAnswers(QuizInfo<K, ?> quizInfo);

    protected abstract boolean storeQuestion(Map<K, List<OptionInfo>> answers) throws Exception;

    protected abstract boolean answerQuestion(Map<K, List<OptionInfo>> answers) throws Exception;

    <S extends QuizData> boolean completeAnswer(Map<S, List<OptionInfo>> questions, S quizData) {
        switch (control.getCompleteStyle()) {
            case AUTO:
                questions.put(quizData, autoCompleteAnswer(quizData));
                break;
            case MANUAL:
                questions.put(quizData, manualCompleteAnswer(quizData));
                break;
            case NONE:
            default:
                return false;
        }
        return true;
    }

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
        do {
            List<String> answers = IOUtil.printAndNextLine("Input answers (such as C or ABC):").replaceAll("\\s", "").chars()
                    .mapToObj(i -> Character.toString((char) i))
                    .collect(Collectors.toList());
            for (OptionInfo option : quizData.getOptions()) {
                if (answers.stream().anyMatch(answer -> answer.equalsIgnoreCase(option.getName()))) {
                    options.add(new OptionInfo(option));
                    if (!quizData.getQuestionType().equals("1"))
                        break;
                }
            }
        } while (options.isEmpty());
        return options;
    }

    void startRefreshTask() {
        refreshTask = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(6000);
                    Try.ever(() -> CXUtil.refreshMenu(url, taskInfo.getDefaults().getClazzId(), taskInfo.getDefaults().getCourseid(), taskInfo.getDefaults().getChapterId()), CheckCodeFactory.CUSTOM.get());
                }
            } catch (InterruptedException ignored) {
            }
        });
    }

    void threadPrintln(String first, String... more) {
        IOUtil.println(Thread.currentThread().getName() + ": " + first, more);
    }
}

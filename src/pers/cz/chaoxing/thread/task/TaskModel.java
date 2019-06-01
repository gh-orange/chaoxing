package pers.cz.chaoxing.thread.task;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.PauseCallBack;
import pers.cz.chaoxing.callback.checkcode.CheckCodeCallBack;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.control.Control;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.TaskData;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.io.StringUtil;

import java.util.*;
import java.util.concurrent.Callable;
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
            IOUtil.println("exception_network", StringUtil.subStringAfterFirst(e.getLocalizedMessage(), ":").trim());
        } catch (WrongAccountException e) {
            IOUtil.println("exception_account", e.getLocalizedMessage());
        } catch (Exception ignored) {
        }
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
            String rightAnswerStr = IOUtil.printAndNextLine("input_answer", quizData.getOptions().length == 1 || !quizData.getQuestionType().equals("1") ? quizData.getOptions()[0].getName() : quizData.getOptions()[0].getName() + quizData.getOptions()[quizData.getOptions().length - 1].getName()).replaceAll("\\s", "");
            if (rightAnswerStr.matches("(?i)[√✓✔对是]|正确|T(RUE)?|Y(ES)?|RIGHT|CORRECT"))
                rightAnswerStr = "true";
            else if (rightAnswerStr.matches("(?i)[X×✖错否]|错误|F(ALSE)?|N(O)?|WRONG|INCORRECT"))
                rightAnswerStr = "false";
            else {
                List<String> answers = rightAnswerStr.chars()
                        .mapToObj(i -> Character.toString((char) i))
                        .collect(Collectors.toList());
                for (OptionInfo option : quizData.getOptions()) {
                    if (answers.stream().anyMatch(answer -> answer.equalsIgnoreCase(option.getName()))) {
                        options.add(new OptionInfo(option));
                        if (!quizData.getQuestionType().equals("1"))
                            break;
                    }
                }
            }
            if (options.isEmpty())
                for (OptionInfo option : quizData.getOptions())
                    if (rightAnswerStr.equalsIgnoreCase(option.getName())) {
                        options.add(new OptionInfo(option));
                        break;
                    }
        } while (options.isEmpty());
        return options;
    }

    void startRefreshTask() {
        new Thread(() -> {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException ignored) {
            }
            Try.ever(() -> CXUtil.getListInfo(url, taskInfo.getDefaults().getClazzId(), taskInfo.getDefaults().getCourseid(), taskInfo.getDefaults().getChapterId()), CheckCodeFactory.CUSTOM.get());
        }).start();
    }

    void threadPrintln(String key, Object... args) {
        threadPrintln(key, args, new String[0]);
    }

    void threadPrintln(String key, Object[] args, String... lines) {
        Object[] newArgs = new Object[args.length + 1];
        newArgs[0] = Thread.currentThread().getName();
        System.arraycopy(args, 0, newArgs, 1, args.length);
        IOUtil.println(key, newArgs, lines);
    }
}

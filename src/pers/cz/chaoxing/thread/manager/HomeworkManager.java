package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.callback.impl.HomeworkCheckCodeCallBack;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizConfig;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizData;
import pers.cz.chaoxing.common.task.data.homework.HomeworkTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.thread.task.HomeworkTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.IOLock;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;

import java.util.Arrays;

/**
 * @author p_chncheng
 * @create 2018/9/4
 */
public class HomeworkManager extends Manager {
    private CallBack<CallBackData> homeworkCallBack;

    public HomeworkManager(int threadPoolSize) {
        super(threadPoolSize);
        this.homeworkCallBack = new HomeworkCheckCodeCallBack("./checkCode-homework.jpeg");
    }

    @Override
    public void doJob() throws Exception {
        paramsList.forEach(Try.once(params -> {
            acquire();
            TaskInfo<HomeworkTaskData> homeworkInfo = Try.ever(() -> CXUtil.getTaskInfo(baseUri, uriModel, params, InfoType.Homework), customCallBack);
            release();
            if (!(homeworkInfo.getDefaults().isFiled() || 1 == homeworkInfo.getDefaults().getState()) || !skipReview)
                Arrays.stream(homeworkInfo.getAttachments()).forEach(attachment -> Try.ever(() -> {
                    QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo = CXUtil.getHomeworkQuiz(baseUri, homeworkInfo, attachment);
                    if (!homeworkQuizInfo.isPassed()) {
                        String homeworkName = attachment.getProperty().getTitle();
                        IOLock.output(() -> System.out.println("Homework did not pass: " + homeworkName));
                        HomeworkTask homeworkTask = new HomeworkTask(homeworkInfo, attachment, homeworkQuizInfo, baseUri);
                        homeworkTask.setCheckCodeCallBack(homeworkCallBack);
                        homeworkTask.setHasSleep(hasSleep);
                        homeworkTask.setSemaphore(semaphore);
                        homeworkTask.setAutoComplete(autoComplete);
                        completionService.submit(homeworkTask);
                        threadCount++;
                        IOLock.output(() -> System.out.println("Added homeworkTask to ThreadPool: " + homeworkName));
                    } else
                        release();
                }, customCallBack));
        }));
        IOLock.output(() -> System.out.println("All homework task has been called"));
    }

    public void close() {
        super.close();
        IOLock.output(() -> System.out.println("Finished homeworkTask count: " + threadCount));
    }
}

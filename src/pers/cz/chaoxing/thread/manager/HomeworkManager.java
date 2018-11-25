package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.callback.CheckCodeSingletonFactory;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizConfig;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizData;
import pers.cz.chaoxing.common.task.data.homework.HomeworkTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.thread.task.HomeworkTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.IOUtil;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;

import java.util.Arrays;

/**
 * @author p_chncheng
 * @since 2018/9/4
 */
public class HomeworkManager extends ManagerModel {

    public HomeworkManager(int threadPoolSize) {
        super(threadPoolSize);
    }

    @Override
    public void doJob() throws Exception {
        paramsList.forEach(Try.once(params -> {
            acquire();
            TaskInfo<HomeworkTaskData> homeworkInfo = Try.ever(() -> CXUtil.getTaskInfo(baseUri, uriModel, params, InfoType.Homework), CheckCodeSingletonFactory.CUSTOM.get());
            release();
            if (!(homeworkInfo.getDefaults().isFiled() || 1 == homeworkInfo.getDefaults().getState()) || !skipReview)
                Arrays.stream(homeworkInfo.getAttachments()).forEach(attachment -> Try.ever(() -> {
                    QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo = CXUtil.getHomeworkQuiz(baseUri, homeworkInfo, attachment);
                    if (!homeworkQuizInfo.isPassed()) {
                        String homeworkName = attachment.getProperty().getTitle();
                        IOUtil.println("Homework did not pass: " + homeworkName);
                        HomeworkTask homeworkTask = new HomeworkTask(homeworkInfo, attachment, homeworkQuizInfo, baseUri);
                        homeworkTask.setCheckCodeCallBack(CheckCodeSingletonFactory.HOMEWORK.get());
                        homeworkTask.setHasSleep(hasSleep);
                        homeworkTask.setSemaphore(semaphore);
                        homeworkTask.setCompleteStyle(completeStyle);
                        completionService.submit(homeworkTask);
                        threadCount++;
                        IOUtil.println("Added homeworkTask to ThreadPool: " + homeworkName);
                    } else
                        release();
                }, CheckCodeSingletonFactory.CUSTOM.get()));
        }));
        IOUtil.println("All homework task has been called");
    }

    public void close() {
        super.close();
        IOUtil.println("Finished homeworkTask count: " + threadCount);
    }
}

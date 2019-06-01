package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.callback.checkcode.CheckCodeData;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizConfig;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.homework.HomeworkTaskData;
import pers.cz.chaoxing.thread.task.HomeworkTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author p_chncheng
 * @since 2018/9/4
 */
public class HomeworkManager extends ManagerModel {
    private CheckCodeData checkCodeData;

    public HomeworkManager(int threadPoolSize) {
        super(threadPoolSize);
        checkCodeData = new CheckCodeData(true, "", "0");
    }

    @Override
    public void doJob() throws Exception {
        urls.forEach(Try.once(chapterURLs -> {
            final String utEnc = getUtEnc(chapterURLs);
            chapterURLs.forEach(Try.once(url -> {
                control.checkState(this);
                TaskInfo<HomeworkTaskData> homeworkInfo;
                control.acquire();
                try {
                    homeworkInfo = Try.ever(() -> CXUtil.getTaskInfo(url + checkCodeData.toQueryString(), InfoType.HOMEWORK), CheckCodeFactory.CHAPTER.get(), checkCodeData);
                } finally {
                    control.release();
                }
                Optional.ofNullable(homeworkInfo.getDefaults()).ifPresent(defaults -> {
                    if (!(defaults.isFiled() || 1 == defaults.getState()) || control.isReview())
                        Arrays.stream(homeworkInfo.getAttachments()).forEach(attachment -> Try.ever(() -> {
                            attachment.setUtEnc(utEnc);
                            QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo = CXUtil.getHomeworkQuiz(url, homeworkInfo, attachment);
                            if (!homeworkQuizInfo.isPassed()) {
                                String homeworkName = attachment.getProperty().getTitle();
                                IOUtil.println("manager_homework_thread_start", homeworkName);
                                HomeworkTask homeworkTask = new HomeworkTask(homeworkInfo, attachment, homeworkQuizInfo, url);
                                homeworkTask.setCheckCodeCallBack(CheckCodeFactory.HOMEWORK.get());
                                homeworkTask.setControl(control);
                                completionService.submit(homeworkTask);
                                threadCount++;
                                IOUtil.println("manager_homework_thread_finish", homeworkName);
                            }
                        }, CheckCodeFactory.CUSTOM.get()));
                });
            }));
        }));
        IOUtil.println("manager_homework_start");
    }

    private String getUtEnc(List<String> chapterURLs) throws InterruptedException {
        control.acquire();
        try {
            return chapterURLs.stream()
                    .map(url -> Try.ever(() -> CXUtil.getUtEnc(url), CheckCodeFactory.CUSTOM.get()))
                    .filter(str -> !str.isEmpty())
                    .findFirst().orElse("");
        } finally {
            control.release();
        }
    }

    public void close() {
        super.close();
        IOUtil.println("manager_homework_finish", successCount, threadCount);
    }
}

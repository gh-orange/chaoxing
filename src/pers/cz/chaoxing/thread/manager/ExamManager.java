package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.callback.CheckCodeSingletonFactory;
import pers.cz.chaoxing.common.task.data.exam.ExamTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.thread.task.ExamTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.IOUtil;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;

import java.util.Arrays;

/**
 * @author p_chncheng
 * @since 2018/9/4
 */
public class ExamManager extends ManagerModel {

    public ExamManager(int threadPoolSize) {
        super(threadPoolSize);
    }

    @Override
    public void doJob() {
        paramsList.forEach(Try.once(params -> {
            TaskInfo<ExamTaskData> examInfo = Try.ever(() -> CXUtil.getTaskInfo(baseUri, uriModel, params, InfoType.Exam), CheckCodeSingletonFactory.CUSTOM.get());
            Arrays.stream(examInfo.getAttachments())
                    .filter(attachment -> !attachment.isPassed() || !skipReview)
                    .forEach(attachment -> Try.ever(() -> {
                        boolean isAllowed;
                        try {
                            isAllowed = CXUtil.startExam(baseUri, examInfo, attachment);
                        } catch (CheckCodeException e) {
                            attachment.setEnc(((CallBackData) CheckCodeSingletonFactory.EXAM.get().call(e.getSession(), e.getUri(), attachment.getProperty().gettId(), examInfo.getDefaults().getClazzId(), examInfo.getDefaults().getCourseid(), "callback")).getEnc());
                            isAllowed = true;
                        }
                        if (isAllowed) {
                            String examName = attachment.getProperty().getTitle();
                            IOUtil.println("exam did not pass: " + examName);
                            ExamTask examTask = new ExamTask(examInfo, attachment, baseUri);
                            examTask.setCheckCodeCallBack(CheckCodeSingletonFactory.EXAM.get());
                            examTask.setHasSleep(hasSleep);
                            examTask.setSemaphore(semaphore);
                            examTask.setCompleteStyle(completeStyle);
                            completionService.submit(examTask);
                            threadCount++;
                            IOUtil.println("Added examTask to ThreadPool: " + examName);
                        }
                    }, CheckCodeSingletonFactory.CUSTOM.get()));
        }));
        IOUtil.println("All exam task has been called");
    }

    public void close() {
        super.close();
        IOUtil.println("Finished examTask count: " + threadCount);
    }
}

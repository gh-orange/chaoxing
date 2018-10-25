package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.callback.impl.CustomCheckCodeCallBack;
import pers.cz.chaoxing.callback.impl.ExamCheckCodeCallBack;
import pers.cz.chaoxing.common.task.data.exam.ExamTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.thread.task.ExamTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.IOLock;
import pers.cz.chaoxing.util.InfoType;
import pers.cz.chaoxing.util.Try;

import java.util.Arrays;

/**
 * @author p_chncheng
 * @create 2018/9/4
 */
public class ExamManager extends Manager {
    private CallBack<CallBackData> examCallBack;

    public ExamManager(int threadPoolSize) {
        super(threadPoolSize);
        this.examCallBack = new ExamCheckCodeCallBack("./checkCode-exam.jpeg");
        ((ExamCheckCodeCallBack) this.examCallBack).setScanner(((CustomCheckCodeCallBack) this.customCallBack).getScanner());
    }

    @Override
    public void doJob() {
        paramsList.forEach(Try.once(params -> {
            TaskInfo<ExamTaskData> examInfo = Try.ever(() -> CXUtil.getTaskInfo(baseUri, uriModel, params, InfoType.Exam), customCallBack);
            Arrays.stream(examInfo.getAttachments())
                    .filter(attachment -> !attachment.isPassed() || !skipReview)
                    .forEach(attachment -> Try.ever(() -> {
                        boolean isAllowed;
                        try {
                            isAllowed = CXUtil.startExam(baseUri, examInfo, attachment);
                        } catch (CheckCodeException e) {
                            attachment.setEnc(examCallBack.call(e.getSession(), e.getUri(), attachment.getProperty().gettId(), examInfo.getDefaults().getClazzId(), examInfo.getDefaults().getCourseid(), "callback").getEnc());
                            isAllowed = true;
                        }
                        if (isAllowed) {
                            String examName = attachment.getProperty().getTitle();
                            IOLock.output(() -> System.out.println("exam did not pass: " + examName));
                            ExamTask examTask = new ExamTask(examInfo, attachment, baseUri);
                            examTask.setCheckCodeCallBack(examCallBack);
                            examTask.setHasSleep(hasSleep);
                            examTask.setSemaphore(semaphore);
                            examTask.setAutoComplete(autoComplete);
                            completionService.submit(examTask);
                            threadCount++;
                            IOLock.output(() -> System.out.println("Added examTask to ThreadPool: " + examName));
                        }
                    }, customCallBack));
        }));
        IOLock.output(() -> System.out.println("All exam task has been called"));
    }

    public void close() {
        super.close();
        IOLock.output(() -> System.out.println("Finished examTask count: " + threadCount));
    }
}

package pers.cz.chaoxing.thread.manager;

import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.callback.checkcode.CheckCodeData;
import pers.cz.chaoxing.common.task.data.exam.ExamTaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.thread.task.ExamTask;
import pers.cz.chaoxing.util.InfoType;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author p_chncheng
 * @since 2018/9/4
 */
public class ExamManager extends ManagerModel {

    public ExamManager(int threadPoolSize) {
        super(threadPoolSize);
    }

    @Override
    public void doJob() throws Exception {
        urls.stream()
                .flatMap(Collection::stream)
                .map(Try.ever(CXUtil::getCourseInfo, CheckCodeFactory.CUSTOM.get()))
                .map(urlMap -> urlMap.get("examURLs"))
                .flatMap(Collection::stream)
                .forEach(Try.once(url -> {
                    control.checkState(this);
                    TaskInfo<ExamTaskData> examInfo = Try.ever(() -> CXUtil.getTaskInfo(url, InfoType.EXAM), CheckCodeFactory.CUSTOM.get());
                    Arrays.stream(examInfo.getAttachments())
                            .filter(attachment -> !attachment.isPassed() || control.isReview())
                            .forEach(attachment -> Try.ever(() -> {
                                boolean isAllowed = true;
                                try {
                                    isAllowed = CXUtil.startExam(url, examInfo, attachment);
                                } catch (CheckCodeException e) {
                                    attachment.setEnc(((CheckCodeData) CheckCodeFactory.EXAM.get().onCheckCode(e.getUrl(), attachment.getProperty().gettId(), examInfo.getDefaults().getClazzId(), examInfo.getDefaults().getCourseid(), "callback")).getEnc());
                                }
                                if (isAllowed) {
                                    String examName = attachment.getProperty().getTitle();
                                    IOUtil.println("exam did not pass: " + examName);
                                    ExamTask examTask = new ExamTask(examInfo, attachment, url);
                                    examTask.setCheckCodeCallBack(CheckCodeFactory.EXAM.get());
                                    examTask.setControl(control);
                                    completionService.submit(examTask);
                                    threadCount++;
                                    IOUtil.println("Added examTask to ThreadPool: " + examName);
                                }
                            }, CheckCodeFactory.CUSTOM.get()));
                }));
        IOUtil.println("All exam task has been called");
    }

    public void close() {
        super.close();
        IOUtil.println("Finished examTask count: " + successCount + "/" + threadCount);
    }
}

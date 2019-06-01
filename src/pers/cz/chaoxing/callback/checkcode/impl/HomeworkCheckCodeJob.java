package pers.cz.chaoxing.callback.checkcode.impl;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.StatusCodes;
import pers.cz.chaoxing.callback.checkcode.CheckCodeCallBack;
import pers.cz.chaoxing.callback.checkcode.CheckCodeData;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.net.ApiURL;
import pers.cz.chaoxing.util.net.NetUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.stream.Stream;

public class HomeworkCheckCodeJob extends CheckCodeJobModel implements CheckCodeCallBack<CheckCodeData> {
    private CheckCodeData checkCodeData;
    private String nodeId;
    private String clazzId;
    private String courseId;

    public HomeworkCheckCodeJob(String checkCodePath) {
        super(checkCodePath);
        this.checkCodeData = new CheckCodeData(true);
    }

    @Override
    public CheckCodeData onCheckCode(String... param) {
        if (lock.writeLock().tryLock()) {
            receiveURL = param[0];
            baseURL = NetUtil.getOriginal(receiveURL);
            try {
                do {
                    if (!receiveCheckCode(checkCodePath))
                        break;
                    if (!readCheckCode(checkCodePath))
                        IOUtil.println("check_code_image_path", checkCodePath);
                    checkCodeData = setCheckCode(IOUtil.printAndNext("input_check_code"), param[1], param[2], param[3]);
                } while (!checkCodeData.isStatus());
            } finally {
                lock.writeLock().unlock();
            }
        }
        lock.readLock().lock();
        try {
            return checkCodeData;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected boolean receiveCheckCode(String path) {
        do {
            RawResponse response = Try.ever(() -> NetUtil.get(receiveURL, 1), CheckCodeFactory.CUSTOM.get());
            if (StatusCodes.NOT_FOUND == response.getStatusCode())
                return false;
            response.writeToFile(path);
            try (Stream<String> lines = Files.lines(Paths.get(path), StandardCharsets.ISO_8859_1)) {
                if (lines.anyMatch(line -> line.contains("<html")))
                    CheckCodeFactory.CUSTOM.get().onCheckCode(receiveURL);
                else
                    break;
            } catch (IOException ignored) {
            }
        } while (true);
        return true;
    }

    private CheckCodeData setCheckCode(String checkCode, String nodeId, String clazzId, String courseId) {
        this.nodeId = nodeId;
        this.clazzId = clazzId;
        this.courseId = courseId;
        return sendCheckCode(checkCode);
    }

    @Override
    protected CheckCodeData sendCheckCode(final String checkCode) {
        CheckCodeData checkCodeData = Try.ever(() -> NetUtil.post(ApiURL.HOMEWORK_CHECK_CODE_SEND.buildURL(baseURL,
                checkCode), 1), CheckCodeFactory.CUSTOM.get())
                .toJsonResponse(CheckCodeData.class).getBody();
        if (!Optional.ofNullable(checkCodeData).isPresent())
            return new CheckCodeData(false);
        checkCodeData.setCode(checkCode);
        checkCodeData.setStatus("true".equals(Try.ever(() -> NetUtil.post(ApiURL.HOMEWORK_CHECK_CODE_VALIDATE.buildURL(baseURL,
                checkCode), 1), CheckCodeFactory.CUSTOM.get()).toTextResponse().getBody()));
        if (checkCodeData.isStatus())
            checkCodeData.setStatus(Try.ever(() -> NetUtil.get(ApiURL.HOMEWORK_CHECK_CODE_STATUS.buildURL(baseURL,
                    clazzId,
                    courseId,
                    nodeId,
                    checkCode
            )), CheckCodeFactory.CUSTOM.get()).toTextResponse().getBody().contains("true"));
        return checkCodeData;
    }
}

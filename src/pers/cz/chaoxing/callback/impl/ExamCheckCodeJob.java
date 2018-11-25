package pers.cz.chaoxing.callback.impl;

import com.alibaba.fastjson.JSON;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Session;
import net.dongliu.requests.StatusCodes;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.util.IOUtil;
import pers.cz.chaoxing.util.StringUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ExamCheckCodeJob extends CheckCodeJobModel implements CallBack<CallBackData> {
    private CallBackData callBackData;
    private String id;
    private String classId;
    private String courseId;
    private String callback;

    public ExamCheckCodeJob(String checkCodePath) {
        super(checkCodePath);
        this.callBackData = new CallBackData(true);
    }

    @Override
    public CallBackData call(Session session, String... param) {
        if (this.lock.writeLock().tryLock()) {
            this.receiveUri = param[0];
            this.baseUri = getBaseUri(this.receiveUri);
            this.session = session;
            try {
                do {
                    if (!receiveCheckCode(this.checkCodePath))
                        break;
                    if (!readCheckCode(checkCodePath))
                        IOUtil.println("CheckCode image path: " + checkCodePath);
                    IOUtil.print("Input checkCode:");
                    callBackData = setCheckCode(IOUtil.next(), param[1], param[2], param[3], param[4]);
                } while (!callBackData.isStatus());
            } finally {
                lock.writeLock().unlock();
            }
        }
        this.lock.readLock().lock();
        try {
            return callBackData;
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    protected boolean receiveCheckCode(String path) {
        while (true) {
            RawResponse response = session.get(receiveUri).followRedirect(false).proxy(proxy).send();
            if (response.getStatusCode() == StatusCodes.NOT_FOUND || response.getStatusCode() == StatusCodes.FOUND)
                return false;
            this.sendUri = "/exam/test/getIdentifyCode";
            response.writeToFile(path);
            try {
                if (Files.lines(Paths.get(path), StandardCharsets.ISO_8859_1).anyMatch(line -> line.contains("<html")))
                    new CustomCheckCodeJob(this.checkCodePath).call(this.session, this.receiveUri);
                else
                    break;
            } catch (IOException ignored) {
            }
        }
        return true;
    }

    private CallBackData setCheckCode(String checkCode, String id, String classId, String courseId, String callback) {
        this.id = id;
        this.classId = classId;
        this.courseId = courseId;
        this.callback = callback;
        return sendCheckCode(checkCode);
    }

    @Override
    protected CallBackData sendCheckCode(String checkCode) {
        Map<String, String> params = new HashMap<>();
        params.put("id", id);
        params.put("classId", classId);
        params.put("courseId", courseId);
        params.put("callback", callback);
        params.put("inpCode", checkCode);
        RawResponse response = session.get(this.baseUri + this.sendUri).params(params).followRedirect(false).proxy(proxy).send();
        String responseStr = StringUtil.subStringBetweenFirst(response.readToText(), params.get("callback") + "(", ")");
        CallBackData callBackData = JSON.parseObject(responseStr, CallBackData.class);
        callBackData.setCode(checkCode);
        return callBackData;
    }
}

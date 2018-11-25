package pers.cz.chaoxing.callback.impl;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Session;
import net.dongliu.requests.StatusCodes;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.callback.CheckCodeSingletonFactory;
import pers.cz.chaoxing.util.IOUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class HomeworkCheckCodeJob extends CheckCodeJobModel implements CallBack<CallBackData> {
    private CallBackData callBackData;
    private String nodeId;
    private String clazzId;
    private String courseId;

    public HomeworkCheckCodeJob(String checkCodePath) {
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
                    callBackData = setCheckCode(IOUtil.next(), param[1], param[2], param[3]);
                } while (!callBackData.isStatus());
            } finally {
                this.lock.writeLock().unlock();
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
            this.sendUri = "/img/ajaxValidate2";
            response.writeToFile(path);
            try (Stream<String> lines = Files.lines(Paths.get(path), StandardCharsets.ISO_8859_1)) {
                if (lines.anyMatch(line -> line.contains("<html")))
                    CheckCodeSingletonFactory.CUSTOM.get().call(this.session, this.receiveUri);
                else
                    break;
            } catch (IOException ignored) {
            }
        }
        return true;
    }

    private CallBackData setCheckCode(String checkCode, String nodeId, String clazzId, String courseId) {
        this.nodeId = nodeId;
        this.clazzId = clazzId;
        this.courseId = courseId;
        return sendCheckCode(checkCode);
    }

    @Override
    protected CallBackData sendCheckCode(String checkCode) {
        Map<String, String> params = new HashMap<>();
        params.put("code", checkCode);
        RawResponse response = session.post(this.baseUri + this.sendUri).params(params).followRedirect(false).proxy(proxy).send();
        CallBackData callBackData = response.readToJson(CallBackData.class);
        if (!Optional.ofNullable(callBackData).isPresent())
            return new CallBackData(false);
        callBackData.setCode(checkCode);
        callBackData.setStatus(session.post(this.baseUri + "/img/ajaxValidate").params(params).followRedirect(false).proxy(proxy).send().readToText().equals("true"));
        if (callBackData.isStatus()) {
            params.put("nodeid", nodeId);
            params.put("clazzid", clazzId);
            params.put("courseId", courseId);
            callBackData.setStatus(session.get(this.baseUri + "/edit/selfservice").params(params).proxy(proxy).send().readToText().contains("true"));
        }
        return callBackData;
    }
}

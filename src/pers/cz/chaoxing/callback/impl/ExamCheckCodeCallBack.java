package pers.cz.chaoxing.callback.impl;

import com.alibaba.fastjson.JSON;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Session;
import net.dongliu.requests.StatusCodes;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.util.CXUtil;

import java.awt.*;
import java.io.File;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class ExamCheckCodeCallBack implements CallBack<CallBackData> {
    private String completeUri;
    private String actionUri;
    private String baseUri;
    private Session session;
    private String checkCodePath;
    private Scanner scanner;
    private ReentrantLock lock;
    private static Proxy proxy = CXUtil.proxy;

    public ExamCheckCodeCallBack(String checkCodePath) {
        this.checkCodePath = checkCodePath;
        this.scanner = new Scanner(System.in);
        this.lock = new ReentrantLock();
    }

    @Override
    public CallBackData call(Session session, String... param) {
        lock.lock();
        this.completeUri = param[0];
        this.baseUri = getBaseUri(this.completeUri);
        this.session = session;
        CallBackData callBackData = new CallBackData(true);
        do {
            if (!saveCheckCode(this.checkCodePath))
                break;
            if (openFile(checkCodePath))
                System.out.println("CheckCode image path:" + checkCodePath);
            System.out.print("Input checkCode:");
            callBackData = setCheckCode(this.scanner.nextLine(), param[1], param[2], param[3], param[4]);
        } while (!callBackData.isStatus());
        lock.unlock();
        return callBackData;
    }

    @Override
    public CallBackData print(String str) {
        if (!lock.isLocked())
            System.out.println(Thread.currentThread().getName() + ": " + str);
        return null;
    }

    private String getBaseUri(String uri) {
        return uri.substring(0, uri.indexOf('/', uri.indexOf("://") + "://".length()));
    }

    private boolean saveCheckCode(String path) {
        RawResponse response = session.get(completeUri).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.NOT_FOUND || response.getStatusCode() == StatusCodes.FOUND)
            return false;
        this.actionUri = "/exam/test/getIdentifyCode";
        response.writeToFile(path);
        return true;
    }

    private CallBackData setCheckCode(String checkCode, String id, String classId, String courseId, String callback) {
        Map<String, String> params = new HashMap<>();
        params.put("id", id);
        params.put("classId", classId);
        params.put("courseId", courseId);
        params.put("callback", callback);
        params.put("inpCode", checkCode);
        RawResponse response = session.get(this.baseUri + this.actionUri).params(params).followRedirect(false).proxy(proxy).send();
        String responseStr = response.readToText();
        String begin = params.get("callback") + "(";
        String end = ")";
        int beginIndex = responseStr.indexOf(begin) + begin.length();
        responseStr = responseStr.substring(beginIndex, responseStr.indexOf(end, beginIndex));
        CallBackData callBackData = JSON.parseObject(responseStr, CallBackData.class);
        callBackData.setCode(checkCode);
        return callBackData;
    }

    public boolean openFile(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }
}

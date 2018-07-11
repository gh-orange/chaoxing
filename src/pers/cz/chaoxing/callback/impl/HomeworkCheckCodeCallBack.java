package pers.cz.chaoxing.callback.impl;

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

public class HomeworkCheckCodeCallBack implements CallBack<CallBackData> {
    private String completeUri;
    private String actionUri;
    private String baseUri;
    private Session session;
    private String checkCodePath;
    private Scanner scanner;
    private ReentrantLock lock;
    private static Proxy proxy = CXUtil.proxy;

    public HomeworkCheckCodeCallBack(String checkCodePath) {
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
            callBackData = setCheckCode(this.scanner.nextLine(), param[1], param[2], param[3]);
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

    public String getCheckCodePath() {
        return checkCodePath;
    }

    private boolean saveCheckCode(String path) {
        RawResponse response = session.get(completeUri).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.NOT_FOUND || response.getStatusCode() == StatusCodes.FOUND)
            return false;
        this.actionUri = "/img/ajaxValidate2";
        response.writeToFile(path);
        return true;
    }

    private CallBackData setCheckCode(String checkCode, String nodeId, String clazzId, String courseId) {
        Map<String, String> params = new HashMap<>();
        params.put("code", checkCode);
        RawResponse response = session.post(this.baseUri + this.actionUri).params(params).followRedirect(false).proxy(proxy).send();
        CallBackData callBackData = response.readToJson(CallBackData.class);
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

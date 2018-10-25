package pers.cz.chaoxing.callback.impl;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Session;
import net.dongliu.requests.StatusCodes;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CallBackData;
import pers.cz.chaoxing.util.IOLock;
import pers.cz.chaoxing.util.CXUtil;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

public class HomeworkCheckCodeCallBack implements CallBack<CallBackData> {
    private String completeUri;
    private String actionUri;
    private String baseUri;
    private Session session;
    private String checkCodePath;
    private Scanner scanner;
    private ReadWriteLock lock;
    private CallBackData callBackData;
    private static Proxy proxy = CXUtil.proxy;

    public HomeworkCheckCodeCallBack(String checkCodePath) {
        this.checkCodePath = checkCodePath;
        this.scanner = new Scanner(System.in);
        this.lock = new ReentrantReadWriteLock();
        this.callBackData = new CallBackData(true);
    }

    @Override
    public CallBackData call(Session session, String... param) {
        if (this.lock.writeLock().tryLock()) {
            this.completeUri = param[0];
            this.baseUri = getBaseUri(this.completeUri);
            this.session = session;
            try {
                IOLock.input(() -> {
                    do {
                        if (!saveCheckCode(this.checkCodePath))
                            break;
                        if (!openFile(checkCodePath))
                            System.out.println("CheckCode image path: " + checkCodePath);
                        System.out.print("Input checkCode:");
                        callBackData = setCheckCode(this.scanner.nextLine(), param[1], param[2], param[3]);
                    } while (!callBackData.isStatus());
                });
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
    public void print(String first, String... more) {
        this.lock.readLock().lock();
        try {
            CallBack.super.print(first, more);
        } finally {
            this.lock.readLock().unlock();
        }
    }

    private String getBaseUri(String uri) {
        return uri.substring(0, uri.indexOf('/', uri.indexOf("://") + "://".length()));
    }

    private boolean saveCheckCode(String path) {
        while (true) {
            RawResponse response = session.get(completeUri).followRedirect(false).proxy(proxy).send();
            if (response.getStatusCode() == StatusCodes.NOT_FOUND || response.getStatusCode() == StatusCodes.FOUND)
                return false;
            this.actionUri = "/img/ajaxValidate2";
            response.writeToFile(path);
            try (Stream<String> lines = Files.lines(Paths.get(path), StandardCharsets.ISO_8859_1)) {
                if (lines.anyMatch(line -> line.contains("<html")))
                    new CustomCheckCodeCallBack(this.checkCodePath).call(this.session, this.completeUri);
                else
                    break;
            } catch (IOException ignored) {
            }
        }
        return true;
    }

    private CallBackData setCheckCode(String checkCode, String nodeId, String clazzId, String courseId) {
        Map<String, String> params = new HashMap<>();
        params.put("code", checkCode);
        RawResponse response = session.post(this.baseUri + this.actionUri).params(params).followRedirect(false).proxy(proxy).send();
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

    private boolean openFile(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
}

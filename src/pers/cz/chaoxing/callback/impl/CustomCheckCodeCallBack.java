package pers.cz.chaoxing.callback.impl;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Session;
import net.dongliu.requests.StatusCodes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.util.IOLock;
import pers.cz.chaoxing.util.CXUtil;

import java.awt.*;
import java.io.File;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class CustomCheckCodeCallBack implements CallBack<Boolean> {
    private String completeUri;
    private String actionUri;
    private String baseUri;
    private Session session;
    private String checkCodePath;
    private Scanner scanner;
    private ReadWriteLock lock;
    private static Proxy proxy = CXUtil.proxy;

    public CustomCheckCodeCallBack(String checkCodePath) {
        this.checkCodePath = checkCodePath;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public Boolean call(Session session, String... param) {
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
                    } while (!setCheckCode(this.scanner.next()));
                });
            } finally {
                lock.writeLock().unlock();
            }
        }
        this.lock.readLock().lock();
        try {
            return true;
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

    public Scanner getScanner() {
        return scanner;
    }

    public void setScanner(Scanner scanner) {
        this.scanner = scanner;
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
        Document document = Jsoup.parse(response.readToText());
        this.actionUri = document.select("form").attr("action");
        Element img = document.selectFirst("img");
        String imgUri = img.attr("src");
        if (imgUri.isEmpty()) {
            imgUri = img.attr("onclick");
            String begin = "this.src='";
            String end = "?";
            int beginIndex = imgUri.indexOf(begin) + begin.length();
            imgUri = imgUri.substring(beginIndex, imgUri.indexOf(end, beginIndex));
        }
        session.get(this.baseUri + imgUri).proxy(CXUtil.proxy).send().writeToFile(path);
        return true;
    }

    private boolean setCheckCode(String checkCode) {
        Map<String, String> params = new HashMap<>();
        params.put("ucode", checkCode);
        RawResponse response = session.get(this.baseUri + this.actionUri).params(params).followRedirect(false).proxy(proxy).send();
        return response.getStatusCode() != StatusCodes.FOUND;
    }

    public boolean openFile(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }
}

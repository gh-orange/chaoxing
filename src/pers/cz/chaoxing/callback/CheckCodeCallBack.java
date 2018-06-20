package pers.cz.chaoxing.callback;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Session;
import net.dongliu.requests.StatusCodes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.awt.*;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;

public class CheckCodeCallBack implements CallBack<Boolean> {
    private String completeUri;
    private String actionUri;
    private String baseUri;
    private Session session;
    private String checkCodePath;
    private Scanner scanner = new Scanner(System.in);
    private ReentrantLock lock = new ReentrantLock();

    public CheckCodeCallBack(String checkCodePath) {
        this.checkCodePath = checkCodePath;
    }

    @Override
    public Boolean call(String param, Session session) {
        lock.lock();
        this.completeUri = param;
        this.baseUri = getBaseUri(this.completeUri);
        this.session = session;
        do {
            if (!saveCheckCode(this.checkCodePath))
                break;
            if (openFile(checkCodePath))
                System.out.println("CheckCode image path:" + checkCodePath);
            System.out.print("Input checkCode:");
        } while (setCheckCode(this.scanner.nextLine()));
        lock.unlock();
        return true;
    }

    @Override
    public Boolean print(String str) {
        if (!lock.isLocked())
            System.out.println(Thread.currentThread().getName() + ": " + str);
        return true;
    }

    private String getBaseUri(String uri) {
        return uri.substring(0, uri.indexOf('/', uri.indexOf("://") + "://".length()));
    }

    public String getCheckCodePath() {
        return checkCodePath;
    }

    private boolean saveCheckCode(String path) {
        RawResponse response = session.get(completeUri).followRedirect(false).send();
        if (response.getStatusCode() == StatusCodes.NOT_FOUND || response.getStatusCode() == StatusCodes.FOUND)
            return false;
        Document document = Jsoup.parse(response.readToText());
        this.actionUri = document.select("form").attr("action");
        String imgUri = document.select("img").attr("src");
        session.get(this.baseUri + imgUri).send().writeToFile(path);
        return true;
    }

    private boolean setCheckCode(String checkCode) {
        Map<String, String> params = new HashMap<>();
        params.put("ucode", checkCode);
        RawResponse response = session.get(this.baseUri + this.actionUri).params(params).followRedirect(false).send();
        return response.getStatusCode() != StatusCodes.FOUND;
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

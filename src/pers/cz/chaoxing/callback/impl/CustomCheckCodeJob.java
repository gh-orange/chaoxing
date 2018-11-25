package pers.cz.chaoxing.callback.impl;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Session;
import net.dongliu.requests.StatusCodes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.IOUtil;
import pers.cz.chaoxing.util.StringUtil;

import java.util.HashMap;
import java.util.Map;

public class CustomCheckCodeJob extends CheckCodeJobModel implements CallBack<Boolean> {

    public CustomCheckCodeJob(String checkCodePath) {
        super(checkCodePath);
    }

    @Override
    public Boolean call(Session session, String... param) {
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
                } while (!sendCheckCode(IOUtil.next()));
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
    protected boolean receiveCheckCode(String path) {
        RawResponse response = session.get(receiveUri).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.NOT_FOUND || response.getStatusCode() == StatusCodes.FOUND)
            return false;
        Document document = Jsoup.parse(response.readToText());
        this.sendUri = document.select("form").attr("action");
        Element img = document.selectFirst("img");
        String imgUri = img.attr("src");
        if (imgUri.isEmpty())
            imgUri = StringUtil.subStringBetweenFirst(img.attr("onclick"), "this.src='", "?");
        session.get(this.baseUri + imgUri).proxy(CXUtil.proxy).send().writeToFile(path);
        return true;
    }

    @Override
    protected Boolean sendCheckCode(String checkCode) {
        Map<String, String> params = new HashMap<>();
        params.put("ucode", checkCode);
        RawResponse response = session.get(this.baseUri + this.sendUri).params(params).followRedirect(false).proxy(proxy).send();
        return response.getStatusCode() != StatusCodes.FOUND;
    }
}

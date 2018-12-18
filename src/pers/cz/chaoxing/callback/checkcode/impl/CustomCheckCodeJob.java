package pers.cz.chaoxing.callback.checkcode.impl;

import net.dongliu.requests.Response;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.util.net.NetUtil;
import net.dongliu.requests.StatusCodes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import pers.cz.chaoxing.callback.checkcode.CheckCodeCallBack;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.io.StringUtil;

import java.nio.file.Paths;

public class CustomCheckCodeJob extends CheckCodeJobModel implements CheckCodeCallBack<Boolean> {

    public CustomCheckCodeJob(String checkCodePath) {
        super(checkCodePath);
    }

    @Override
    public Boolean onCheckCode(String... param) {
        if (lock.writeLock().tryLock()) {
            receiveURL = param[0];
            baseURL = NetUtil.getOriginal(receiveURL);
            try {
                do {
                    if (!receiveCheckCode(checkCodePath))
                        break;
                    if (!readCheckCode(checkCodePath))
                        IOUtil.println("CheckCode image path: " + checkCodePath);
                } while (!sendCheckCode(IOUtil.printAndNext("Input checkCode:")));
            } finally {
                lock.writeLock().unlock();
            }
        }
        lock.readLock().lock();
        try {
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected boolean receiveCheckCode(String path) {
        try {
            Response<Document> response = NetUtil.get(receiveURL, 0).toResponse(CXUtil.RESPONSE_HANDLER);
            if (response.getStatusCode() == StatusCodes.NOT_FOUND)
                return false;
            sendURL = response.getBody().selectFirst("form").absUrl("action");
            Element img = response.getBody().selectFirst("img");
            String imgURL = img.absUrl("src");
            if (imgURL.isEmpty())
                imgURL = baseURL + StringUtil.subStringBetweenFirst(img.attr("onclick"), "src='", "?");
            NetUtil.get(imgURL).toFileResponse(Paths.get(path));
            return true;
        } catch (CheckCodeException ignored) {
        }
        return false;
    }

    @Override
    protected Boolean sendCheckCode(String checkCode) {
        try {
            NetUtil.get(sendURL + "?ucode=" + checkCode, 0);
        } catch (CheckCodeException e) {
            return true;
        }
        return false;
    }
}

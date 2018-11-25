package pers.cz.chaoxing.callback.impl;

import net.dongliu.requests.Session;

import java.awt.*;
import java.io.File;
import java.net.Proxy;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * the super class for checkCodes
 *
 * @author 橙子
 * @since 2018/11/21
 */
public abstract class CheckCodeJobModel {
    protected String baseUri;
    String receiveUri;
    String sendUri;
    String checkCodePath;
    Session session;
    ReadWriteLock lock;
    Proxy proxy;

    CheckCodeJobModel(String checkCodePath) {
        this.checkCodePath = checkCodePath;
    }

    public void setLock(ReadWriteLock lock) {
        this.lock = lock;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    String getBaseUri(String uri) {
        return uri.substring(0, uri.indexOf('/', uri.indexOf("://") + "://".length()));
    }

    boolean readCheckCode(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception ignored) {
            return false;
        }
        return true;
    }

    protected abstract Object sendCheckCode(String checkCode);

    protected abstract boolean receiveCheckCode(String path);
}

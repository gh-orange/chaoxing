package pers.cz.chaoxing.callback.checkcode.impl;

import java.awt.*;
import java.io.File;
import java.util.concurrent.locks.ReadWriteLock;

/**
 * the super class for checkCodes
 *
 * @author 橙子
 * @since 2018/11/21
 */
public abstract class CheckCodeJobModel {
    String baseURL;
    String receiveURL;
    String sendURL;
    String checkCodePath;
    ReadWriteLock lock;

    CheckCodeJobModel(String checkCodePath) {
        this.checkCodePath = checkCodePath;
    }

    public void setLock(ReadWriteLock lock) {
        this.lock = lock;
    }

    boolean readCheckCode(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    protected abstract Object sendCheckCode(String checkCode);

    protected abstract boolean receiveCheckCode(String path);
}

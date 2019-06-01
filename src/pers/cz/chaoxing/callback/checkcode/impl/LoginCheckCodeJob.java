package pers.cz.chaoxing.callback.checkcode.impl;

import pers.cz.chaoxing.callback.checkcode.CheckCodeCallBack;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.net.NetUtil;

import java.nio.file.Paths;

public class LoginCheckCodeJob extends CheckCodeJobModel implements CheckCodeCallBack<String> {
    private int fid;
    private String username;
    private String password;
    private String indexURL;

    public LoginCheckCodeJob(String checkCodePath) {
        super(checkCodePath);
    }

    @Override
    public String onCheckCode(String... param) {
        if (lock.writeLock().tryLock()) {
            receiveURL = param[0];
            fid = Integer.valueOf(param[1]);
            username = param[2];
            password = param[3];
            try {
                do {
                    if (!receiveCheckCode(checkCodePath))
                        break;
                    if (!readCheckCode(checkCodePath))
                        IOUtil.println("check_code_image_path", checkCodePath);
                    indexURL = sendCheckCode(IOUtil.printAndNextLine("input_check_code").replaceAll("\\s", ""));
                } while (indexURL.isEmpty());
            } finally {
                lock.writeLock().unlock();
            }
        }
        lock.readLock().lock();
        try {
            return indexURL;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected boolean receiveCheckCode(String path) {
        Try.ever(() -> NetUtil.get(receiveURL).toFileResponse(Paths.get(path)), CheckCodeFactory.CUSTOM.get());
        return true;
    }

    @Override
    protected String sendCheckCode(String checkCode) {
        try {
            return CXUtil.login(fid, username, password, checkCode);
        } catch (WrongAccountException e) {
            IOUtil.println("exception_account", e.getLocalizedMessage());
            username = IOUtil.printAndNextLine("input_account");
            password = IOUtil.printAndNextLine("input_password");
        } catch (CheckCodeException e) {
            receiveURL = e.getUrl();
        }
        return "";
    }
}

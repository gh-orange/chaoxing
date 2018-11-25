package pers.cz.chaoxing.callback.impl;

import net.dongliu.requests.Session;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.IOUtil;

public class LoginCheckCodeJob extends CheckCodeJobModel implements CallBack<Boolean> {
    private int fid;
    private String username;
    private String password;

    public LoginCheckCodeJob(String checkCodePath) {
        super(checkCodePath);
    }

    @Override
    public Boolean call(Session session, String... param) {
        if (this.lock.writeLock().tryLock()) {
            this.receiveUri = param[0];
            this.fid = Integer.valueOf(param[1]);
            this.username = param[2];
            this.password = param[3];
            this.session = session;
            try {
                do {
                    if (!receiveCheckCode(this.checkCodePath))
                        break;
                    if (!readCheckCode(checkCodePath))
                        IOUtil.println("CheckCode image path: " + checkCodePath);
                    IOUtil.print("Input checkCode:");
                } while (!sendCheckCode(IOUtil.nextLine().replaceAll("\\s", "")));
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
        session.get(receiveUri + "?" + System.currentTimeMillis()).proxy(proxy).send().writeToFile(path);
        return true;
    }

    @Override
    protected Boolean sendCheckCode(String checkCode) {
        try {
            return CXUtil.login(fid, username, password, checkCode);
        } catch (WrongAccountException e) {
            IOUtil.println(e.getLocalizedMessage());
            IOUtil.print("Input account:");
            username = IOUtil.nextLine();
            IOUtil.print("Input password:");
            password = IOUtil.nextLine();
        }
        return false;
    }
}

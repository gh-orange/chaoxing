package pers.cz.chaoxing.callback;

import pers.cz.chaoxing.callback.impl.*;

import java.net.Proxy;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * a factory to create singleton checkCodeCallBack
 *
 * @author 橙子
 * @since 2018/11/21
 */
public enum CheckCodeSingletonFactory {
    LOGIN {
        @Override
        protected void createInstance() {
            this.callBack = new LoginCheckCodeJob("./checkCode-login.jpeg");
        }
    },
    CUSTOM {
        @Override
        protected void createInstance() {
            this.callBack = new CustomCheckCodeJob("./checkCode-custom.jpeg");
        }
    },
    HOMEWORK {
        @Override
        protected void createInstance() {
            this.callBack = new HomeworkCheckCodeJob("./checkCode-homework.jpeg");
        }
    },
    EXAM {
        @Override
        protected void createInstance() {
            this.callBack = new ExamCheckCodeJob("./checkCode-exam.jpeg");
        }
    };

    protected CheckCodeJobModel callBack;

    CheckCodeSingletonFactory() {
        createInstance();
    }

    @SuppressWarnings("unchecked")
    public <T extends CallBack> T get() {
        return (T) this.callBack;
    }

    protected abstract void createInstance();

    public static void setProxy(Proxy proxy) {
        for (CheckCodeSingletonFactory factory : CheckCodeSingletonFactory.values())
            factory.callBack.setProxy(proxy);
    }

    public static void setLock(ReadWriteLock lock) {
        for (CheckCodeSingletonFactory factory : CheckCodeSingletonFactory.values())
            factory.callBack.setLock(lock);
    }

    static {
        CheckCodeSingletonFactory.setLock(new ReentrantReadWriteLock());
    }
}

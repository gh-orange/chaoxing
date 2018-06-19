package pers.cz.chaoxing.callback;

import net.dongliu.requests.Session;

public interface CallBack<T> {
    T call(String param, Session session);
    T print(String str);
}

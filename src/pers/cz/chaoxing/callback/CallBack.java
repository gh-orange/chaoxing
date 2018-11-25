package pers.cz.chaoxing.callback;

import net.dongliu.requests.Session;

public interface CallBack<T> {
    T call(Session session, String... param);
}

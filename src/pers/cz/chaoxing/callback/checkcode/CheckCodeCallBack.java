package pers.cz.chaoxing.callback.checkcode;

public interface CheckCodeCallBack<T> {
    T onCheckCode(String... param);
}

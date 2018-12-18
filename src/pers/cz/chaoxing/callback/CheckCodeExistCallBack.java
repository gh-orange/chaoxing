package pers.cz.chaoxing.callback;

import net.dongliu.requests.RawResponse;

/**
 * @author 橙子
 * @since 2018/12/6
 */
public interface CheckCodeExistCallBack {
    boolean isCheckCodeExist(RawResponse response);
}

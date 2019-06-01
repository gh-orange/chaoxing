package pers.cz.chaoxing.util.net;

import net.dongliu.requests.*;
import pers.cz.chaoxing.callback.CheckCodeExistCallBack;
import pers.cz.chaoxing.exception.CheckCodeException;

import java.net.Proxy;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author 橙子
 * @since 2018/12/4
 */
public class NetUtil {
    private static Session session = Requests.session();
    //    private static Proxy proxy = Proxies.httpProxy("10.14.36.103", 8080);
    private static Proxy proxy = null;

    public static RawResponse get(String url) throws CheckCodeException {
        return get(url, Integer.MAX_VALUE, response -> false);
    }

    public static RawResponse post(String url, int maxRedirects) throws CheckCodeException {
        return post(url, null, maxRedirects);
    }

    public static RawResponse post(String url, Map<String, String> body) throws CheckCodeException {
        return post(url, body, Integer.MAX_VALUE, response -> false);
    }

    public static RawResponse get(String url, CheckCodeExistCallBack redirectCallBack) throws CheckCodeException {
        return get(url, Integer.MAX_VALUE, redirectCallBack);
    }

    public static RawResponse post(String url, Map<String, String> body, CheckCodeExistCallBack redirectCallBack) throws CheckCodeException {
        return post(url, body, Integer.MAX_VALUE, redirectCallBack);
    }

    public static RawResponse get(String url, int maxRedirects) throws CheckCodeException {
        return get(url, maxRedirects, response -> false);
    }

    public static RawResponse post(String url, Map<String, String> body, int maxRedirects) throws CheckCodeException {
        return post(url, body, maxRedirects, response -> false);
    }

    public static RawResponse get(String url, int maxRedirects, CheckCodeExistCallBack redirectCallBack) throws CheckCodeException {
        RequestBuilder requestBuilder = session.get(url);
        return getResponse(requestBuilder, maxRedirects, redirectCallBack);
    }

    public static RawResponse post(String url, Map<String, String> body, int maxRedirects, CheckCodeExistCallBack redirectCallBack) throws CheckCodeException {
        RequestBuilder requestBuilder = session.post(url);
        if (Optional.ofNullable(body).isPresent())
            requestBuilder = requestBuilder.body(body);
        return getResponse(requestBuilder, maxRedirects, redirectCallBack);
    }

    private static RawResponse getResponse(RequestBuilder requestBuilder, int maxRedirects, CheckCodeExistCallBack redirectCallBack) throws CheckCodeException {
        final String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:64.0) Gecko/20100101 Firefox/64.0";
        RawResponse response = requestBuilder.userAgent(userAgent).followRedirect(false).proxy(proxy).send();
        while (maxRedirects-- > 0)
            if (StatusCodes.FOUND == response.getStatusCode()) {
                if (redirectCallBack.isCheckCodeExist(response))
                    throw new CheckCodeException(response.getHeader("location"));
                else
                    response = session.get(response.getHeader("location")).headers(new Header("Referer", response.getURL())).userAgent(userAgent).cookies(session.currentCookies()).followRedirect(false).proxy(proxy).send();
            } else
                break;
        if (StatusCodes.FOUND == response.getStatusCode())
            throw new CheckCodeException(response.getHeader("location"));
        return response;
    }

    public static String getOriginal(String path) {
        int index = path.indexOf('?');
        int end = index == -1 ? path.lastIndexOf('/') : path.lastIndexOf('/', index);
        if ('/' == path.charAt(end - 1))
            end = path.length();
        else
            end += 1;
        return path.substring(0, end);
    }

    public static String getHost(String path) {
        return URI.create(path).getHost();
    }

    public static List<Parameter<String>> getQueries(String path) {
        return URIEncoder.decodeQueries(Optional.ofNullable(URI.create(path).getRawQuery()).orElse(""), StandardCharsets.UTF_8);
    }

    public static void addCookie(Cookie cookie) {
        NetUtil.session.currentCookies().add(cookie);
    }
}

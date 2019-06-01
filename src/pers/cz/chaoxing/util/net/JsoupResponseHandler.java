package pers.cz.chaoxing.util.net;

import net.dongliu.requests.ResponseHandler;
import net.dongliu.requests.StatusCodes;
import net.dongliu.requests.exception.RequestsException;
import net.dongliu.requests.utils.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * @author 橙子
 * @since 2018/12/7
 */
public class JsoupResponseHandler implements ResponseHandler<Document> {
    private Charset defaultCharset;

    public JsoupResponseHandler() {
        defaultCharset = StandardCharsets.UTF_8;
    }

    public JsoupResponseHandler setDefaultCharset(String charsetName) {
        return setDefaultCharset(Charset.forName(charsetName));
    }

    public JsoupResponseHandler setDefaultCharset(Charset defaultCharset) {
        this.defaultCharset = defaultCharset;
        return this;
    }

    @Override
    public Document handle(ResponseInfo responseInfo) {
        try (Reader reader = new InputStreamReader(responseInfo.getIn(), getCharset(responseInfo))) {
            if (StatusCodes.FOUND == responseInfo.getStatusCode())
                return Jsoup.parse(IOUtils.readAll(reader), NetUtil.getOriginal(responseInfo.getHeaders().getHeader("location")));
            else
                return Jsoup.parse(IOUtils.readAll(reader), NetUtil.getOriginal(responseInfo.getUrl()));
        } catch (IOException e) {
            throw new RequestsException(e);
        }
    }

    /**
     * fix unable to get charset problem from ResponseInfo
     *
     * @param responseInfo response info
     * @return charset read from response info or {@link #defaultCharset}
     */
    private Charset getCharset(ResponseInfo responseInfo) {
        for (String item : Optional.ofNullable(responseInfo.getHeaders().getHeader("Content-Type")).orElse("").split(";")) {
            String itemTrim = item.trim();
            if (!itemTrim.isEmpty()) {
                int idx = itemTrim.indexOf('=');
                if (idx >= 0 && itemTrim.substring(0, idx).trim().equalsIgnoreCase("charset"))
                    return Charset.forName(itemTrim.substring(idx + 1).trim());
            }
        }
        return defaultCharset;
    }
}

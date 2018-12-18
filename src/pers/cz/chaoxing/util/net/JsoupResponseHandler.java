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
import java.util.Objects;

/**
 * @author 橙子
 * @since 2018/12/7
 */
public class JsoupResponseHandler implements ResponseHandler<Document> {
    @Override
    public Document handle(ResponseInfo responseInfo) {
        try (Reader reader = new InputStreamReader(responseInfo.getIn(), Objects.requireNonNull(responseInfo.getHeaders().getCharset()))) {
            if (StatusCodes.FOUND == responseInfo.getStatusCode())
                return Jsoup.parse(IOUtils.readAll(reader), NetUtil.getOriginal(responseInfo.getHeaders().getHeader("location")));
            else
                return Jsoup.parse(IOUtils.readAll(reader), NetUtil.getOriginal(responseInfo.getUrl()));
        } catch (IOException e) {
            throw new RequestsException(e);
        }
    }
}

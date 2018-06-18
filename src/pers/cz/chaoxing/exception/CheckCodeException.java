package pers.cz.chaoxing.exception;

import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Session;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;

public class CheckCodeException extends Exception {
    private String baseUri;
    private String uri;
    private Map<String, String> params;
    private String action;
    private Session session;

    public CheckCodeException(String baseUri, String uri, Session session) {
        this.baseUri = baseUri;
        this.uri = uri;
        this.session = session;
    }

    public CheckCodeException(String baseUri, String uri, Map<String, String> params, Session session) {
        this.baseUri = baseUri;
        this.uri = uri;
        this.params = params;
        this.session = session;
    }

    public void saveCheckCode(String path) {
        String responseStr;
        if (this.params != null)
            responseStr = session.get(this.uri).params(this.params).send().readToText();
        else
            responseStr = session.get(this.uri).send().readToText();
        Document document = Jsoup.parse(responseStr);
        this.action = document.select("form").attr("action");
        String uri = document.select("img").attr("src");
        session.get(baseUri + uri).send().writeToFile(path);
    }

    public boolean setCheckCode(String checkCode) {
        Map<String, String> params = new HashMap<>();
        params.put("ucode", checkCode);
        RawResponse response = session.get(this.baseUri + this.action).params(params).send();
        return !response.readToText().contains("操作出现异常");
    }
}

package pers.cz.chaoxing.exception;

import net.dongliu.requests.Session;

public class CheckCodeException extends Throwable {
    private String uri;
    private Session session;

    public CheckCodeException(String uri, Session session) {
        this.uri = uri;
        this.session = session;
    }

    public String getUri() {
        return uri;
    }

    public Session getSession() {
        return session;
    }
}

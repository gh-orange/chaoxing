package pers.cz.chaoxing.exception;

import net.dongliu.requests.Session;

public class CheckCodeException extends Exception {
    private Session session;
    private String uri;

    public CheckCodeException(Session session, String uri) {
        this.session = session;
        this.uri = uri;
    }

    public String getUri() {
        return uri;
    }

    public Session getSession() {
        return session;
    }
}

package pers.cz.chaoxing.exception;

public class CheckCodeException extends Exception {
    private String url;

    public CheckCodeException(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }
}

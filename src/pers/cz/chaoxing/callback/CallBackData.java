package pers.cz.chaoxing.callback;

public class CallBackData {
    private boolean status;
    private String code;
    private String enc;

    public CallBackData() {
    }

    public CallBackData(boolean status) {
        this.status = status;
    }

    public CallBackData(boolean status, String code) {
        this.status = status;
        this.code = code;
    }

    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getEnc() {
        return enc;
    }

    public void setEnc(String enc) {
        this.enc = enc;
    }
}

package pers.cz.chaoxing.callback.checkcode;

public class CheckCodeData {
    private boolean status;
    private String code;
    private String enc;
    private String cpi;

    public CheckCodeData() {
    }

    public CheckCodeData(boolean status) {
        this.status = status;
    }

    public CheckCodeData(boolean status, String code, String cpi) {
        this.status = status;
        this.code = code;
        this.cpi = cpi;
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

    public String getCpi() {
        return cpi;
    }

    public void setCpi(String cpi) {
        this.cpi = cpi;
    }

    public String toQueryString() {
        return "&code=" + code + "&cpi=" + cpi;
    }
}

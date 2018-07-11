package pers.cz.chaoxing.exception;

public class WrongAccountException extends Throwable {

    public WrongAccountException() {
        super("account error");
    }
}

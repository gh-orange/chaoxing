package pers.cz.chaoxing.util;

import pers.cz.chaoxing.callback.checkcode.CheckCodeCallBack;
import pers.cz.chaoxing.callback.checkcode.CheckCodeData;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizConfig;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizConfig;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * lambda checked exception throw cheat util
 *
 * @author 橙子
 * @since 2018/9/28
 */
public final class Try {

    @FunctionalInterface
    public interface ExceptionFunction<T, R, E extends Exception> {
        R apply(T t) throws E;
    }

    @FunctionalInterface
    public interface ExceptionRunnable<E extends Exception> {
        void run() throws E, CheckCodeException;
    }

    @FunctionalInterface
    public interface ExceptionConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    @FunctionalInterface
    public interface ExceptionSupplier<T, E extends Exception> {
        T get() throws E, CheckCodeException;
    }

    public static <T, E extends Exception> Consumer<T> once(ExceptionConsumer<T, E> consumer) throws E {
        return t -> {
            try {
                consumer.accept(t);
            } catch (Exception e) {
                throwAsUnchecked(e);
            }
        };
    }

    public static <T, R> Function<T, R> ever(ExceptionFunction<T, R, CheckCodeException> function, CheckCodeCallBack<?> checkCodeCallBack) {
        return t -> {
            while (true)
                try {
                    return function.apply(t);
                } catch (CheckCodeException e) {
                    checkCodeCallBack.onCheckCode(e.getUrl());
                }
        };
    }

    public static void ever(ExceptionRunnable<CheckCodeException> runnable, CheckCodeCallBack<?> checkCodeCallBack) {
        while (true)
            try {
                runnable.run();
                break;
            } catch (CheckCodeException e) {
                checkCodeCallBack.onCheckCode(e.getUrl());
            }
    }

    public static <T> T ever(ExceptionSupplier<T, CheckCodeException> supplier, CheckCodeCallBack<?> checkCodeCallBack) {
        while (true)
            try {
                return supplier.get();
            } catch (CheckCodeException e) {
                checkCodeCallBack.onCheckCode(e.getUrl());
            }
    }

    @SuppressWarnings("unchecked")
    public static <T> T ever(ExceptionSupplier<T, WrongAccountException> supplier, CheckCodeCallBack<?> checkCodeCallBack, String... extra) throws WrongAccountException {
        try {
            return supplier.get();
        } catch (CheckCodeException e) {
            return (T) checkCodeCallBack.onCheckCode(e.getUrl(), extra[0], extra[1], extra[2]);
        }
    }

    public static <T> T ever(ExceptionSupplier<T, WrongAccountException> supplier, CheckCodeCallBack<?> checkCodeCallBack, HomeworkQuizConfig homeworkQuizConfig, String... extra) throws WrongAccountException {
        while (true)
            try {
                return supplier.get();
            } catch (CheckCodeException e) {
                homeworkQuizConfig.setEnc(((CheckCodeData) checkCodeCallBack.onCheckCode(e.getUrl(), extra[0], extra[1], extra[2])).getEnc());
            } catch (Exception e) {
                throwAsUnchecked(e);
            }
    }

    public static <T> T ever(ExceptionSupplier<T, CheckCodeException> supplier, CheckCodeCallBack<?> checkCodeCallBack, ExamQuizConfig examQuizConfig, String... extra) {
        while (true)
            try {
                return supplier.get();
            } catch (CheckCodeException e) {
                examQuizConfig.setEnc(((CheckCodeData) checkCodeCallBack.onCheckCode(e.getUrl(), extra[0], extra[1], extra[2])).getEnc());
            }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Exception> void throwAsUnchecked(Exception exception) throws E {
        throw (E) exception;
    }
}

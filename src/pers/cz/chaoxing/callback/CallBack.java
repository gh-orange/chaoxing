package pers.cz.chaoxing.callback;

import net.dongliu.requests.Session;

import java.util.Arrays;

public interface CallBack<T> {
    T call(Session session, String... param);

    default void print(String first, String... more) {
        System.out.println(Thread.currentThread().getName() + ": " + first);
        Arrays.stream(more).forEach(System.out::println);
    }

    default void print(String first, String second, String... more) {
        this.print(first + "\n" + second, more);
    }
}

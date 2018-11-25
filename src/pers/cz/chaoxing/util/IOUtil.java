package pers.cz.chaoxing.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 橙子
 * @since 2018/10/25
 */
public final class IOUtil {
    private static Queue<Optional<String>> inputData;
    private static ReentrantReadWriteLock lock;
    private static Condition condition;

    public static String next() {
        try {
            IOUtil.lock.writeLock().lock();
            while (IOUtil.inputData.isEmpty())
                IOUtil.condition.await();
            String result = IOUtil.inputData.poll().orElse("");
            IOUtil.inputData.poll();
            return result;
        } catch (InterruptedException ignored) {
            return "";
        } finally {
            IOUtil.lock.writeLock().unlock();
        }
    }

    public static String nextLine() {
        try {
            IOUtil.lock.writeLock().lock();
            while (IOUtil.inputData.isEmpty())
                IOUtil.condition.await();
            StringBuilder stringBuilder = new StringBuilder();
            while (Objects.requireNonNull(IOUtil.inputData.peek()).isPresent())
                IOUtil.inputData.poll().ifPresent(stringBuilder::append);
            IOUtil.inputData.poll();
            return stringBuilder.toString();
        } catch (InterruptedException ignored) {
            return "";
        } finally {
            IOUtil.lock.writeLock().unlock();
        }
    }

    public static int nextInt() {
        try {
            IOUtil.lock.writeLock().lock();
            String result;
            do {
                while (IOUtil.inputData.isEmpty())
                    IOUtil.condition.await();
                result = IOUtil.inputData.poll().orElse("");
                IOUtil.inputData.poll();
            } while (!result.matches("\\d+"));
            return Integer.valueOf(result);
        } catch (InterruptedException ignored) {
            return 0;
        } finally {
            IOUtil.lock.writeLock().unlock();
        }
    }

    public static void print(String str) {
        IOUtil.lock.readLock().lock();
        System.out.print(str);
        IOUtil.lock.readLock().unlock();
    }

    public static void println(String first, String... more) {
        IOUtil.lock.readLock().lock();
        System.out.println(first);
        Arrays.stream(more).forEach(System.out::println);
        IOUtil.lock.readLock().unlock();
    }

    static {
        IOUtil.inputData = new LinkedList<>();
        IOUtil.lock = new ReentrantReadWriteLock();
        IOUtil.condition = IOUtil.lock.writeLock().newCondition();
    }

    public static final class ScanJob implements Runnable, Closeable {
        private BufferedReader reader;
        private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\p{javaWhitespace}+");

        public ScanJob() {
            this.reader = new BufferedReader(new InputStreamReader(System.in));
        }

        @Override
        public void run() {
            try {
                String line;
                do {
                    line = readLine();
                    try {
                        IOUtil.lock.writeLock().lock();
                        Matcher matcher = WHITESPACE_PATTERN.matcher(line);
                        for (String s : line.split(WHITESPACE_PATTERN.pattern())) {
                            IOUtil.inputData.offer(Optional.of(s));
                            if (matcher.find())
                                IOUtil.inputData.offer(Optional.of(matcher.group()));
                        }
                        IOUtil.inputData.offer(Optional.empty());
                        IOUtil.condition.signal();
                    } finally {
                        IOUtil.lock.writeLock().unlock();
                    }
                } while (!line.equals("exit"));
            } catch (InterruptedException ignored) {
            }
        }

        @Override
        public void close() throws IOException {
            this.reader.close();
        }

        private String readLine() throws InterruptedException {
            try {
                while (!reader.ready())
                    Thread.sleep(100);
                return reader.readLine();
            } catch (IOException e) {
                throw new InterruptedException();
            }
        }
    }
}

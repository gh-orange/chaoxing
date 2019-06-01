package pers.cz.chaoxing.util.io;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author 橙子
 * @since 2018/10/25
 */
public final class IOUtil {
    private static ResourceBundle resourceBundle;
    private static Queue<Optional<String>> inputData;
    private static ReadWriteLock lock;
    private static Condition condition;

    public static String next() {
        return IOUtil.printAndNext("");
    }

    public static String nextLine() {
        return IOUtil.printAndNextLine("");
    }

    public static int nextInt() {
        return IOUtil.printAndNextInt("");
    }

    public static String printAndNext(String promptKey, Object... args) {
        IOUtil.lock.writeLock().lock();
        try {
            if (!promptKey.isEmpty())
                IOUtil.print(promptKey, args);
            while (IOUtil.inputData.isEmpty())
                IOUtil.condition.await();
            String result = IOUtil.inputData.poll().orElse("");
            IOUtil.inputData.poll();
            return result;
        } catch (InterruptedException e) {
            return "";
        } finally {
            IOUtil.lock.writeLock().unlock();
        }
    }

    public static String printAndNextLine(String promptKey, Object... args) {
        IOUtil.lock.writeLock().lock();
        try {
            if (!promptKey.isEmpty())
                IOUtil.print(promptKey, args);
            while (IOUtil.inputData.isEmpty())
                IOUtil.condition.await();
            StringBuilder stringBuilder = new StringBuilder();
            while (Objects.requireNonNull(IOUtil.inputData.peek()).isPresent())
                IOUtil.inputData.poll().ifPresent(stringBuilder::append);
            IOUtil.inputData.poll();
            return stringBuilder.toString();
        } catch (InterruptedException e) {
            return "";
        } finally {
            IOUtil.lock.writeLock().unlock();
        }
    }

    public static int printAndNextInt(String promptKey, Object... args) {
        IOUtil.lock.writeLock().lock();
        try {
            if (!promptKey.isEmpty())
                IOUtil.print(promptKey, args);
            String result;
            do {
                while (IOUtil.inputData.isEmpty())
                    IOUtil.condition.await();
                result = IOUtil.inputData.poll().orElse("");
                IOUtil.inputData.poll();
            } while (!result.matches("-?\\d+"));
            return Integer.valueOf(result);
        } catch (InterruptedException e) {
            return 0;
        } finally {
            IOUtil.lock.writeLock().unlock();
        }
    }

    public static void print(String key, Object... args) {
        if (key.isEmpty())
            return;
        IOUtil.lock.readLock().lock();
        try {
            System.out.print(String.format(IOUtil.resourceBundle.getString(key), args));
        } finally {
            IOUtil.lock.readLock().unlock();
        }
    }

    public static void println(String key, Object... args) {
        println(key, args, new String[0]);
    }

    public static void println(String key, Object[] args, String... lines) {
        IOUtil.lock.readLock().lock();
        try {
            String format = IOUtil.resourceBundle.getString(key);
            if (key.startsWith("thread_"))
                format = IOUtil.resourceBundle.getString("thread_prefix") + format;
            System.out.println(String.format(format, args));
            Arrays.stream(lines).forEach(System.out::println);
        } finally {
            IOUtil.lock.readLock().unlock();
        }
    }

    static {
        IOUtil.resourceBundle = ResourceBundle.getBundle("strings");
        IOUtil.inputData = new LinkedList<>();
        IOUtil.lock = new ReentrantReadWriteLock();
        IOUtil.condition = IOUtil.lock.writeLock().newCondition();
    }

    public static final class ScanJob implements Runnable, Closeable {
        private Scanner scanner;
        private InputFilter inputFilter;
        private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\p{javaWhitespace}+");

        public ScanJob() {
            scanner = new Scanner(System.in);
        }

        public void setInputFilter(InputFilter inputFilter) {
            this.inputFilter = inputFilter;
        }

        @Override
        public void run() {
            String line;
            do {
                line = scanner.nextLine();
                if (Optional.ofNullable(inputFilter).isPresent())
                    if (inputFilter.doFilter(line))
                        continue;
                IOUtil.lock.writeLock().lock();
                try {
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
            } while (!"s".equalsIgnoreCase(line));
        }

        @Override
        public void close() {
            System.exit(0);
            scanner.close();
        }
    }
}

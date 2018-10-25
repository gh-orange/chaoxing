package pers.cz.chaoxing.util;

/**
 * @author 橙子
 * @date 2018/10/25
 */
public class StringUtil {
    public static String subStringAfterFirst(String origin, String begin) {
        int beginIndex = origin.indexOf(begin);
        if (-1 != beginIndex)
            return origin.substring(beginIndex + begin.length());
        else
            return origin;
    }

    public static String subStringAfterLast(String origin, String begin) {
        int beginIndex = origin.lastIndexOf(begin);
        if (-1 != beginIndex)
            return origin.substring(beginIndex + begin.length());
        else
            return origin;
    }

    public static String subStringBeforeFirst(String origin, String end) {
        int endIndex = origin.indexOf(end);
        if (-1 != endIndex)
            return origin.substring(0, endIndex);
        else
            return origin;
    }

    public static String subStringBeforeLast(String origin, String end) {
        int endIndex = origin.lastIndexOf(end);
        if (-1 != endIndex)
            return origin.substring(0, endIndex);
        else
            return origin;
    }

    public static String subStringBetweenFirst(String origin, String begin, String end) {
        String subStr = StringUtil.subStringAfterFirst(origin, begin);
        if (subStr.equals(origin))
            return "";
        subStr = StringUtil.subStringBeforeFirst(subStr, end);
        if (subStr.equals(origin))
            return "";
        return subStr;
    }

    public static String subStringBetweenLast(String origin, String begin, String end) {
        String subStr = StringUtil.subStringAfterLast(origin, begin);
        if (subStr.equals(origin))
            return "";
        subStr = StringUtil.subStringBeforeFirst(subStr, end);
        if (subStr.equals(origin))
            return "";
        return subStr;
    }
}

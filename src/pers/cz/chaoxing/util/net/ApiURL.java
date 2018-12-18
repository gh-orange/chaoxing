package pers.cz.chaoxing.util.net;

import java.util.Optional;

/**
 * @author 橙子
 * @since 2018/12/5
 */
public enum ApiURL {
    LOGIN_NORMAL_ABS("http://passport2.chaoxing.com/login"),
    LOGIN_SCHOOL_ABS(
            "http://passport2.chaoxing.com/login",
            "fid={}"
    ),
    LOGIN_CHECK_CODE(
            "/num/code",
            "{}"
    ),
    SEARCH_SCHOOL("/org/searchforms"),
    TASK_INFO(
            "/knowledge/cards",
            "clazzid={}&courseid={}&knowledgeid={}&num={}&v=20160407-1"
    ),
    TITLE_INFO("/mycourse/studentstudyAjax"),
    LIST_INFO(
            "/mycourse/studentstudycourselist",
            "clazzid={}&courseId={}&chapterId={}"
    ),
    VIDEO_INFO(
            "/ananas/status/{}",
            "k={}&_dc={}"
    ),
    PLAY_VALIDATE(
            "/edit/validatejobcount",
            "nodeid={}"
    ),
    HOMEWORK_VALIDATE(
            "/work/validate",
            "classId={}&courseId={}&_={}"
    ),
    EXAM_VALIDATE(
            "/exam/test/isExpire",
            "classId={}&courseId={}&id={}&endTime={}&moocTeacherId={}"
    ),
    HOMEWORK_QUIZ(
            "/api/work",
            "clazzId={}&courseid={}&knowledgeid={}&workId={}&jobid={}&enc={}&utenc={}&type={}&api=1&needRedirect=true&ut=s"
    ),
    EXAM_QUIZ(
            "/exam/test/reVersionTestStartNew",
            "classId={}&courseId={}&tId={}&id={}&examsystem={}&enc={}&start={}&remainTimeParam={}&relationAnswerLastUpdateTime{}&p=1&getTheNextQuestion=1&keyboardDisplayRequiresUserAction=1"
    ),
    HOMEWORK_CHECK_CODE_VALIDATE(
            "/img/ajaxValidate",
            "code={}"
    ),
    HOMEWORK_CHECK_CODE_STATUS(
            "/edit/selfservice",
            "clazzid={}&courseId={}&nodeid={}&code={}"
    ),
    HOMEWORK_CHECK_CODE_IMG("/img/code"),
    HOMEWORK_CHECK_CODE_SEND(
            "/img/ajaxValidate2",
            "code={}"
    ),
    EXAM_CHECK_CODE_IMG("/verifyCode/stuExam"),
    EXAM_CHECK_CODE_SEND(
            "/exam/test/getIdentifyCode",
            "classId={}&courseId={}&id={}&callback={}&inpCode={}"
    ),
    ANSWER_QUIZ("https://m.3gmfw.cn/so/{}/");

    private final String url;
    private final String params;

    ApiURL(String url) {
        this(url, "");
    }

    ApiURL(String url, String params) {
        this.url = url;
        this.params = params;
    }

    public String buildURL(String... paramValues) {
        StringBuilder url = new StringBuilder(this.url);
        StringBuilder params = new StringBuilder(this.params);
        int index;
        for (String paramValue : paramValues) {
            paramValue = Optional.ofNullable(paramValue).orElse("");
            if ('/' == url.charAt(0))
                url.insert(0, paramValue);
            else if (-1 != (index = url.indexOf("{}")))
                url.replace(index, index + 2, paramValue);
            else if (-1 != (index = params.indexOf("{}")))
                params.replace(index, index + 2, paramValue);
        }
        if (0 == params.length())
            return url.toString();
        return url + "?" + params;
    }
}
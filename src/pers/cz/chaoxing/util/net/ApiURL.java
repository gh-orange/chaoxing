package pers.cz.chaoxing.util.net;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
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
    SCHOOL_SEARCH("/org/searchforms"),
    SCHOOL_NOTICE_ABS(
            "http://passport2.chaoxing.com/api/notice",
            "fid={}"
    ),
    TITLE_INFO("/mycourse/studentstudyAjax"),
    CHAPTER_LOCK_OPEN(
            "/edit/openlock",
            "clazzid={}&courseId={}&nodeid={}"
    ),
    CHAPTER_CHANGE("/mycourse/changeCapter"),
    TASK_INFO(
            "/knowledge/cards",
            "clazzid={}&courseid={}&knowledgeid={}&cpi={}&num={}&ut=s&v=20160407-1"
    ),
    LIST_INFO(
            "/mycourse/studentstudycourselist",
            "clazzid={}&courseId={}&chapterId={}"
    ),
    VIDEO_INFO(
            "/ananas/status/{}",
            "k={}&_dc={}"
    ),
    READ_INFO(
            "/readv2job/preview",
            "clazzId={}&courseid={}&knowledgeid={}&jobid={}&enc={}&utenc={}&type=read&ut=s&isphone=false"
    ),
    PLAYER_VALIDATE(
            "/edit/validatejobcount",
            "clazzid={}&courseId={}&nodeid={}"
    ),
    READ_VALIDATE(
            "/ananas/job/readv2",
            "clazzid={}&courseid={}&knowledgeid={}&jobid={}&jtoken={}&_dc={}"
    ),
    HOMEWORK_VALIDATE(
            "/work/validate",
            "classId={}&courseId={}&_={}"
    ),
    EXAM_VALIDATE(
            "/exam/test/isExpire",
            "classId={}&courseId={}&id={}&endTime={}&moocTeacherId={}&cpi={}"
    ),
    READ_OPEN(
            "/course/{}.html",
            "_from_={}"
    ),
    READ_CARD(
            "/zt/getcards",
            "courseid={}&knowledgeid={}&_from_={}"
    ),
    READ_PROCESS(
            "/multimedia/readlog",
            "courseid={}&chapterid={}&_from_={}&height={}&h={}"
    ),
    VIDEO_PROCESS(
            "{}",
            "clazzId={}&objectId={}&userid={}&jobid={}&otherInfo={}&playingTime={}&isdrag={}&duration={}&clipTime={}&dtype={}&rt={}&enc={}&_t={}&view=pc"
    ),
    VIDEO_QUIZ(
            "{}",
            "mid={}&start=undefined"
    ),
    HOMEWORK_QUIZ(
            "/api/work",
            "clazzId={}&courseid={}&knowledgeid={}&workId={}&jobid={}&enc={}&utenc={}&type={}&api=1&needRedirect=true&ut=s"
    ),
    EXAM_QUIZ(
            "/exam/test/reVersionTestStartNew",
            "classId={}&courseId={}&tId={}&id={}&examsystem={}&enc={}&start={}&remainTimeParam={}&relationAnswerLastUpdateTime={}&cpi={}&p=1&getTheNextQuestion=1&keyboardDisplayRequiresUserAction=1"
    ),
    VIDEO_ANSWER(
            "{}",
            "resourceid={}&answer={}"
    ),
    HOMEWORK_ANSWER(
            "{}",
            "ua=pc&fromType=post&saveStatus=1&version={}&pos={}&rd={}&value={}&wid={}"
    ),
    EXAM_ANSWER(
            "{}",
            "tempSave={}&version={}&pos={}&rd={}&value={}&qid={}"
    ),
    CHAPTER_CHECK_CODE_IMG(
            "/verifyCode/studychapter",
            "cpi={}"
    ),
    CUSTOM_CHECK_CODE_SEND(
            "{}",
            "ucode={}"
    ),
    HOMEWORK_CHECK_CODE_VALIDATE(
            "/kaptcha-img/ajaxValidate",
            "code={}"
    ),
    HOMEWORK_CHECK_CODE_STATUS(
            "/edit/selfservice",
            "clazzid={}&courseId={}&nodeid={}&code={}"
    ),
    HOMEWORK_CHECK_CODE_IMG("/kaptcha-img/code"),
    HOMEWORK_CHECK_CODE_SEND(
            "/kaptcha-img/ajaxValidate2",
            "code={}"
    ),
    EXAM_CHECK_CODE_IMG("/verifyCode/stuExam"),
    EXAM_CHECK_CODE_SEND(
            "/exam/test/getIdentifyCode",
            "classId={}&courseId={}&id={}&callback={}&inpCode={}"
    ),
    ANSWER_QUIZ_3GMFW_ABS("https://m.3gmfw.cn/so/{}/"),
    ANSWER_QUIZ_TF_ABS(
            "https://api.tensor-flow.club:8700/cx",
            "question={}"
    );

    private final String url;
    private final String params;

    ApiURL(String url) {
        this(url, "");
    }

    ApiURL(String url, String params) {
        this.url = url;
        this.params = params;
    }

    public String buildURL(Object... paramValues) {
        StringBuilder url = new StringBuilder(this.url);
        StringBuilder params = new StringBuilder(this.params);
        int index;
        boolean hasParam = false;
        for (Object paramValue : paramValues) {
            String paramValueStr = Optional.ofNullable(paramValue).map(Object::toString).orElse("");
            switch (url.charAt(0)) {
                case '{':
                    hasParam = paramValueStr.indexOf('?') != -1;
                    url.replace(0, 2, paramValueStr);
                    break;
                case '.':
                    url.replace(0, 1, paramValueStr);
                    break;
                case '/':
                    URI uri = URI.create(paramValueStr);
                    url.insert(0, uri.getScheme() + "://" + uri.getRawAuthority());
                    break;
                default:
                    try {
                        paramValueStr = URLEncoder.encode(paramValueStr, "utf-8");
                    } catch (UnsupportedEncodingException ignored) {
                    }
                    if (-1 != (index = url.indexOf("{}")))
                        url.replace(index, index + 2, paramValueStr);
                    else if (-1 != (index = params.indexOf("{}")))
                        params.replace(index, index + 2, paramValueStr);
                    break;
            }
        }
        if (0 == params.length())
            return url.toString();
        return url.append(hasParam ? '&' : '?').append(params).toString();
    }
}
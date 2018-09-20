package pers.cz.chaoxing.util;

import com.alibaba.fastjson.*;
import net.dongliu.requests.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.quiz.*;
import pers.cz.chaoxing.common.quiz.ExamQuizInfo;
import pers.cz.chaoxing.common.quiz.HomeworkQuizInfo;
import pers.cz.chaoxing.common.quiz.PlayerQuizInfo;
import pers.cz.chaoxing.common.task.*;
import pers.cz.chaoxing.common.task.data.TaskData;
import pers.cz.chaoxing.common.task.data.exam.ExamData;
import pers.cz.chaoxing.common.task.data.exam.ExamDataProperty;
import pers.cz.chaoxing.common.task.data.homework.HomeworkData;
import pers.cz.chaoxing.common.task.data.player.PlayerData;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.Proxy;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CXUtil {

    private static final Type taskInfoType = new TypeReference<TaskInfo<TaskData>>() {
    }.getType();

    private static final Type playerInfoType = new TypeReference<TaskInfo<PlayerData>>() {
    }.getType();

    private static final Type homeworkInfoType = new TypeReference<TaskInfo<HomeworkData>>() {
    }.getType();

    private static Session session = Requests.session();

    //    public static Proxy proxy = Proxies.httpProxy("10.14.36.103", 8080);
    public static Proxy proxy = null;

    public static boolean login(String username, String password, String checkCode) throws WrongAccountException {
        String indexUri = session.get("http://dlnu.fy.chaoxing.com/topjs?index=1").proxy(proxy).send().readToText();
        String beginStr = "location.href = \\\"";
        String endStr = "\\\"";
        int beginIndex = indexUri.indexOf(beginStr) + beginStr.length();
        Document document = Jsoup.parse(session.get(indexUri.substring(beginIndex, indexUri.indexOf(endStr, beginIndex))).proxy(proxy).send().readToText());
        Map<String, String> postBody = new HashMap<>();
        postBody.put("refer_0x001", document.getElementById("refer_0x001").val());
        postBody.put("pid", document.getElementById("pid").val());
        postBody.put("pidName", document.getElementById("pidName").val());
        postBody.put("fid", document.getElementById("fid").val());
        postBody.put("fidName", document.getElementById("fidName").val());
        postBody.put("allowJoin", document.select("input[name=allowJoin]").val());
        postBody.put("isCheckNumCode", document.select("input[name=isCheckNumCode]").val());
        postBody.put("f", document.select("input[name=f]").val());
        postBody.put("productid", document.getElementById("productid").val());
        postBody.put("verCode", document.getElementById("verCode").val());
        postBody.put("uname", username);
        postBody.put("password", password);
        postBody.put("numcode", checkCode);
        String responseStr = session.post("http://passport2.chaoxing.com/login?refer=http://i.mooc.chaoxing.com/space/index.shtml").body(postBody).proxy(proxy).send().readToText();
        if (responseStr.contains("密码错误"))
            throw new WrongAccountException();
        return !responseStr.contains("用户登录");
    }

    public static String getClassesUri() throws CheckCodeException {
        RawResponse response = session.get("http://i.mooc.chaoxing.com/space/index.shtml").followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        String src = Jsoup.parse(response.readToText()).select("div.mainright iframe").attr("src");
        for (int i = 0; i < 2; i++) {
            response = session.get(src).followRedirect(false).proxy(proxy).send();
            if (response.getStatusCode() == StatusCodes.FOUND)
                src = response.getHeader("location");
            else
                break;
        }
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, src);
        return response.getURL();
    }

    public static List<String> getClasses(String uri) {
        RawResponse response = session.get(uri).proxy(proxy).send();
        Document document = Jsoup.parse(response.readToText());
        return document.select("div.httpsClass.Mconright a").eachAttr("href");
    }

    public static List<String> getTasks(String baseUri, String uri) {
        Document document = Jsoup.parse(session.get(baseUri + uri).proxy(proxy).send().readToText());
        /*
        return 'success'
         */
        String logUri = document.select("script[type=text/javascript]").last().attr("src");
        if (!logUri.isEmpty())
            session.get(logUri).proxy(proxy).send();
        Elements elements = new Elements();
        for (Element element : document.select("h3.clearfix"))
            if (!element.select("em.orange").text().isEmpty())
                elements.addAll(element.select("span.articlename a"));
        return elements.eachAttr("href");
    }

    public static List<String> getExams(String baseUri, String uri) {
//        Document document = Jsoup.parse(session.get(baseUri + uri).proxy(proxy).send().readToText());
//        return document.select("div.navshow ul li a:contains(考试)").eachAttr("data");
        List<String> exams = new ArrayList<>();
        exams.add("/exam/test" + uri.substring(uri.indexOf("?")).replaceAll("clazzid=", "classId="));
        return exams;
    }

    public static String getCardUriModel(String baseUri, String uri, Map<String, String> params) throws CheckCodeException {
        RawResponse response = session.get(baseUri + uri).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        String cardUri = response.readToText();
        String beginStr = "utEnc=\"";
        String endStr = "\";";
        int beginIndex = cardUri.indexOf(beginStr) + beginStr.length();
        params.put("utenc", cardUri.substring(beginIndex, cardUri.indexOf(endStr, beginIndex)));
        beginStr = "document.getElementById(\"iframe\").src=\"";
        endStr = "\";";
        beginIndex = cardUri.indexOf(beginStr) + beginStr.length();
        return cardUri.substring(beginIndex, cardUri.indexOf(endStr, beginIndex)).replaceAll("[\"+]", "");
    }

    /**
     * active task point color to green
     *
     * @param baseUri
     * @param params
     * @return
     * @throws CheckCodeException
     */
    public static boolean activeTask(String baseUri, Map<String, String> params) throws CheckCodeException {
        System.out.println(params.get("chapterId"));
        String src = baseUri + "/mycourse/studentstudy";
        RawResponse response = null;
        for (int i = 0; i < 2; i++) {
            response = session.get(src).params(params).followRedirect(false).proxy(proxy).send();
            if (response.getStatusCode() == StatusCodes.FOUND) {
                src = response.getHeader("location");
                if (src != null && !src.contains("study"))
                    throw new CheckCodeException(session, src);
            } else
                break;
        }
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, src);
        Element script = Jsoup.parse(response.readToText()).select("script[src~=https?://]").first();
        if (null == script)
            return false;
        response = session.get(script.attr("src")).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        return response.readToText().contains("success");
    }

    public static synchronized <T extends TaskData> TaskInfo<T> getTaskInfo(String baseUri, String cardUri, Map<String, String> params, InfoType infoType) throws CheckCodeException {
        if (infoType.equals(InfoType.Exam))
            return (TaskInfo<T>) getExamInfo(baseUri, cardUri, params);
//        session.post(baseUri + "/mycourse/studentstudyAjax").body(params).proxy(proxy).send();
        params.put("num", String.valueOf(infoType.getId()));
        for (Map.Entry<String, String> param : params.entrySet())
            cardUri = cardUri.replaceAll("(?i)=" + param.getKey(), "=" + param.getValue());
        RawResponse response = session.get(baseUri + cardUri).followRedirect(false).proxy(proxy).send();
//        session.get(baseUri + "/mycourse/studentstudycourselist").params(params).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        String responseStr = response.readToText();
        String beginStr = "mArg = ";
        String endStr = ";";
        int beginIndex = responseStr.indexOf(beginStr, responseStr.indexOf("try{")) + beginStr.length();
        try {
            switch (infoType) {
                case Video:
                    return JSON.parseObject(responseStr.substring(beginIndex, responseStr.indexOf(endStr, beginIndex)), playerInfoType);
                case Homework:
                    TaskInfo<HomeworkData> taskInfo = JSON.parseObject(responseStr.substring(beginIndex, responseStr.indexOf(endStr, beginIndex)), homeworkInfoType);
                    for (HomeworkData attachment : taskInfo.getAttachments())
                        attachment.setUtEnc(params.get("utenc"));
                    return (TaskInfo<T>) taskInfo;
                default:
                    return JSON.parseObject(responseStr.substring(beginIndex, responseStr.indexOf(endStr, beginIndex)), taskInfoType);
            }
        } catch (JSONException ignored) {
            TaskInfo<T> taskInfo = new TaskInfo<>();
            switch (infoType) {
                case Video:
                    /*
                    none player
                     */
                    PlayerData playerData = new PlayerData();
                    playerData.setPassed(true);
                    taskInfo.setAttachments((T[]) new PlayerData[]{playerData});
                    break;
                case Homework:
                    /*
                    none homework
                     */
                    HomeworkData homeworkData = new HomeworkData();
                    homeworkData.setUtEnc(params.get("utenc"));
                    taskInfo.setAttachments((T[]) new HomeworkData[]{homeworkData});
                    break;
            }
            return taskInfo;
        }
    }

    public static VideoInfo getVideoInfo(String baseUri, String uri, String objectId, String fid) {
        Map<String, String> params = new HashMap<>();
        params.put("k", fid);
        params.put("_dc", String.valueOf(System.currentTimeMillis()));
        return session.get(baseUri + uri + "/" + objectId).params(params).proxy(proxy).send().readToJson(VideoInfo.class);
    }

    public static boolean startExam(String baseUri, TaskInfo<ExamData> taskInfo, ExamData attachment) throws CheckCodeException {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("classId", taskInfo.getDefaults().getClazzId());
            params.put("courseId", taskInfo.getDefaults().getCourseid());
            params.put("id", attachment.getProperty().gettId());
            params.put("endTime", attachment.getProperty().getEndTime());
            params.put("moocTeacherId", attachment.getProperty().getMoocTeacherId());
            JSONObject result = JSON.parseObject(session.get(baseUri + "/exam/test/isExpire").params(params).proxy(proxy).send().readToText());
            switch (result.getInteger("status")) {
                case 0:
                    System.out.println("Exam need finishStandard:" + params.get("title") + "[" + result.getInteger("finishStandard") + "%]");
                    break;
                case 1:
                    return true;
                case 2:
                    throw new CheckCodeException(session, baseUri + "/verifyCode/stuExam");
                default:
                    break;
            }
        } catch (JSONException ignored) {
        }
        return false;
    }

    /**
     * call since player loaded
     *
     * @param baseUri
     * @param params
     * @return
     */
    public static boolean startRecord(String baseUri, Map<String, String> params) {
        params.put("nodeid", params.get("chapterId"));
        return session.get(baseUri + "/edit/validatejobcount").params(params).proxy(proxy).send().readToText().contains("true");
    }

    /**
     * call since player loaded first
     *
     * @param taskInfo
     * @param videoInfo
     * @return
     * @throws CheckCodeException
     */
    public static boolean onStart(TaskInfo<PlayerData> taskInfo, PlayerData attachment, VideoInfo videoInfo) throws CheckCodeException {
        return sendLog(taskInfo, attachment, videoInfo, (int) (attachment.getHeadOffset() / 1000), 3);
    }

    /**
     * call since player finished
     *
     * @param taskInfo
     * @param videoInfo
     * @return
     * @throws CheckCodeException
     */
    public static boolean onEnd(TaskInfo taskInfo, PlayerData attachment, VideoInfo videoInfo) throws CheckCodeException {
        return sendLog(taskInfo, attachment, videoInfo, videoInfo.getDuration(), 4);
    }

    /**
     * call since player clicked to play
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     * @throws CheckCodeException
     */
    public static boolean onPlay(TaskInfo taskInfo, PlayerData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        return sendLog(taskInfo, attachment, videoInfo, playSecond, 3);
    }

    /**
     * call since player clicked to pause
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     * @throws CheckCodeException
     */
    public static boolean onPause(TaskInfo taskInfo, PlayerData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        if (taskInfo.getDefaults().getChapterId() != null && !taskInfo.getDefaults().getChapterId().isEmpty())
            return sendLog(taskInfo, attachment, videoInfo, playSecond, 2);
        return false;
    }

    /**
     * call each intervalTime since player playing
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     * @throws CheckCodeException
     */
    public static boolean onPlayProgress(TaskInfo taskInfo, PlayerData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        return sendLog(taskInfo, attachment, videoInfo, playSecond, 0);
    }

    /**
     * call since player start playing
     *
     * @param initDataUrl
     * @param mid
     * @return
     * @throws CheckCodeException
     */
    public static List<PlayerQuizInfo> getPlayerQuizzes(String initDataUrl, String mid) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("mid", mid);
        params.put("start", "undefined");
        RawResponse response = session.get(initDataUrl).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        return JSONArray.parseArray(response.readToText(), PlayerQuizInfo.class);
    }

    public static HomeworkQuizInfo getHomeworkQuizzes(String baseUri, TaskInfo<HomeworkData> taskInfo, HomeworkData attachment) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("api", "1");
        params.put("needRedirect", "true");
        params.put("workId", attachment.getProperty().getWorkid());
        params.put("jobid", attachment.getJobid());
        params.put("knowledgeid", taskInfo.getDefaults().getKnowledgeid());
        /*
        teacher or student
         */
        params.put("ut", "s");
        params.put("courseid", taskInfo.getDefaults().getCourseid());
        params.put("clazzId", taskInfo.getDefaults().getClazzId());
        params.put("type", "workB".equals(attachment.getProperty().getWorktype()) ? "b" : "");
        params.put("enc", attachment.getEnc());
        params.put("utenc", attachment.getUtEnc());
        RawResponse response = null;
        String src = baseUri + "/api/work";
        for (int i = 0; i < 3; i++) {
            response = session.get(src).params(params).followRedirect(false).proxy(proxy).send();
            if (response.getStatusCode() == StatusCodes.FOUND) {
                src = response.getHeader("location");
                if (src != null && !src.contains("work"))
                    throw new CheckCodeException(session, src);
            } else
                break;
        }
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, src);
        String responseStr = response.readToText();
        Elements elements = Jsoup.parse(responseStr).select("div.CeYan");
        FormElement form = elements.select("form#form1").forms().get(0);
        Elements questions = form.select("div.TiMu");
        HomeworkQuizInfo homeworkQuizInfo = new HomeworkQuizInfo();
        homeworkQuizInfo.setPassed(!elements.select("div.ZyTop h3 span").text().contains("待做"));
        String beginStr = "= \"";
        String endStr = "\"";
        int beginIndex = responseStr.lastIndexOf(beginStr, responseStr.indexOf("$(\"#answerwqbid\")")) + beginStr.length();
        homeworkQuizInfo.setDatas(new QuizConfig[questions.size()]);
        homeworkQuizInfo.setAnswerwqbid(responseStr.substring(beginIndex, responseStr.indexOf(endStr, beginIndex)));
        homeworkQuizInfo.setPyFlag(form.getElementById("pyFlag").val());
        homeworkQuizInfo.setCourseId(form.getElementById("courseId").val());
        homeworkQuizInfo.setClassId(form.getElementById("classId").val());
        homeworkQuizInfo.setApi(form.getElementById("api").val());
        homeworkQuizInfo.setWorkAnswerId(form.getElementById("workAnswerId").val());
        homeworkQuizInfo.setTotalQuestionNum(form.getElementById("totalQuestionNum").val());
        homeworkQuizInfo.setFullScore(form.getElementById("fullScore").val());
        homeworkQuizInfo.setKnowledgeid(form.getElementById("knowledgeid").val());
        homeworkQuizInfo.setOldSchoolId(form.getElementById("oldSchoolId").val());
        homeworkQuizInfo.setOldWorkId(form.getElementById("oldWorkId").val());
        homeworkQuizInfo.setJobid(form.getElementById("jobid").val());
        homeworkQuizInfo.setWorkRelationId(form.getElementById("workRelationId").val());
        homeworkQuizInfo.setEnc(form.getElementById("enc").val());
        homeworkQuizInfo.setEnc_work(form.getElementById("enc_work").val());
        homeworkQuizInfo.setUserId(form.getElementById("userId").val());
        for (int i = 0; i < questions.size(); i++) {
            homeworkQuizInfo.getDatas()[i] = new QuizConfig();
            homeworkQuizInfo.getDatas()[i].setValidationUrl(baseUri + "/work/" + form.attr("action"));
            homeworkQuizInfo.getDatas()[i].setAnswered(false);
            homeworkQuizInfo.getDatas()[i].setDescription(questions.get(i).select("div.Zy_TItle div.clearfix").first().text());
            Element inputAnswerType = questions.get(i).select("input[id~=answertype]").first();
            Element inputAnswerCheck = inputAnswerType.previousElementSibling();
            homeworkQuizInfo.getDatas()[i].setMemberinfo(inputAnswerType.id());
            if (inputAnswerCheck.tagName().equals("input"))
                homeworkQuizInfo.getDatas()[i].setAnswerCheck(inputAnswerCheck.attr("name"));
            homeworkQuizInfo.getDatas()[i].setQuestionType(inputAnswerType.val());
            Elements lis = questions.get(i).getElementsByTag("ul").first().getElementsByTag("li");
            homeworkQuizInfo.getDatas()[i].setOptions(new OptionInfo[lis.size()]);
            for (int j = 0; j < lis.size(); j++) {
                Element inputAnswer = lis.get(j).selectFirst("label input");
                if (homeworkQuizInfo.getDatas()[i].getResourceId() == null || homeworkQuizInfo.getDatas()[i].getResourceId().isEmpty())
                    homeworkQuizInfo.getDatas()[i].setResourceId(inputAnswer.attr("name"));
                homeworkQuizInfo.getDatas()[i].getOptions()[j] = new OptionInfo();
                homeworkQuizInfo.getDatas()[i].getOptions()[j].setRight(inputAnswer.hasAttr("checked"));
                if (homeworkQuizInfo.getDatas()[i].getOptions()[j].isRight())
                    homeworkQuizInfo.getDatas()[i].setAnswered(true);
                homeworkQuizInfo.getDatas()[i].getOptions()[j].setName(inputAnswer.val());
                if (lis.isEmpty())
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription("");
                else
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription(lis.get(j).select("a").text());
                if (homeworkQuizInfo.getDatas()[i].getOptions()[j].getDescription().isEmpty())
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription(homeworkQuizInfo.getDatas()[i].getOptions()[j].getName());
            }
        }
        return homeworkQuizInfo;
    }

    public static ExamQuizInfo getExamQuizzes(String baseUri, TaskInfo<ExamData> examInfo, ExamData attachment) throws CheckCodeException {
        ExamQuizInfo examQuizInfo = new ExamQuizInfo();
        examQuizInfo.setClassId(examInfo.getDefaults().getClazzId());
        examQuizInfo.setCourseId(examInfo.getDefaults().getCourseid());
        examQuizInfo.settId(attachment.getProperty().gettId());
        examQuizInfo.setTestUserRelationId(attachment.getProperty().getId());
        examQuizInfo.setExamsystem(attachment.getProperty().getExamsystem());
        examQuizInfo.setEnc(attachment.getEnc());
        storeExamQuizzes(baseUri, examQuizInfo);
        return examQuizInfo;
    }

    public static boolean storeHomeworkQuizzes(String baseUri, HomeworkQuizInfo homeworkQuizInfo) throws WrongAccountException, CheckCodeException {
        homeworkQuizInfo.setPyFlag("1");
        return answerHomeworkQuizzes(baseUri, homeworkQuizInfo);
    }

    public static boolean storeExamQuizzes(String baseUri, ExamQuizInfo examQuizInfo) throws CheckCodeException {
        if (!storeExamQuiz(getExamQuiz(baseUri, 0, examQuizInfo), examQuizInfo))
            return false;
        for (int i = 0; i < examQuizInfo.getDatas().length; i++) {
            i = getExamQuiz(baseUri, i, examQuizInfo);
            if (!storeExamQuiz(i, examQuizInfo))
                return false;
        }
        return true;
    }

    public static boolean answerPlayerQuiz(String baseUri, String validationUrl, String resourceId, String answer) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("resourceid", resourceId);
        params.put("answer", "'" + answer + "'");
        RawResponse response = session.get(baseUri + validationUrl).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        JSONObject jsonObject = JSONObject.parseObject(response.readToText());
        return jsonObject.getString("answer").equals(answer) && jsonObject.getBoolean("isRight");
    }

    /**
     * JavaScript code:
     * function encrypt(h, g) {
     * if (g == null || g.length <= 0) {
     * return null
     * }
     * var m = "";
     * for (var d = 0; d < g.length; d++) {
     * m += g.charCodeAt(d).toString()
     * }
     * var j = Math.floor(m.length / 5);
     * var c = parseInt(m.charAt(j) + m.charAt(j * 2) + m.charAt(j * 3) + m.charAt(j * 4));
     * var a = Math.ceil(g.length / 2);
     * var k = Math.pow(2, 31) - 1;
     * if (c < 2) {
     * alert("Algorithm cannot find a suitable hash. Please choose a different password. \nPossible considerations are to choose a more complex or longer password.");
     * return null
     * }
     * var b = Math.random();
     * var e = Math.round(b * 1000000000) % 100000000;
     * m += e;
     * if (m.length > 10) {
     * m = parseInt(m.substring(0, 10)).toString()
     * }
     * m = (c * m + a) % k;
     * var f = "";
     * var l = "";
     * for (var d = 0; d < h.length; d++) {
     * f = parseInt(h.charCodeAt(d) ^ Math.floor((m / k) * 255));
     * if (f < 16) {
     * l += "0" + f.toString(16)
     * } else {
     * l += f.toString(16)
     * }
     * m = (c * m + a) % k
     * }
     * e = e.toString(16);
     * while (e.length < 8) {
     * e = "0" + e
     * }
     * l += e;
     * return l + "&rd=" + b
     * }
     * var __e = function() {
     * var g = {
     * "x": -1,
     * "y": -1
     * };
     * var c = document.getElementById("userId").value;
     * var a = document.getElementById("workRelationId").value;
     * var j = c + "_" + a;
     * try {
     * if (typeof(j) == "undefined") {
     * j = "axvP^&Sg"
     * }
     * var d = window.event;
     * if (typeof(d) == "undefined") {
     * var k = arguments.callee.caller,
     * l = k;
     * while (k != null) {
     * l = k;
     * k = k.caller
     * }
     * d = l.arguments[0]
     * }
     * if (d != null) {
     * var i = document.documentElement.scrollLeft || document.body.scrollLeft;
     * var h = document.documentElement.scrollTop || document.body.scrollTop;
     * g.x = d.pageX || d.clientX + i;
     * g.y = d.pageY || d.clientY + h
     * }
     * } catch (f) {
     * g = {
     * "x": -2,
     * "y": -2
     * }
     * }
     * var b = "(" + Math.ceil(g.x) + "|" + Math.ceil(g.y) + ")";
     * return encrypt(b, j) + "&value=" + b + "&wid=" + a
     * };
     * window.getEnc = function() {
     * return __e()
     * };
     */
    public static boolean answerHomeworkQuizzes(String baseUri, HomeworkQuizInfo homeworkQuizInfo) throws CheckCodeException, WrongAccountException {
        Map<String, String> params = new HashMap<>();
        params.put("courseId", homeworkQuizInfo.getCourseId());
        params.put("classId", homeworkQuizInfo.getClassId());
        /*
        cache false
         */
//        params.put("_", String.valueOf(System.currentTimeMillis()));
        if (homeworkQuizInfo.getEnc() == null || homeworkQuizInfo.getEnc().isEmpty())
            switch (JSONObject.parseObject(session.get(baseUri + "/work/validate").params(params).followRedirect(false).proxy(proxy).send().readToText()).getInteger("status")) {
                case 1:
                    throw new WrongAccountException();
                case 2:
                    throw new CheckCodeException(session, baseUri + "/img/code");
                case 3:
                    break;
                default:
                    return false;
            }
        int version = 1;
        Matcher matcher = Pattern.compile("version=(\\d)").matcher(homeworkQuizInfo.getDatas()[0].getValidationUrl());
        if (matcher.find())
            version += Integer.valueOf(matcher.group(1));
        params.clear();
        params.put("ua", "pc");
        params.put("formType", "post");
        params.put("saveStatus", "1");
        params.put("version", String.valueOf(version));
        //region skip when store
//        if (!homeworkQuizInfo.getPyFlag().equals("1")) {
        int pageWidth = 898;
        int pageHeight = 687;
        String value = "(" + pageWidth + "|" + pageHeight + ")";
        String uwId = homeworkQuizInfo.getUserId() + "_" + homeworkQuizInfo.getWorkRelationId();
//        if (uwId == null)
//            uwId = "axvP^&Sg";
        int uwIdLength = uwId.length() / 2 + ((uwId.length() % 2 == 0) ? 0 : 1);
        double random = Math.random();
//        double random = 0.03926823033418314;
        long randomMillion = Math.round(random * 1000000000) % 100000000;
        StringBuilder uwIdASCII = new StringBuilder();
        for (byte ascii : uwId.getBytes())
            uwIdASCII.append(ascii);
        StringBuilder multiplierStr = new StringBuilder();
        for (int i = 1; i <= 4; i++)
            multiplierStr.append(uwIdASCII.charAt(uwIdASCII.length() / 5 * i));
        int multiplier = Integer.valueOf(multiplierStr.toString());
//        if (multiplier < 2)
//            throw new IOException("Algorithm cannot find a suitable hash. Please choose a different password. \nPossible considerations are to choose a more complex or longer password.");
        uwIdASCII.append(randomMillion);
        if (uwIdASCII.length() > 10)
            uwIdASCII.setLength(10);
        long n = (multiplier * Long.valueOf(uwIdASCII.toString()) + uwIdLength) % Integer.MAX_VALUE;
        StringBuilder pos = new StringBuilder();
        for (char c : value.toCharArray()) {
            pos.append(String.format("%02x", c ^ Math.floorDiv(n * 255, Integer.MAX_VALUE)));
            n = (multiplier * n + uwIdLength) % Integer.MAX_VALUE;
        }
        pos.append(String.format("%08x", randomMillion));
        params.put("pos", pos.toString());
        params.put("rd", String.valueOf(random));
        params.put("value", value);
        params.put("wid", homeworkQuizInfo.getWorkRelationId());
//        }
        //endregion
        Map<String, String> body = new IdentityHashMap<>();
        body.put("pyFlag", homeworkQuizInfo.getPyFlag());
        body.put("courseId", homeworkQuizInfo.getCourseId());
        body.put("classId", homeworkQuizInfo.getClassId());
        body.put("api", homeworkQuizInfo.getApi());
        body.put("workAnswerId", homeworkQuizInfo.getWorkAnswerId());
        body.put("totalQuestionNum", homeworkQuizInfo.getTotalQuestionNum());
        body.put("fullScore", homeworkQuizInfo.getFullScore());
        body.put("knowledgeid", homeworkQuizInfo.getKnowledgeid());
        body.put("oldSchoolId", homeworkQuizInfo.getOldSchoolId());
        body.put("oldWorkId", homeworkQuizInfo.getOldWorkId());
        body.put("jobid", homeworkQuizInfo.getJobid());
        body.put("workRelationId", homeworkQuizInfo.getWorkRelationId());
        body.put("enc", homeworkQuizInfo.getEnc());
        body.put("enc_work", homeworkQuizInfo.getEnc_work());
        body.put("userId", homeworkQuizInfo.getUserId());
        body.put("answerwqbid", homeworkQuizInfo.getAnswerwqbid());
        for (QuizConfig quizConfig : homeworkQuizInfo.getDatas()) {
            StringBuilder answerStr = new StringBuilder();
            for (OptionInfo optionInfo : quizConfig.getOptions())
                if (optionInfo.isRight()) {
                    body.put(new String(quizConfig.getResourceId().getBytes()), optionInfo.getName());
                    if (null != quizConfig.getAnswerCheck() && !quizConfig.getAnswerCheck().isEmpty())
                        answerStr.append(optionInfo.getName());
                }
            if (null != quizConfig.getAnswerCheck() && !quizConfig.getAnswerCheck().isEmpty())
                body.put(quizConfig.getAnswerCheck(), answerStr.toString());
            if (quizConfig.getMemberinfo() != null && !quizConfig.getMemberinfo().isEmpty())
                body.put(quizConfig.getMemberinfo(), quizConfig.getQuestionType());
        }
        RawResponse response = session.post(homeworkQuizInfo.getDatas()[0].getValidationUrl()).params(params).body(body).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        String responseStr = response.readToText();
        return !responseStr.contains("提交失败");
    }

    public static boolean answerExamQuizzes(String baseUri, ExamQuizInfo examQuizInfo) throws CheckCodeException {
        if (!storeExamQuizzes(baseUri, examQuizInfo))
            return false;
        for (int i = 0; i < examQuizInfo.getDatas().length; i++) {
            if (null != examQuizInfo.getDatas()[i]) {
                examQuizInfo.setTempSave(false);
                examQuizInfo.getDatas()[i].setMemberinfo("");
                return answerExamQuiz(i, examQuizInfo);
            }
        }
        return true;
    }

    public static void saveCheckCode(String path) {
        session.get("http://passport2.chaoxing.com/num/code?" + System.currentTimeMillis()).proxy(proxy).send().writeToFile(path);
    }

    /**
     * Thanks to m.3gmfw.cn for database support
     *
     * @param quizConfig
     * @return
     */
    public static boolean getQuizAnswer(QuizConfig quizConfig) {
        String[] descriptions = quizConfig.getDescription().replaceAll("【.*?】", "").split("[\\pP\\pS\\pZ]");
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < (descriptions.length > 8 ? descriptions.length / 2 : descriptions.length); i++) {
            stringBuilder.append(descriptions[i]);
        }
        String description = stringBuilder.toString();
        try {
            description = URLEncoder.encode(description, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return false;
        }
        RawResponse response;
        Document document;
        Elements forms;
        /*
        circumvent protection
         */
        do {
            response = session.get("https://m.3gmfw.cn/so/" + description + "/").proxy(proxy).send();
            document = Jsoup.parse(response.charset("GBK").readToText());
            forms = document.select("form#challenge-form");
            if (forms.isEmpty())
                break;
            try {
                FormElement form = forms.forms().get(0);
                form.setBaseUri("https://m.3gmfw.cn/");
                String script = document.select("script").html();
                String beginStr = "setTimeout(function(){";
                String endStr = "f.submit();";
                int beginIndex = script.indexOf(beginStr) + beginStr.length();
                script = script.substring(beginIndex, script.indexOf(endStr, beginIndex));
                script = script.replaceAll("a\\.value", "var a");
                String[] strings = script.split("\n");
                script = "function jschl_answer(){" +
                        strings[1] +
                        "t = \"" + form.baseUri() + "\";" +
                        "r = t.match(/https?:\\/\\//)[0];" +
                        "t = t.substr(r.length);" +
                        "t = t.substr(0, t.length - 1);" +
                        strings[8] +
                        ";return a;" +
                        "}";
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName("Nashorn");
                engine.eval(script);
                double answer = (double) ((Invocable) engine).invokeFunction("jschl_answer");
                Map<String, String> params = new HashMap<>();
                params.put("jschl_answer", String.valueOf(answer));
                params.put("pass", form.select("input[name=pass]").val());
                params.put("jschl_vc", form.select("input[name=jschl_vc]").val());
                Thread.sleep(4 * 1000);
                session.get(form.absUrl("action")).params(params).proxy(proxy).send();
            } catch (Exception ignored) {
                break;
            }
        } while (!forms.isEmpty());
//        Element ul = document.selectFirst("ul.article-list");
//        if (null == ul)
//            return false;
//        Element li = ul.selectFirst("li");
//        document = Jsoup.parse(Requests.get("https://m.3gmfw.cn/" + li.selectFirst("a").attr("href")).proxy(proxy).send().charset("GBK").readToText());
        Element div = document.selectFirst("div.searchTopic");
        if (null == div)
            return false;
        document = Jsoup.parse(Requests.get("https://m.3gmfw.cn/" + div.selectFirst("a").attr("href")).proxy(proxy).send().charset("GBK").readToText());
        Elements p = document.select("div.content p");
        Map<String, String> answers = new HashMap<>();
        String rightAnswers = p.last().text();
        for (Element element : p)
            for (TextNode textNode : element.textNodes())
                if (!textNode.isBlank())
                    if (!textNode.text().trim().contains("答案：")) {
                        Matcher matcher = Pattern.compile("[a-zA-Z]").matcher(textNode.text());
                        if (matcher.find())
                            answers.put(matcher.group(), textNode.text().trim());
                    } else
                        rightAnswers = textNode.text();
        if (rightAnswers.contains("答案："))
            rightAnswers = rightAnswers.substring(rightAnswers.indexOf("答案：") + "答案：".length()).trim();
        if (rightAnswers.equalsIgnoreCase("✔") || rightAnswers.equalsIgnoreCase("T") || rightAnswers.equalsIgnoreCase("TRUE") || rightAnswers.equalsIgnoreCase("对"))
            rightAnswers = "true";
        if (rightAnswers.equalsIgnoreCase("X") || rightAnswers.equalsIgnoreCase("F") || rightAnswers.equalsIgnoreCase("FALSE") || rightAnswers.equalsIgnoreCase("错"))
            rightAnswers = "false";
        boolean answered = false;
        ArrayList<OptionInfo> wrongOptions = new ArrayList<>();
        for (char c : rightAnswers.toCharArray())
            for (OptionInfo optionInfo : quizConfig.getOptions()) {
                if (answers.containsKey(Character.toString(c)) && answers.get(Character.toString(c)).contains(optionInfo.getDescription()) || optionInfo.getName().equals(rightAnswers)) {
                    optionInfo.setRight(true);
                    if (!quizConfig.getQuestionType().equals("1"))
                        if (!quizConfig.isAnswered())
                            return true;
                        else if (answered)
                            wrongOptions.add(optionInfo);
                    answered = true;
                } else if (quizConfig.isAnswered())
                    wrongOptions.add(optionInfo);
            }
        if (answered)
            for (OptionInfo wrongOption : wrongOptions)
                wrongOption.setRight(false);
        return answered;
    }

    private static int getExamQuiz(String baseUri, int index, ExamQuizInfo examQuizInfo) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("classId", examQuizInfo.getClassId());
        params.put("courseId", examQuizInfo.getCourseId());
        params.put("tId", examQuizInfo.gettId());
        params.put("id", examQuizInfo.getTestUserRelationId());
        params.put("examsystem", examQuizInfo.getExamsystem());
        params.put("enc", examQuizInfo.getEnc());
        params.put("p", "1");
//        params.put("tag", "1");
        params.put("start", String.valueOf(index));
        params.put("remainTimeParam", String.valueOf(examQuizInfo.getRemainTime()));
        params.put("relationAnswerLastUpdateTime", String.valueOf(examQuizInfo.getEncLastUpdateTime()));
        params.put("getTheNextQuestion", "1");
        params.put("keyboardDisplayRequiresUserAction", "1");
        RawResponse response = session.get(baseUri + "/exam/test/reVersionTestStartNew").params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        Document document = Jsoup.parse(response.readToText());
        FormElement form = document.select("form#submitTest").forms().get(0);
        if (null == examQuizInfo.getDatas())
            examQuizInfo.setDatas(new QuizConfig[document.select("a[id~=span]").size()]);
        examQuizInfo.setClassId(document.getElementById("classId").val());
        examQuizInfo.setCourseId(document.getElementById("courseId").val());
        examQuizInfo.settId(document.getElementById("tId").val());
        examQuizInfo.setTestUserRelationId(form.getElementById("testUserRelationId").val());
        examQuizInfo.setExamsystem(document.getElementById("examsystem").val());
        examQuizInfo.setUserId(document.getElementById("userId").val());
        examQuizInfo.setTestPaperId(document.getElementById("testPaperId").val());
        examQuizInfo.setPaperId(document.getElementById("paperId").val());
        examQuizInfo.setSubCount(document.getElementById("subCount").val());
        examQuizInfo.setEnc(document.getElementById("enc").val());
        examQuizInfo.setRandomOptions(document.getElementById("randomOptions").val().equals("true"));
        examQuizInfo.setRemainTime(Integer.parseInt(document.getElementById("remainTime").val()));
        examQuizInfo.setEncRemainTime(Integer.parseInt(document.getElementById("encRemainTime").val()));
        examQuizInfo.setEncLastUpdateTime(Long.parseLong(document.getElementById("encLastUpdateTime").val()));
//        examQuizInfo.setTimeOver(examQuizInfo.getRemainTime() <= 0);
        examQuizInfo.setTimeOver(false);
        examQuizInfo.setTempSave(true);
        QuizConfig quizConfig = new QuizConfig();
        quizConfig.setValidationUrl(baseUri + form.attr("action"));
        quizConfig.setAnswered(false);
        quizConfig.setMemberinfo(form.getElementById("questionId").val());
        quizConfig.setResourceId(examQuizInfo.getTestUserRelationId());
        quizConfig.setDescription(document.selectFirst("div.Cy_Title div").text().replaceFirst("（[\\d.]+分）", ""));
        quizConfig.setQuestionType(document.getElementById("type").val());
        quizConfig.setScore(document.getElementById("questionScore").val());
        Elements lisDescription = document.select("ul.Cy_ulTop li");
        Elements lis = document.select("ul.Cy_ulBottom li");
        quizConfig.setOptions(new OptionInfo[lis.size()]);
        for (int j = 0; j < lis.size(); j++) {
            quizConfig.getOptions()[j] = new OptionInfo();
            Element input = lis.get(j).selectFirst("input");
            quizConfig.getOptions()[j].setRight(input.hasAttr("checked"));
            if (quizConfig.getOptions()[j].isRight())
                quizConfig.setAnswered(true);
            quizConfig.getOptions()[j].setName(input.val());
            if (lisDescription.isEmpty())
                quizConfig.getOptions()[j].setDescription("");
            else
                quizConfig.getOptions()[j].setDescription(lisDescription.get(j).selectFirst("a").text());
            if (quizConfig.getOptions()[j].getDescription().isEmpty())
                quizConfig.getOptions()[j].setDescription(quizConfig.getOptions()[j].getName());
        }
        int i = index - 1;
        Matcher matcher = Pattern.compile("\\d+").matcher(document.selectFirst("div.leftBottom span").text());
        if (matcher.find())
            i = Integer.valueOf(matcher.group()) - 1;
        if (null == examQuizInfo.getDatas()[i])
            examQuizInfo.getDatas()[i] = quizConfig;
        return i;
    }

    private static TaskInfo<ExamData> getExamInfo(String baseUri, String examUri, Map<String, String> params) throws CheckCodeException {
        RawResponse response = session.get(baseUri + examUri).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        Document document = Jsoup.parse(response.readToText());
        Elements lis = document.selectFirst("div.ulDiv ul").getElementsByTag("li");
        String classId = document.getElementById("classId").val();
        String moocTeacherId = document.getElementById("moocTeacherId").val();
        String examsystem = document.getElementById("examsystem").val();
        String examEnc = document.getElementById("examEnc").val();
        String begin = "(";
        String end = ")";
        TaskInfo<ExamData> examInfo = new TaskInfo<>();
        examInfo.setDefaults(new TaskConfig());
        examInfo.setAttachments(new ExamData[lis.size()]);
        for (int i = 0; i < lis.size(); i++) {
            Element examElement = lis.get(i).selectFirst("div.titTxt");
            Element dataElement = examElement.selectFirst("p a");
            String statusStr = examElement.wholeText();
            statusStr = statusStr.substring(statusStr.indexOf("状态："));
            boolean isPassed = !statusStr.contains("待做");
            String paramStr = dataElement.attr("onclick");
            int beginIndex = paramStr.indexOf(begin) + begin.length();
            paramStr = paramStr.substring(beginIndex, paramStr.indexOf(end, beginIndex));
            String[] funcParams = paramStr.split(",");
            examInfo.getDefaults().setCourseid(funcParams[0].replaceAll("'", ""));
            examInfo.getDefaults().setClazzId(!classId.isEmpty() ? classId : params.get("classId"));
            examInfo.getAttachments()[i] = new ExamData();
            examInfo.getAttachments()[i].setPassed(isPassed);
            examInfo.getAttachments()[i].setEnc(params.get("enc"));
            examInfo.getAttachments()[i].setProperty(new ExamDataProperty());
            examInfo.getAttachments()[i].getProperty().settId(funcParams[1].isEmpty() ? "0" : funcParams[1]);
            examInfo.getAttachments()[i].getProperty().setId(funcParams[2]);
            examInfo.getAttachments()[i].getProperty().setEndTime(funcParams[3]);
            examInfo.getAttachments()[i].getProperty().setMoocTeacherId(moocTeacherId);
            examInfo.getAttachments()[i].getProperty().setExamsystem(examsystem);
            examInfo.getAttachments()[i].getProperty().setExamEnc(examEnc);
            examInfo.getAttachments()[i].getProperty().setTitle(dataElement.attr("title"));
        }
        return examInfo;
    }

    private static boolean storeExamQuiz(int index, ExamQuizInfo examQuizInfo) throws CheckCodeException {
        examQuizInfo.setTempSave(true);
        return answerExamQuiz(index, examQuizInfo);
    }

    /**
     * JavaScript code:
     * function encrypt(h, g) {
     * if (g == null || g.length <= 0) {
     * return null
     * }
     * var m = "";
     * for (var d = 0; d < g.length; d++) {
     * m += g.charCodeAt(d).toString()
     * }
     * var j = Math.floor(m.length / 5);
     * var c = parseInt(m.charAt(j) + m.charAt(j * 2) + m.charAt(j * 3) + m.charAt(j * 4));
     * var a = Math.ceil(g.length / 2);
     * var k = Math.pow(2, 31) - 1;
     * if (c < 2) {
     * alert("Algorithm cannot find a suitable hash. Please choose a different password. \nPossible considerations are to choose a more complex or longer password.");
     * return null
     * }
     * var b = Math.random();
     * var e = Math.round(b * 1000000000) % 100000000;
     * m += e;
     * if (m.length > 10) {
     * m = parseInt(m.substring(0, 10)).toString()
     * }
     * m = (c * m + a) % k;
     * var f = "";
     * var l = "";
     * for (var d = 0; d < h.length; d++) {
     * f = parseInt(h.charCodeAt(d) ^ Math.floor((m / k) * 255));
     * if (f < 16) {
     * l += "0" + f.toString(16)
     * } else {
     * l += f.toString(16)
     * }
     * m = (c * m + a) % k
     * }
     * e = e.toString(16);
     * while (e.length < 8) {
     * e = "0" + e
     * }
     * l += e;
     * return l + "&rd=" + b
     * }
     * var __e = function() {
     * var h = {
     * "x": -1,
     * "y": -1
     * };
     * var c = document.getElementById("userId").value;
     * var g = document.getElementById("questionId").value;
     * var a = "";
     * var k = c;
     * if (g != null) {
     * a = g.value;
     * k = c + "_" + a
     * }
     * try {
     * if (typeof(k) == "undefined") {
     * k = "axvP^&Sg"
     * }
     * var d = window.event;
     * if (typeof(d) == "undefined") {
     * var l = arguments.callee.caller,
     * m = l;
     * while (l != null) {
     * m = l;
     * l = l.caller
     * }
     * d = m.arguments[0]
     * }
     * if (d != null) {
     * var j = document.documentElement.scrollLeft || document.body.scrollLeft;
     * var i = document.documentElement.scrollTop || document.body.scrollTop;
     * h.x = d.pageX || d.clientX + j;
     * h.y = d.pageY || d.clientY + i
     * }
     * } catch (f) {
     * h = {
     * "x": -2,
     * "y": -2
     * }
     * }
     * var b = "(" + Math.ceil(h.x) + "|" + Math.ceil(h.y) + ")";
     * return encrypt(b, k) + "&value=" + b + "&qid=" + a
     * };
     * window.getEnc = function() {
     * return __e()
     * };
     */
    private static boolean answerExamQuiz(int index, ExamQuizInfo examQuizInfo) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("tempSave", examQuizInfo.isTempSave() ? "true" : "false");
        int version = 1;
        Matcher matcher = Pattern.compile("version=(\\d)").matcher(examQuizInfo.getDatas()[index].getValidationUrl());
        if (matcher.find())
            version += Integer.valueOf(matcher.group(1));
        params.put("version", String.valueOf(version));
        int pageWidth = 898;
        int pageHeight = 687;
        String value = "(" + pageWidth + "|" + pageHeight + ")";
        String uwId = examQuizInfo.getUserId() + "_" + examQuizInfo.getDatas()[index].getMemberinfo();
//        if (uwId == null)
//            uwId = "axvP^&Sg";
        int uwIdLength = uwId.length() / 2 + ((uwId.length() % 2 == 0) ? 0 : 1);
        double random = Math.random();
//        double random = 0.03926823033418314;
        long randomMillion = Math.round(random * 1000000000) % 100000000;
        StringBuilder uwIdASCII = new StringBuilder();
        for (byte ascii : uwId.getBytes())
            uwIdASCII.append(ascii);
        StringBuilder multiplierStr = new StringBuilder();
        for (int i = 1; i <= 4; i++)
            multiplierStr.append(uwIdASCII.charAt(uwIdASCII.length() / 5 * i));
        int multiplier = Integer.valueOf(multiplierStr.toString());
//        if (multiplier < 2)
//            throw new IOException("Algorithm cannot find a suitable hash. Please choose a different password. \nPossible considerations are to choose a more complex or longer password.");
        uwIdASCII.append(randomMillion);
        if (uwIdASCII.length() > 10)
            uwIdASCII.setLength(10);
        long n = (multiplier * Long.valueOf(uwIdASCII.toString()) + uwIdLength) % Integer.MAX_VALUE;
        StringBuilder pos = new StringBuilder();
        for (char c : value.toCharArray()) {
            pos.append(String.format("%02x", c ^ Math.floorDiv(n * 255, Integer.MAX_VALUE)));
            n = (multiplier * n + uwIdLength) % Integer.MAX_VALUE;
        }
        pos.append(String.format("%08x", randomMillion));
        params.put("pos", pos.toString());
        params.put("rd", String.valueOf(random));
        params.put("value", value);
        params.put("qid", examQuizInfo.getDatas()[index].getMemberinfo());
        HashMap<String, String> body = new HashMap<>();
        body.put("classId", examQuizInfo.getClassId());
        body.put("courseId", examQuizInfo.getCourseId());
        body.put("userId", examQuizInfo.getUserId());
        body.put("testUserRelationId", examQuizInfo.getDatas()[index].getResourceId());
        body.put("tId", examQuizInfo.gettId());
        body.put("paperId", examQuizInfo.getPaperId());
        body.put("testPaperId", examQuizInfo.getTestPaperId());
        body.put("subCount", examQuizInfo.getSubCount());
        body.put("remainTime", String.valueOf(examQuizInfo.getRemainTime()));
        body.put("encRemainTime", String.valueOf(examQuizInfo.getEncRemainTime()));
        body.put("encLastUpdateTime", String.valueOf(examQuizInfo.getEncLastUpdateTime()));
        body.put("type", examQuizInfo.getDatas()[index].getQuestionType());
        body.put("examsystem", examQuizInfo.getExamsystem());
        body.put("enc", examQuizInfo.getEnc());
        body.put("start", String.valueOf(index));
        body.put("randomOptions", examQuizInfo.isRandomOptions() ? "true" : "false");
        body.put("questionId", examQuizInfo.getDatas()[index].getMemberinfo());
        body.put("questionScore", examQuizInfo.getDatas()[index].getScore());
        body.put("tempSave", examQuizInfo.isTempSave() ? "true" : "false");
        body.put("timeOver", examQuizInfo.isTimeOver() ? "true" : "false");
        if (!body.get("questionId").isEmpty()) {
            body.put("type" + body.get("questionId"), body.get("type"));
            body.put("score" + body.get("questionId"), body.get("questionScore"));
            for (OptionInfo option : examQuizInfo.getDatas()[index].getOptions())
                if (option.isRight())
                    body.put(new String(("answer" + body.get("questionId")).getBytes()), option.getName());
        }
        RawResponse response = session.post(examQuizInfo.getDatas()[index].getValidationUrl()).params(params).body(body).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        String responseStr = response.readToText();
        if (!examQuizInfo.isTempSave())
            return responseStr.equals("1");
        String[] results = responseStr.split("\\|");
        if (results.length != 3)
            return false;
        examQuizInfo.setEncLastUpdateTime(Long.parseLong(results[0]));
        examQuizInfo.setRemainTime(Integer.parseInt(results[1]));
        examQuizInfo.setEncRemainTime(examQuizInfo.getRemainTime());
        examQuizInfo.setEnc(results[2]);
        return true;
    }

    /**
     * Flash ActionScript code:
     * public function onSendlog(me_data:Object, isDrag:int):void
     * {
     * var paramStr:*=null;
     * var playSecond:*=0;
     * var md5Str:*=null;
     * var state:*=0;
     * if (me_data.chapterId && isDrag != 1) {
     * state = 0;
     * if (isDrag == 4 || isDrag == 2)
     * state = 2;
     * else if (isDrag == 3)
     * state = 1;
     * paramStr = "s=" + me_data.clazzId + "&c=" + me_data.chapterId + "&o=" + me_data.objectId + "&st=" + state + "&m=0&d=" + me_data.duration;
     * md5Str = MD5.startMd("[" + me_data.chapterId + "]" + "[" + me_data.clazzId + "]" + "[" + me_data.duration + "]" + "[0]" + "[" + me_data.objectId + "]" + "[" + state + "]" + "[535e933c498001]");
     * paramStr = paramStr + "&enc=" + md5Str;
     * this.jQuery("sendlogzt", paramStr);
     * }
     * if (me_data.isSendLog != "1")
     * return;
     * paramStr = "";
     * var isSendLog:*="";
     * for (dataName in me_data) {
     * if (dataName == "dtoken")
     * continue;
     * paramStr = paramStr + "&" + dataName + "=" + me_data[dataName];
     * }
     * playSecond = this.getPlaySecond();
     * paramStr = paramStr + "&view=pc&playingTime=" + playSecond;
     * paramStr = paramStr + "&isdrag=" + isDrag;
     * md5Str = MD5.startMd("[" + me_data.clazzId + "]" + "[" + me_data.userid + "]" + "[" + me_data.jobid + "]" + "[" + me_data.objectId + "]" + "[" + playSecond * 1000 + "]" + "[d_yHJ!$pdA~5]" + "[" + int
     * (me_data.duration) * 1000 + "]" + "[" + me_data.clipTime + "]");
     * paramStr = paramStr + "&enc=" + md5Str;
     * paramStr = paramStr.substring(1);
     * this.jQuery("logFunc", paramStr);
     * return;
     * }
     **/
    private static boolean sendLog(TaskInfo taskInfo, PlayerData attachment, VideoInfo videoInfo, int playSecond, int dragStatus) throws CheckCodeException {
        /*
        don't send when review mode
        */
//        if (taskInfo.getDefaults().isFiled() || taskInfo.getDefaults().getState() == 1)
//            return false;
        Map<String, String> params = new HashMap<>();
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ignored) {
            return false;
        }
        String chapterId = taskInfo.getDefaults().getChapterId();
        if (chapterId != null && !chapterId.isEmpty()) {
            int state;
            switch (dragStatus) {
                case 3:
                    state = 1;
                    break;
                case 2:
                case 4:
                    state = 2;
                    break;
                default:
                    state = 0;
                    break;
            }
            md5.update(("[" + taskInfo.getDefaults().getChapterId() + "]" + "[" + taskInfo.getDefaults().getClazzId() + "]" + "[" + videoInfo.getDuration() + "]" + "[0]" + "[" + videoInfo.getObjectid() + "]" + "[" + state + "]" + "[535e933c498001]").getBytes());
            StringBuilder md5Str = new StringBuilder(new BigInteger(1, md5.digest()).toString(16));
            while (md5Str.length() < 32)
                md5Str.insert(0, "0");
            params.put("u", taskInfo.getDefaults().getUserid());
            params.put("s", taskInfo.getDefaults().getClazzId());
            params.put("c", taskInfo.getDefaults().getChapterId());
            params.put("o", videoInfo.getObjectid());
            params.put("st", String.valueOf(state));
            params.put("m", "0");
            params.put("d", String.valueOf(videoInfo.getDuration()));
            params.put("enc", md5Str.toString());
            session.get("http://data.xxt.aichaoxing.com/analysis/datalog").params(params).proxy(proxy).send();
            params.clear();
        }
        if (taskInfo.getAttachments().length == 0)
            return false;
        String clipTime = videoInfo.getStartTime() + "_" + (videoInfo.getEndTime() != 0 ? videoInfo.getEndTime() : videoInfo.getDuration());
        params.put("clazzId", taskInfo.getDefaults().getClazzId());
        params.put("objectId", videoInfo.getObjectid());
        params.put("userid", taskInfo.getDefaults().getUserid());
        params.put("jobid", attachment.getJobid());
        params.put("otherInfo", attachment.getOtherInfo());
        params.put("playingTime", String.valueOf(playSecond));
        params.put("isdrag", String.valueOf(dragStatus));
        params.put("duration", String.valueOf(videoInfo.getDuration()));
        params.put("clipTime", clipTime);
        params.put("dtype", attachment.getType());
        params.put("rt", String.valueOf(videoInfo.getRt() != 0.0f ? videoInfo.getRt() : 0.9f));
        params.put("view", "pc");
        md5.update(("[" + taskInfo.getDefaults().getClazzId() + "]" + "[" + taskInfo.getDefaults().getUserid() + "]" + "[" + attachment.getJobid() + "]" + "[" + videoInfo.getObjectid() + "]" + "[" + playSecond * 1000 + "]" + "[d_yHJ!$pdA~5]" + "[" + videoInfo.getDuration() * 1000 + "]" + "[" + clipTime + "]").getBytes());
        StringBuilder md5Str = new StringBuilder(new BigInteger(1, md5.digest()).toString(16));
        while (md5Str.length() < 32)
            md5Str.insert(0, "0");
        params.put("enc", md5Str.toString());
        RawResponse response;
        if (videoInfo.getDtoken() != null && !videoInfo.getDtoken().isEmpty())
            response = session.get(taskInfo.getDefaults().getReportUrl() + "/" + videoInfo.getDtoken()).params(params).followRedirect(false).proxy(proxy).send();
        else
            response = session.get(taskInfo.getDefaults().getReportUrl()).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        return JSONObject.parseObject(response.readToText()).getBoolean("isPassed");
    }
}
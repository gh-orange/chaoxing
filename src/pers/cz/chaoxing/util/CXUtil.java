package pers.cz.chaoxing.util;

import com.alibaba.fastjson.*;
import net.dongliu.requests.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.quiz.HomeworkQuizInfo;
import pers.cz.chaoxing.common.quiz.OptionInfo;
import pers.cz.chaoxing.common.quiz.PlayerQuizInfo;
import pers.cz.chaoxing.common.quiz.QuizConfig;
import pers.cz.chaoxing.common.task.HomeworkData;
import pers.cz.chaoxing.common.task.PlayerData;
import pers.cz.chaoxing.common.task.TaskData;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.net.Proxy;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
        int begin = indexUri.indexOf(beginStr) + beginStr.length();
        Document document = Jsoup.parse(session.get(indexUri.substring(begin, indexUri.indexOf(endStr, begin))).proxy(proxy).send().readToText());
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
            throw new CheckCodeException(response.getHeader("location"), session);
        String src = Jsoup.parse(response.readToText()).select("div.mainright iframe").attr("src");
        for (int i = 0; i < 2; i++) {
            response = session.get(src).followRedirect(false).proxy(proxy).send();
            if (response.getStatusCode() == StatusCodes.FOUND)
                src = response.getHeader("location");
            else
                break;
        }
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("location"), session);
        return response.getURL();
    }

    public static List<String> getClasses(String uri) {
        RawResponse response = session.get(uri).proxy(proxy).send();
        Document document = Jsoup.parse(response.readToText());
        return document.select("div.httpsClass.Mconright a").eachAttr("href");
    }

    public static List<String> getTasks(String uri) {
        Document document = Jsoup.parse(session.get(uri).proxy(proxy).send().readToText());
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

    public static String getCardUriModel(String baseUri, String uri, Map<String, String> params) throws CheckCodeException {
        RawResponse response = session.get(baseUri + uri).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("location"), session);
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

    public static <T extends TaskData> TaskInfo<T> getTaskInfo(String baseUri, String cardUri, Map<String, String> params, InfoType infoType) throws CheckCodeException {
//        session.post(baseUri + "/mycourse/studentstudyAjax").body(params).proxy(proxy).send();
        params.put("num", String.valueOf(infoType.ordinal()));
        for (Map.Entry<String, String> param : params.entrySet())
            cardUri = cardUri.replaceAll("(?i)=" + param.getKey(), "=" + param.getValue());
        RawResponse response = session.get(baseUri + cardUri).followRedirect(false).proxy(proxy).send();
//        session.get(baseUri + "/mycourse/studentstudycourselist").params(params).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("location"), session);
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
                    taskInfo.getAttachments()[0].setUtEnc(params.get("utenc"));
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
     */
    public static boolean onStart(TaskInfo<PlayerData> taskInfo, VideoInfo videoInfo) throws CheckCodeException {
        return sendLog(taskInfo, videoInfo, (int) (taskInfo.getAttachments()[0].getHeadOffset() / 1000), 3);
    }

    /**
     * call since player finished
     *
     * @param taskInfo
     * @param videoInfo
     * @return
     */
    public static boolean onEnd(TaskInfo taskInfo, VideoInfo videoInfo) throws CheckCodeException {
        return sendLog(taskInfo, videoInfo, videoInfo.getDuration(), 4);
    }

    /**
     * call since player clicked to play
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     */
    public static boolean onPlay(TaskInfo taskInfo, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        return sendLog(taskInfo, videoInfo, playSecond, 3);
    }

    /**
     * call since player clicked to pause
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     */
    public static boolean onPause(TaskInfo taskInfo, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        if (taskInfo.getDefaults().getChapterId() != null && !taskInfo.getDefaults().getChapterId().isEmpty())
            return sendLog(taskInfo, videoInfo, playSecond, 2);
        return false;
    }

    /**
     * call each intervalTime since player playing
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     */
    public static boolean onPlayProgress(TaskInfo taskInfo, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        return sendLog(taskInfo, videoInfo, playSecond, 0);
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
    private static boolean sendLog(TaskInfo taskInfo, VideoInfo videoInfo, int playSecond, int dragStatus) throws CheckCodeException {
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
            params.put("m", String.valueOf(0));
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
        params.put("jobid", taskInfo.getAttachments()[0].getJobid());
        params.put("otherInfo", taskInfo.getAttachments()[0].getOtherInfo());
        params.put("playingTime", String.valueOf(playSecond));
        params.put("isdrag", String.valueOf(dragStatus));
        params.put("duration", String.valueOf(videoInfo.getDuration()));
        params.put("clipTime", clipTime);
        params.put("dtype", taskInfo.getAttachments()[0].getType());
        params.put("rt", String.valueOf(videoInfo.getRt() != 0.0f ? videoInfo.getRt() : 0.9f));
        params.put("view", "pc");
        md5.update(("[" + taskInfo.getDefaults().getClazzId() + "]" + "[" + taskInfo.getDefaults().getUserid() + "]" + "[" + taskInfo.getAttachments()[0].getJobid() + "]" + "[" + videoInfo.getObjectid() + "]" + "[" + playSecond * 1000 + "]" + "[d_yHJ!$pdA~5]" + "[" + videoInfo.getDuration() * 1000 + "]" + "[" + clipTime + "]").getBytes());
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
            throw new CheckCodeException(response.getHeader("location"), session);
        return JSONObject.parseObject(response.readToText()).getBoolean("isPassed");
    }

    /**
     * call since player start playing
     *
     * @param initDataUrl
     * @param mid
     * @return
     */
    public static List<PlayerQuizInfo> getVideoQuiz(String initDataUrl, String mid) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("mid", mid);
        params.put("start", "undefined");
        RawResponse response = session.get(initDataUrl).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("location"), session);
        return JSONArray.parseArray(response.readToText(), PlayerQuizInfo.class);
    }

    public static boolean answerVideoQuiz(String baseUri, String validationUrl, String resourceId, String answer) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("resourceid", resourceId);
        params.put("answer", "'" + answer + "'");
        RawResponse response = session.get(baseUri + validationUrl).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("lotcation"), session);
        JSONObject jsonObject = JSONObject.parseObject(response.readToText());
        return jsonObject.getString("answer").equals(answer) && jsonObject.getBoolean("isRight");
    }

    public static HomeworkQuizInfo getHomeworkQuiz(String baseUri, TaskInfo<HomeworkData> taskInfo) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("api", "1");
        params.put("needRedirect", "true");
        params.put("workId", taskInfo.getAttachments()[0].getProperty().getWorkid());
        params.put("jobid", taskInfo.getAttachments()[0].getJobid());
        params.put("knowledgeid", taskInfo.getDefaults().getKnowledgeid());
        /*
        teacher or student
         */
        params.put("ut", "s");
        params.put("courseid", taskInfo.getDefaults().getCourseid());
        params.put("clazzId", taskInfo.getDefaults().getClazzId());
        params.put("type", taskInfo.getAttachments()[0].getProperty().getWorktype().equals("workB") ? "b" : "");
        params.put("enc", taskInfo.getAttachments()[0].getEnc());
        params.put("utenc", taskInfo.getAttachments()[0].getUtEnc());
        String responseStr = session.get(baseUri + "/api/work").params(params).proxy(proxy).send().readToText();
        Elements elements = Jsoup.parse(responseStr).select("div.CeYan");
        boolean isAnswered = !elements.select("div.ZyTop h3 span").text().contains("待做");
        FormElement form = elements.select("form#form1").forms().get(0);
        Elements questions = form.select("div.TiMu");
        HomeworkQuizInfo homeworkQuizInfo = new HomeworkQuizInfo();
        String beginStr = "= \"";
        String endStr = "\"";
        int beginIndex = responseStr.lastIndexOf(beginStr, responseStr.indexOf("$(\"#answerwqbid\")")) + beginStr.length();
        homeworkQuizInfo.setAnswerwqbid(responseStr.substring(beginIndex, responseStr.indexOf(endStr, beginIndex)));
        homeworkQuizInfo.setDatas(new QuizConfig[questions.size()]);
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
            homeworkQuizInfo.getDatas()[i].setAnswered(isAnswered);
            homeworkQuizInfo.getDatas()[i].setDescription(questions.get(i).select("div.Zy_TItle div.clearfix").first().text());
            Element input = questions.get(i).select("input[id~=answertype]").first();
            Element previous = input.previousElementSibling();
            if (previous.tagName().equals("input"))
                homeworkQuizInfo.getDatas()[i].setMemberinfo(previous.id());
            homeworkQuizInfo.getDatas()[i].setResourceId(input.id());
            homeworkQuizInfo.getDatas()[i].setQuestionType(input.val());
            Elements lis = questions.get(i).getElementsByTag("ul").first().getElementsByTag("li");
            homeworkQuizInfo.getDatas()[i].setOptions(new OptionInfo[lis.size()]);
            for (int j = 0; j < lis.size(); j++) {
                homeworkQuizInfo.getDatas()[i].getOptions()[j] = new OptionInfo();
                homeworkQuizInfo.getDatas()[i].getOptions()[j].setName(lis.get(j).select("label input").val());
                homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription(lis.get(j).select("a").text());
                if (homeworkQuizInfo.getDatas()[i].getOptions()[j].getDescription().isEmpty())
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription(homeworkQuizInfo.getDatas()[i].getOptions()[j].getName());
            }
        }
        return homeworkQuizInfo;
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
    public static boolean answerHomeworkQuiz(String baseUri, HomeworkQuizInfo homeworkQuizInfo) throws CheckCodeException, WrongAccountException {
        Map<String, String> params = new HashMap<>();
        params.put("courseId", homeworkQuizInfo.getCourseId());
        params.put("classId", homeworkQuizInfo.getClassId());
        params.put("_", String.valueOf(System.currentTimeMillis()));
        switch (JSONObject.parseObject(session.get(baseUri + "/work/validate").params(params).followRedirect(false).proxy(proxy).send().readToText()).getInteger("status")) {
            case 1:
                throw new WrongAccountException();
            case 2:
                throw new CheckCodeException(baseUri + "/img/code", session);
            case 3:
                break;
            default:
                return false;
        }
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
        int version = 1;
        Matcher matcher = Pattern.compile("version=(\\d)").matcher(homeworkQuizInfo.getDatas()[0].getValidationUrl());
        if (matcher.find())
            version += Integer.valueOf(matcher.group(1));
        params.clear();
        params.put("ua", "pc");
        params.put("formType", "post");
        params.put("saveStatus", "1");
        params.put("version", String.valueOf(version));
        params.put("pos", pos.toString());
        params.put("rd", String.valueOf(random));
        params.put("value", value);
        params.put("wid", homeworkQuizInfo.getWorkRelationId());
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
                    body.put(new String(quizConfig.getResourceId()), optionInfo.getName());
                    if (quizConfig.getMemberinfo() != null && !quizConfig.getMemberinfo().isEmpty())
                        answerStr.append(optionInfo.getName());
                }
            if (quizConfig.getMemberinfo() != null && !quizConfig.getMemberinfo().isEmpty())
                body.put(new String(quizConfig.getMemberinfo()), answerStr.toString());
        }
        RawResponse response = session.post(homeworkQuizInfo.getDatas()[0].getValidationUrl()).params(params).body(body).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("lotcation"), session);
        String responseStr = response.readToText();
        return !responseStr.contains("提交失败");
    }

    public static void saveCheckCode(String path) {
        session.get("http://passport2.chaoxing.com/num/code?" + System.currentTimeMillis()).proxy(proxy).send().writeToFile(path);
    }

}

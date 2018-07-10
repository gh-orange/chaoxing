package pers.cz.chaoxing.util;

import com.alibaba.fastjson.*;
import net.dongliu.requests.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.select.Elements;
import pers.cz.chaoxing.common.*;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;

import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;

public class CXUtil {

    private static final Type taskInfoType = new TypeReference<TaskInfo<TaskData>>() {
    }.getType();

    private static final Type playerInfoType = new TypeReference<TaskInfo<PlayerData>>() {
    }.getType();

    private static final Type homeworkInfoType = new TypeReference<TaskInfo<HomeworkData>>() {
    }.getType();

    private static Session session = Requests.session();

    public static boolean login(String username, String password, String checkCode) throws WrongAccountException {
        String indexUri = session.get("http://dlnu.fy.chaoxing.com/topjs?index=1").send().readToText();
        String beginStr = "location.href = \\\"";
        String endStr = "\\\"";
        int begin = indexUri.indexOf(beginStr) + beginStr.length();
        Document document = Jsoup.parse(session.get(indexUri.substring(begin, indexUri.indexOf(endStr, begin))).send().readToText());
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
        String responseStr = session.post("http://passport2.chaoxing.com/login?refer=http://i.mooc.chaoxing.com/space/index.shtml").body(postBody).send().readToText();
        if (responseStr.contains("密码错误"))
            throw new WrongAccountException();
        return !responseStr.contains("用户登录");
    }

    public static String getClassesUri() throws CheckCodeException {
        RawResponse response = session.get("http://i.mooc.chaoxing.com/space/index.shtml").followRedirect(false).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("location"), session);
        String src = Jsoup.parse(response.readToText()).select("div.mainright iframe").attr("src");
        for (int i = 0; i < 2; i++) {
            response = session.get(src).followRedirect(false).send();
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
        RawResponse response = session.get(uri).send();
        Document document = Jsoup.parse(response.readToText());
        return document.select("div.httpsClass.Mconright a").eachAttr("href");
    }

    public static List<String> getTasks(String uri) {
        Document document = Jsoup.parse(session.get(uri).send().readToText());
        /*
        return 'success'
         */
        String logUri = document.select("script[type=text/javascript]").last().attr("src");
        if (!logUri.isEmpty())
            session.get(logUri).send();
        Elements elements = new Elements();
        for (Element element : document.select("h3.clearfix"))
            if (!element.select("em.orange").text().isEmpty())
                elements.addAll(element.select("span.articlename a"));
        return elements.eachAttr("href");
    }

    public static String getCardUriModel(String baseUri, String uri, Map<String, String> params) throws CheckCodeException {
        RawResponse response = session.get(baseUri + uri).params(params).followRedirect(false).send();
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
//        session.post(baseUri + "/mycourse/studentstudyAjax").body(params).send();
        params.put("num", String.valueOf(infoType.ordinal()));
        for (Map.Entry<String, String> param : params.entrySet())
            cardUri = cardUri.replaceAll("(?i)=" + param.getKey(), "=" + param.getValue());
        RawResponse response = session.get(baseUri + cardUri).followRedirect(false).send();
//        session.get(baseUri + "/mycourse/studentstudycourselist").params(params).send();
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
                    none video
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
        return session.get(baseUri + uri + "/" + objectId).params(params).send().readToJson(VideoInfo.class);
    }

    /**
     * call since video loaded
     *
     * @param baseUri
     * @param params
     * @return
     */
    public static boolean startRecord(String baseUri, Map<String, String> params) {
        params.put("nodeid", params.get("chapterId"));
        return session.get(baseUri + "/edit/validatejobcount").params(params).send().readToText().contains("true");
    }

    /**
     * call since video loaded first
     *
     * @param taskInfo
     * @param videoInfo
     * @return
     */
    public static boolean onStart(TaskInfo<PlayerData> taskInfo, VideoInfo videoInfo) throws CheckCodeException {
        return sendLog(taskInfo, videoInfo, (int) (taskInfo.getAttachments()[0].getHeadOffset() / 1000), 3);
    }

    /**
     * call since video finished
     *
     * @param taskInfo
     * @param videoInfo
     * @return
     */
    public static boolean onEnd(TaskInfo taskInfo, VideoInfo videoInfo) throws CheckCodeException {
        return sendLog(taskInfo, videoInfo, videoInfo.getDuration(), 4);
    }

    /**
     * call since video clicked to play
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
     * call since video clicked to pause
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
     * call each intervalTime since video playing
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
            session.get("http://data.xxt.aichaoxing.com/analysis/datalog").params(params).send();
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
            response = session.get(taskInfo.getDefaults().getReportUrl() + "/" + videoInfo.getDtoken()).params(params).followRedirect(false).send();
        else
            response = session.get(taskInfo.getDefaults().getReportUrl()).params(params).followRedirect(false).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("location"), session);
        return JSONObject.parseObject(response.readToText()).getBoolean("isPassed");
    }

    /**
     * call since video start playing
     *
     * @param initDataUrl
     * @param mid
     * @return
     */
    public static List<QuizInfo> getVideoQuiz(String initDataUrl, String mid) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("mid", mid);
        params.put("start", "undefined");
        RawResponse response = session.get(initDataUrl).params(params).followRedirect(false).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("location"), session);
        return JSONArray.parseArray(response.readToText(), QuizInfo.class);
    }

    public static boolean answerVideoQuiz(String baseUri, String validationUrl, String resourceId, String answer) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("resourceid", resourceId);
        params.put("answer", "'" + answer + "'");
        RawResponse response = session.get(baseUri + validationUrl).params(params).followRedirect(false).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(response.getHeader("location"), session);
        JSONObject jsonObject = JSONObject.parseObject(response.readToText());
        return jsonObject.getString("answer").equals(answer) && jsonObject.getBoolean("isRight");
    }

    public static List<QuizInfo> getExamQuiz(String baseUri, TaskInfo<HomeworkData> taskInfo) throws CheckCodeException {
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
        String responseStr = session.get(baseUri + "/api/work").params(params).send().readToText();
        Elements elements = Jsoup.parse(responseStr).select("div.CeYan");
        boolean isAnswered = !elements.select("div.ZyTop h3 span").text().contains("待做");
        FormElement form = elements.select("form#form1").forms().get(0);
        form.setBaseUri(baseUri);
        Elements questions = form.select("div.TiMu");
        List<QuizInfo> quizInfoList = new ArrayList<>();
        QuizInfo quizInfo = new QuizInfo();
        quizInfoList.add(quizInfo);
        quizInfo.setDatas(new QuizConfig[questions.size()]);
        for (int i = 0; i < questions.size(); i++) {
            quizInfo.getDatas()[i] = new QuizConfig();
            quizInfo.getDatas()[i].setValidationUrl(form.absUrl("action"));
            quizInfo.getDatas()[i].setAnswered(isAnswered);
            quizInfo.getDatas()[i].setDescription(questions.get(i).select("div.Zy_TItle div.clearfix").first().text());
            Element input = questions.get(i).select("input[id~=answertype]").first();
            Element previous = input.previousElementSibling();
            if (previous.tagName().equals("input"))
                quizInfo.getDatas()[i].setMemberinfo(previous.id());
            quizInfo.getDatas()[i].setResourceId(input.id());
            quizInfo.getDatas()[i].setQuestionType(input.val());
            Elements lis = questions.get(i).getElementsByTag("ul").first().getElementsByTag("li");
            quizInfo.getDatas()[i].setOptions(new OptionInfo[lis.size()]);
            for (int j = 0; j < lis.size(); j++) {
                quizInfo.getDatas()[i].getOptions()[j] = new OptionInfo();
                quizInfo.getDatas()[i].getOptions()[j].setName(lis.get(j).select("label input").val());
                quizInfo.getDatas()[i].getOptions()[j].setDescription(lis.get(j).select("a").text());
                if (quizInfo.getDatas()[i].getOptions()[j].getDescription().isEmpty())
                    quizInfo.getDatas()[i].getOptions()[j].setDescription(quizInfo.getDatas()[i].getOptions()[j].getName());
            }
        }
        return quizInfoList;
    }

    public static void saveCheckCode(String path) {
        session.get("http://passport2.chaoxing.com/num/code?" + System.currentTimeMillis()).send().writeToFile(path);
    }

}

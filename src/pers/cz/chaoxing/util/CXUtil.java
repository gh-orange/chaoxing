package pers.cz.chaoxing.util;

import com.alibaba.fastjson.*;
import net.dongliu.requests.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.FormElement;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.other.SchoolInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizConfig;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizData;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizConfig;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizData;
import pers.cz.chaoxing.common.quiz.data.player.PlayerQuizData;
import pers.cz.chaoxing.common.task.*;
import pers.cz.chaoxing.common.task.data.TaskData;
import pers.cz.chaoxing.common.task.data.exam.ExamTaskData;
import pers.cz.chaoxing.common.task.data.exam.ExamDataProperty;
import pers.cz.chaoxing.common.task.data.homework.HomeworkTaskData;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CXUtil {

    private static final Type taskInfoType = new TypeReference<TaskInfo<TaskData>>() {
    }.getType();

    private static final Type playerInfoType = new TypeReference<TaskInfo<PlayerTaskData>>() {
    }.getType();

    private static final Type homeworkInfoType = new TypeReference<TaskInfo<HomeworkTaskData>>() {
    }.getType();

    private static final Type playerQuizInfoType = new TypeReference<List<QuizInfo<PlayerQuizData, Void>>>() {
    }.getType();

    private static Session session = Requests.session();

    //    public static Proxy proxy = Proxies.httpProxy("10.14.36.103", 8080);
    public static Proxy proxy = null;

    public static SchoolInfo searchSchool(String schoolName) {
        String responseStr = session.get("http://passport2.chaoxing.com/login").proxy(proxy).send().readToText();
        if (responseStr.isEmpty())
            return new SchoolInfo(false);
        Document document = Jsoup.parse(responseStr);
        String productid = document.getElementById("productid").val();
        Map<String, String> body = new HashMap<>();
        if (!productid.isEmpty())
            body.put("productid", productid);
        body.put("pid", document.getElementById("pid").val());
        body.put("allowJoin", "0");
        body.put("filter", schoolName);
        try {
            return session.post("http://passport2.chaoxing.com/org/searchforms").body(body).proxy(proxy).send().readToJson(SchoolInfo.class);
        } catch (JSONException ignored) {
            return new SchoolInfo(false);
        }
    }

    public static boolean login(int fid, String username, String password, String checkCode) throws WrongAccountException {
        Map<String, String> params = new HashMap<>();
        params.put("fid", String.valueOf(fid));
//        params.put("refer", "http://i.mooc.chaoxing.com/space/index.shtml");
        RawResponse response = session.get("http://passport2.chaoxing.com/login").params(params).proxy(proxy).send();
        String responseStr = response.readToText();
        if (responseStr.isEmpty())
            return false;
        Document document = Jsoup.parse(responseStr);
        Map<String, String> body = new HashMap<>();
        body.put("refer_0x001", document.getElementById("refer_0x001").val());
        body.put("pid", document.getElementById("pid").val());
        body.put("pidName", document.getElementById("pidName").val());
        body.put("fid", document.getElementById("fid").val());
        body.put("fidName", document.getElementById("fidName").val());
        body.put("allowJoin", document.select("input[name=allowJoin]").val());
        body.put("isCheckNumCode", document.select("input[name=isCheckNumCode]").val());
        body.put("f", document.select("input[name=f]").val());
        body.put("productid", document.getElementById("productid").val());
        body.put("verCode", document.getElementById("verCode").val());
        body.put("uname", username);
        body.put("password", password);
        body.put("numcode", checkCode);
        FormElement form = document.select("form#form").forms().get(0);
        form.setBaseUri("http://passport2.chaoxing.com");
        response = session.post(form.absUrl("action")).body(body).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            responseStr = session.get(response.getHeader("location")).cookies(session.currentCookies()).proxy(proxy).send().readToText();
        if (responseStr.contains("密码错误"))
            throw new WrongAccountException();
        return !responseStr.contains("用户登录");
    }

    public static String getClassesUri() throws CheckCodeException {
        RawResponse response = session.get("http://i.mooc.chaoxing.com/space/index.shtml").followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        Document document = Jsoup.parse(response.readToText());
        session.get(document.selectFirst("div.headbanner_new script").attr("src")).proxy(proxy).send();
        String src = document.select("div.mainright iframe").attr("src");
        for (int i = 0; i < 2; i++) {
            response = session.get(src).followRedirect(false).proxy(proxy).send();
            if (response.getStatusCode() == StatusCodes.FOUND)
                src = response.getHeader("location");
            else
                return response.getURL();
        }
        throw new CheckCodeException(session, src);
    }

    public static List<String> getClasses(String uri) throws CheckCodeException {
        RawResponse response = session.get(uri).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
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
        document.select("h3.clearfix").stream()
                .filter(element -> !element.select("em.orange").text().isEmpty())
                .forEach(element -> elements.addAll(element.select("span.articlename a")));
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
        params.put("utenc", StringUtil.subStringBetweenFirst(cardUri, "utEnc=\"", "\";"));
        return StringUtil.subStringBetweenFirst(cardUri, "document.getElementById(\"iframe\").src=\"", "\";").replaceAll("[\"+]", "");
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
        String src = baseUri + "/mycourse/studentstudy";
        RawResponse response = null;
        for (int i = 0; i < 2; i++) {
            response = session.get(src).params(params).followRedirect(false).proxy(proxy).send();
            if (response.getStatusCode() == StatusCodes.FOUND) {
                src = Optional.ofNullable(response.getHeader("location")).orElse("study");
                if (!src.contains("study"))
                    throw new CheckCodeException(session, src);
            } else
                break;
        }
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, src);
        Element script = Jsoup.parse(response.readToText()).select("script[src~=https?://]").first();
        if (!Optional.ofNullable(script).isPresent())
            return false;
        response = session.get(script.attr("src")).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        return response.readToText().contains("success");
    }

    public static <T extends TaskData> TaskInfo<T> getTaskInfo(String baseUri, String cardUri, Map<String, String> params, InfoType infoType) throws CheckCodeException {
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
        String responseStr = StringUtil.subStringBetweenFirst(StringUtil.subStringAfterFirst(response.readToText(), "try{"), "mArg = ", ";");
        try {
            switch (infoType) {
                case Video:
                    return JSON.parseObject(responseStr, playerInfoType);
                case Homework:
                    TaskInfo<HomeworkTaskData> taskInfo = JSON.parseObject(responseStr, homeworkInfoType);
                    for (HomeworkTaskData attachment : taskInfo.getAttachments())
                        attachment.setUtEnc(params.get("utenc"));
                    return (TaskInfo<T>) taskInfo;
                default:
                    return JSON.parseObject(responseStr, taskInfoType);
            }
        } catch (JSONException ignored) {
            TaskInfo<T> taskInfo = new TaskInfo<>();
            switch (infoType) {
                case Video:
                    /*
                    none player
                     */
                    PlayerTaskData playerTaskData = new PlayerTaskData();
                    playerTaskData.setPassed(true);
                    taskInfo.setAttachments((T[]) new PlayerTaskData[]{playerTaskData});
                    break;
                case Homework:
                    /*
                    none homework
                     */
                    HomeworkTaskData homeworkTaskData = new HomeworkTaskData();
                    homeworkTaskData.setUtEnc(params.get("utenc"));
                    taskInfo.setAttachments((T[]) new HomeworkTaskData[]{homeworkTaskData});
                    break;
            }
            return taskInfo;
        }
    }

    public static VideoInfo getVideoInfo(String baseUri, String uri, String objectId, String fid) throws CheckCodeException {
        Map<String, String> params = new HashMap<>();
        params.put("k", fid);
        params.put("_dc", String.valueOf(System.currentTimeMillis()));
        RawResponse response = session.get(baseUri + uri + "/" + objectId).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
//        return response.readToJson(VideoInfo.class);
        return response.readToJson(VideoInfo.class);
    }

    public static boolean startExam(String baseUri, TaskInfo<ExamTaskData> taskInfo, ExamTaskData attachment) throws CheckCodeException {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("classId", taskInfo.getDefaults().getClazzId());
            params.put("courseId", taskInfo.getDefaults().getCourseid());
            params.put("id", attachment.getProperty().gettId());
            params.put("endTime", attachment.getProperty().getEndTime());
            params.put("moocTeacherId", attachment.getProperty().getMoocTeacherId());
            String responseStr = session.get(baseUri + "/exam/test/isExpire").params(params).proxy(proxy).send().readToText();
            if (responseStr.isEmpty())
                return false;
            JSONObject result = JSON.parseObject(responseStr);
            switch (result.getIntValue("status")) {
                case 0:
                    System.out.println("Exam need finishStandard: " + attachment.getProperty().getTitle() + "[" + result.getIntValue("finishStandard") + "%]");
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
    public static boolean onStart(TaskInfo<PlayerTaskData> taskInfo, PlayerTaskData attachment, VideoInfo videoInfo) throws CheckCodeException {
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
    public static boolean onEnd(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo) throws CheckCodeException {
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
    public static boolean onPlay(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
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
    public static boolean onPause(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        if (!Optional.ofNullable(taskInfo.getDefaults().getChapterId()).orElse("").isEmpty())
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
    public static boolean onPlayProgress(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
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
    public static List<QuizInfo<PlayerQuizData, Void>> getPlayerQuizzes(String initDataUrl, String mid) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("mid", mid);
        params.put("start", "undefined");
        RawResponse response = session.get(initDataUrl).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        return JSON.parseArray(response.readToText()).toJavaObject(playerQuizInfoType);
    }

    public static QuizInfo<HomeworkQuizData, HomeworkQuizConfig> getHomeworkQuiz(String baseUri, TaskInfo<HomeworkTaskData> taskInfo, HomeworkTaskData attachment) throws CheckCodeException {
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
                src = Optional.ofNullable(response.getHeader("location")).orElse("work");
                if (!src.contains("work"))
                    throw new CheckCodeException(session, src);
            } else
                break;
        }
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, src);
        String responseStr = response.readToText();
        Element element = Jsoup.parse(responseStr).selectFirst("div.CeYan");
        Element form = element.selectFirst("form#form1");
        Elements questions = element.select("div.TiMu");
        responseStr = StringUtil.subStringAfterFirst(responseStr, "$(\"#answerwqbid\")");
        QuizInfo<HomeworkQuizData, HomeworkQuizConfig> homeworkQuizInfo = new QuizInfo<>();
        homeworkQuizInfo.setDefaults(new HomeworkQuizConfig());
        homeworkQuizInfo.setDatas(new HomeworkQuizData[questions.size()]);
        homeworkQuizInfo.setPassed(!element.select("div.ZyTop h3 span").text().contains("待做"));
        if (homeworkQuizInfo.isPassed()) {
            homeworkQuizInfo.getDefaults().setUserId(taskInfo.getDefaults().getUserid());
            homeworkQuizInfo.getDefaults().setClassId(taskInfo.getDefaults().getClazzId());
            homeworkQuizInfo.getDefaults().setCourseId(taskInfo.getDefaults().getCourseid());
            homeworkQuizInfo.getDefaults().setJobid(attachment.getJobid());
            homeworkQuizInfo.getDefaults().setOldWorkId(attachment.getProperty().getWorkid());
            homeworkQuizInfo.getDefaults().setKnowledgeid(taskInfo.getDefaults().getKnowledgeid());
            homeworkQuizInfo.getDefaults().setTotalQuestionNum(String.valueOf(questions.size()));
            homeworkQuizInfo.getDefaults().setEnc(attachment.getEnc());
            homeworkQuizInfo.getDefaults().setEnc_work(attachment.getUtEnc());
        } else {
            homeworkQuizInfo.getDefaults().setUserId(element.getElementById("userId").val());
            homeworkQuizInfo.getDefaults().setClassId(element.getElementById("classId").val());
            homeworkQuizInfo.getDefaults().setCourseId(element.getElementById("courseId").val());
            homeworkQuizInfo.getDefaults().setJobid(element.getElementById("jobid").val());
            homeworkQuizInfo.getDefaults().setOldWorkId(element.getElementById("oldWorkId").val());
            homeworkQuizInfo.getDefaults().setOldSchoolId(element.getElementById("oldSchoolId").val());
            homeworkQuizInfo.getDefaults().setKnowledgeid(element.getElementById("knowledgeid").val());
            homeworkQuizInfo.getDefaults().setAnswerwqbid(StringUtil.subStringBetweenLast(responseStr, "= \"", "\""));
            homeworkQuizInfo.getDefaults().setWorkAnswerId(element.getElementById("workAnswerId").val());
            homeworkQuizInfo.getDefaults().setWorkRelationId(element.getElementById("workRelationId").val());
            homeworkQuizInfo.getDefaults().setApi(element.getElementById("api").val());
            homeworkQuizInfo.getDefaults().setPyFlag(element.getElementById("pyFlag").val());
            homeworkQuizInfo.getDefaults().setFullScore(element.getElementById("fullScore").val());
            homeworkQuizInfo.getDefaults().setTotalQuestionNum(element.getElementById("totalQuestionNum").val());
            homeworkQuizInfo.getDefaults().setEnc(element.getElementById("enc").val());
            homeworkQuizInfo.getDefaults().setEnc_work(element.getElementById("enc_work").val());
        }
        IntStream.range(0, questions.size()).forEach(i -> {
            homeworkQuizInfo.getDatas()[i] = new HomeworkQuizData();
            homeworkQuizInfo.getDatas()[i].setAnswered(homeworkQuizInfo.isPassed());
            if (Optional.ofNullable(form).isPresent())
                homeworkQuizInfo.getDatas()[i].setValidationUrl(baseUri + "/work/" + form.attr("action"));
            else
                homeworkQuizInfo.getDatas()[i].setValidationUrl(baseUri + "/work/" + questions.get(i).selectFirst("form[id~=questionErrorForm]").attr("action"));
            Element inputAnswerType = questions.get(i).select("input[id~=answertype]").first();
            if (Optional.ofNullable(inputAnswerType).isPresent()) {
                Element inputAnswerCheck = inputAnswerType.previousElementSibling();
                homeworkQuizInfo.getDatas()[i].setAnswerTypeId(inputAnswerType.id());
                if (inputAnswerCheck.tagName().equals("input"))
                    homeworkQuizInfo.getDatas()[i].setAnswerCheckName(inputAnswerCheck.attr("name"));
                homeworkQuizInfo.getDatas()[i].setQuestionType(inputAnswerType.val());
            }
            homeworkQuizInfo.getDatas()[i].setDescription(questions.get(i).select("div.Zy_TItle div.clearfix").first().text());
            Elements lis = questions.get(i).getElementsByTag("ul").first().getElementsByTag("li");
            homeworkQuizInfo.getDatas()[i].setOptions(new OptionInfo[lis.size()]);
            IntStream.range(0, lis.size()).forEach(j -> {
                Element inputAnswer = lis.get(j).selectFirst("label input");
                homeworkQuizInfo.getDatas()[i].getOptions()[j] = new OptionInfo();
                if (Optional.ofNullable(inputAnswer).isPresent()) {
                    if (Optional.ofNullable(homeworkQuizInfo.getDatas()[i].getAnswerId()).orElse("").isEmpty())
                        homeworkQuizInfo.getDatas()[i].setAnswerId(inputAnswer.attr("name"));
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setRight(inputAnswer.hasAttr("checked"));
                    if (homeworkQuizInfo.getDatas()[i].getOptions()[j].isRight())
                        homeworkQuizInfo.getDatas()[i].setAnswered(true);
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setName(inputAnswer.val());
                } else
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setName(lis.get(j).selectFirst("i").text().replaceAll("、", ""));
                if (!lis.isEmpty())
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription(lis.get(j).select("a").text());
                if (Optional.ofNullable(homeworkQuizInfo.getDatas()[i].getOptions()[j].getDescription()).orElse("").isEmpty())
                    homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription(homeworkQuizInfo.getDatas()[i].getOptions()[j].getName());
            });
        });
        return homeworkQuizInfo;
    }

    public static QuizInfo<ExamQuizData, ExamQuizConfig> getExamQuiz(String baseUri, QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("classId", examQuizInfo.getDefaults().getClassId());
        params.put("courseId", examQuizInfo.getDefaults().getCourseId());
        params.put("tId", examQuizInfo.getDefaults().gettId());
        params.put("id", examQuizInfo.getDefaults().getTestUserRelationId());
        params.put("examsystem", examQuizInfo.getDefaults().getExamsystem());
        params.put("enc", examQuizInfo.getDefaults().getEnc());
        params.put("p", "1");
//        params.put("tag", "1");
        params.put("start", String.valueOf(examQuizInfo.getDefaults().getStart()));
        params.put("remainTimeParam", String.valueOf(examQuizInfo.getDefaults().getRemainTime()));
        params.put("relationAnswerLastUpdateTime", String.valueOf(examQuizInfo.getDefaults().getEncLastUpdateTime()));
        params.put("getTheNextQuestion", "1");
        params.put("keyboardDisplayRequiresUserAction", "1");
        RawResponse response = session.get(baseUri + "/exam/test/reVersionTestStartNew").params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        Document document = Jsoup.parse(response.readToText());
        FormElement form = document.select("form#submitTest").forms().get(0);
        form.setBaseUri(baseUri);
        if (!Optional.ofNullable(examQuizInfo.getDatas()).isPresent())
            examQuizInfo.setDatas(new ExamQuizData[document.select("a[id~=span]").size()]);
        examQuizInfo.getDefaults().setUserId(document.getElementById("userId").val());
        examQuizInfo.getDefaults().setClassId(document.getElementById("classId").val());
        examQuizInfo.getDefaults().setCourseId(document.getElementById("courseId").val());
        examQuizInfo.getDefaults().settId(document.getElementById("tId").val());
        examQuizInfo.getDefaults().setTestUserRelationId(form.getElementById("testUserRelationId").val());
        examQuizInfo.getDefaults().setExamsystem(document.getElementById("examsystem").val());
        examQuizInfo.getDefaults().setEnc(document.getElementById("enc").val());
        examQuizInfo.getDefaults().setTempSave(false);
//        examQuizInfo.getDefaults().setTimeOver(examQuizInfo.getRemainTime() <= 0);
        examQuizInfo.getDefaults().setTimeOver(false);
        Matcher matcher = Pattern.compile("\\d+").matcher(document.selectFirst("div.leftBottom span").text());
        if (matcher.find())
            examQuizInfo.getDefaults().setStart(Integer.valueOf(matcher.group()) - 1);
        examQuizInfo.getDefaults().setRemainTime(Integer.parseInt(document.getElementById("remainTime").val()));
        examQuizInfo.getDefaults().setEncRemainTime(Integer.parseInt(document.getElementById("encRemainTime").val()));
        examQuizInfo.getDefaults().setEncLastUpdateTime(Long.parseLong(document.getElementById("encLastUpdateTime").val()));
        ExamQuizData examQuizConfig = new ExamQuizData();
        examQuizConfig.setAnswered(false);
        examQuizConfig.setTestPaperId(document.getElementById("testPaperId").val());
        examQuizConfig.setPaperId(document.getElementById("paperId").val());
        examQuizConfig.setSubCount(document.getElementById("subCount").val());
        examQuizConfig.setRandomOptions(document.getElementById("randomOptions").val().equals("true"));
        examQuizConfig.setValidationUrl(form.absUrl("action"));
        examQuizConfig.setQuestionId(form.getElementById("questionId").val());
        examQuizConfig.setDescription(document.selectFirst("div.Cy_Title div").text().replaceFirst("（[\\d.]+分）", ""));
        examQuizConfig.setQuestionType(document.getElementById("type").val());
        examQuizConfig.setQuestionScore(document.getElementById("questionScore").val());
        Elements lisDescription = document.select("ul.Cy_ulTop li");
        Elements lis = document.select("ul.Cy_ulBottom li");
        examQuizConfig.setOptions(new OptionInfo[lis.size()]);
        IntStream.range(0, lis.size()).forEach(j -> {
            examQuizConfig.getOptions()[j] = new OptionInfo();
            Element input = lis.get(j).selectFirst("input");
            examQuizConfig.getOptions()[j].setRight(input.hasAttr("checked"));
            if (examQuizConfig.getOptions()[j].isRight())
                examQuizConfig.setAnswered(true);
            examQuizConfig.getOptions()[j].setName(input.val());
            if (!lisDescription.isEmpty())
                examQuizConfig.getOptions()[j].setDescription(lisDescription.get(j).selectFirst("a").text());
            if (Optional.ofNullable(examQuizConfig.getOptions()[j].getDescription()).orElse("").isEmpty())
                examQuizConfig.getOptions()[j].setDescription(examQuizConfig.getOptions()[j].getName());
        });
        if (!Optional.ofNullable(examQuizInfo.getDatas()[examQuizInfo.getDefaults().getStart()]).isPresent())
            examQuizInfo.getDatas()[examQuizInfo.getDefaults().getStart()] = examQuizConfig;
        return examQuizInfo;
    }

    public static boolean storeHomeworkQuiz(String baseUri, HomeworkQuizConfig defaults, Map<HomeworkQuizData, List<OptionInfo>> answers) throws CheckCodeException, WrongAccountException {
        defaults.setPyFlag("1");
        return answerHomeworkQuiz(baseUri, defaults, answers);
    }

    public static boolean storeExamQuiz(ExamQuizConfig defaults, Map<ExamQuizData, List<OptionInfo>> answers) throws CheckCodeException {
        defaults.setTempSave(true);
        return answerExamQuiz(defaults, answers);
    }

    public static boolean answerPlayerQuiz(String baseUri, String validationUrl, String resourceId, String answer) throws CheckCodeException {
        HashMap<String, String> params = new HashMap<>();
        params.put("resourceid", resourceId);
        params.put("answer", "'" + answer + "'");
        RawResponse response = session.get(baseUri + validationUrl).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        JSONObject jsonObject = JSON.parseObject(response.readToText());
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
    public static boolean answerHomeworkQuiz(String baseUri, HomeworkQuizConfig defaults, Map<HomeworkQuizData, List<OptionInfo>> answers) throws CheckCodeException, WrongAccountException {
        HomeworkQuizData first = null;
        Iterator<HomeworkQuizData> iterator = answers.keySet().iterator();
        if (iterator.hasNext())
            first = iterator.next();
        if (!Optional.ofNullable(first).isPresent())
            return false;
        Map<String, String> params = new HashMap<>();
        params.put("courseId", defaults.getCourseId());
        params.put("classId", defaults.getClassId());
        /*
        cache false
         */
//        params.put("_", String.valueOf(System.currentTimeMillis()));
        if (Optional.ofNullable(defaults.getEnc()).orElse("").isEmpty()) {
            String responseStr = session.get(baseUri + "/work/validate").params(params).followRedirect(false).proxy(proxy).send().readToText();
            if (responseStr.isEmpty())
                return false;
            switch (JSON.parseObject(responseStr).getIntValue("status")) {
                case 1:
                    throw new WrongAccountException();
                case 2:
                    throw new CheckCodeException(session, baseUri + "/img/code");
                case 3:
                    break;
                default:
                    return false;
            }
        }
        int version = 1;
        Matcher matcher = Pattern.compile("version=(\\d)").matcher(first.getValidationUrl());
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
        String uwId = defaults.getUserId() + "_" + defaults.getWorkRelationId();
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
        params.put("wid", defaults.getWorkRelationId());
//        }
        //endregion
        Map<String, String> body = new IdentityHashMap<>();
        body.put("pyFlag", defaults.getPyFlag());
        body.put("courseId", defaults.getCourseId());
        body.put("classId", defaults.getClassId());
        body.put("api", defaults.getApi());
        body.put("workAnswerId", defaults.getWorkAnswerId());
        body.put("totalQuestionNum", defaults.getTotalQuestionNum());
        body.put("fullScore", defaults.getFullScore());
        body.put("knowledgeid", defaults.getKnowledgeid());
        body.put("oldSchoolId", defaults.getOldSchoolId());
        body.put("oldWorkId", defaults.getOldWorkId());
        body.put("jobid", defaults.getJobid());
        body.put("workRelationId", defaults.getWorkRelationId());
        body.put("enc", defaults.getEnc());
        body.put("enc_work", defaults.getEnc_work());
        body.put("userId", defaults.getUserId());
        body.put("answerwqbid", defaults.getAnswerwqbid());
        answers.forEach((homeworkQuizData, options) -> {
            StringBuilder answerStr = new StringBuilder();
            if (options.isEmpty())
                options = Arrays.stream(homeworkQuizData.getOptions()).filter(OptionInfo::isRight).collect(Collectors.toList());
            options.forEach(optionInfo -> {
                body.put(new String(homeworkQuizData.getAnswerId().getBytes()), optionInfo.getName());
                if (!Optional.ofNullable(homeworkQuizData.getAnswerCheckName()).orElse("").isEmpty())
                    answerStr.append(optionInfo.getName());
            });
            if (!Optional.ofNullable(homeworkQuizData.getAnswerCheckName()).orElse("").isEmpty())
                body.put(homeworkQuizData.getAnswerCheckName(), answerStr.toString());
            if (!Optional.ofNullable(homeworkQuizData.getAnswerTypeId()).orElse("").isEmpty())
                body.put(homeworkQuizData.getAnswerTypeId(), homeworkQuizData.getQuestionType());
        });
        RawResponse response = session.post(first.getValidationUrl()).params(params).body(body).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        String responseStr = response.readToText();
        return !responseStr.contains("提交失败") && !responseStr.contains("false");
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
    public static boolean answerExamQuiz(ExamQuizConfig defaults, Map<ExamQuizData, List<OptionInfo>> answers) throws CheckCodeException {
        ExamQuizData first = null;
        Iterator<ExamQuizData> iterator = answers.keySet().iterator();
        if (iterator.hasNext())
            first = iterator.next();
        if (!Optional.ofNullable(first).isPresent())
            return false;
        HashMap<String, String> params = new HashMap<>();
        params.put("tempSave", defaults.isTempSave() ? "true" : "false");
        int version = 1;
        Matcher matcher = Pattern.compile("version=(\\d)").matcher(first.getValidationUrl());
        if (matcher.find())
            version += Integer.valueOf(matcher.group(1));
        params.put("version", String.valueOf(version));
        int pageWidth = 898;
        int pageHeight = 687;
        String value = "(" + pageWidth + "|" + pageHeight + ")";
        String uwId = defaults.getUserId() + "_" + first.getQuestionId();
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
        params.put("qid", first.getQuestionId());
        HashMap<String, String> body = new HashMap<>();
        body.put("userId", defaults.getUserId());
        body.put("classId", defaults.getClassId());
        body.put("courseId", defaults.getCourseId());
        body.put("tId", defaults.gettId());
        body.put("testUserRelationId", defaults.getTestUserRelationId());
        body.put("examsystem", defaults.getExamsystem());
        body.put("enc", defaults.getEnc());
        body.put("tempSave", defaults.isTempSave() ? "true" : "false");
        body.put("timeOver", defaults.isTimeOver() ? "true" : "false");
        body.put("remainTime", String.valueOf(defaults.getRemainTime()));
        body.put("encRemainTime", String.valueOf(defaults.getEncRemainTime()));
        body.put("encLastUpdateTime", String.valueOf(defaults.getEncLastUpdateTime()));
        body.put("type", first.getQuestionType());
        body.put("start", String.valueOf(defaults.getStart()));
        body.put("paperId", first.getPaperId());
        body.put("testPaperId", first.getTestPaperId());
        body.put("subCount", first.getSubCount());
        body.put("randomOptions", first.isRandomOptions() ? "true" : "false");
        body.put("questionId", first.getQuestionId());
        body.put("questionScore", first.getQuestionScore());
        if (!body.get("questionId").isEmpty()) {
            body.put("type" + body.get("questionId"), body.get("type"));
            body.put("score" + body.get("questionId"), body.get("questionScore"));
            List<OptionInfo> options = answers.values().stream()
                    .flatMap(Collection::stream).collect(Collectors.toList());
            if (options.isEmpty())
                options = answers.keySet().stream().flatMap(examQuizData -> Arrays.stream(examQuizData.getOptions())).filter(OptionInfo::isRight).collect(Collectors.toList());
            options.stream()
                    .map(OptionInfo::getName)
                    .forEach(name -> body.put(new String(("answer" + body.get("questionId")).getBytes()), name));
        }
        RawResponse response = session.post(first.getValidationUrl()).params(params).body(body).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        String responseStr = response.readToText();
        if (!defaults.isTempSave())
            return responseStr.equals("1");
        String[] results = responseStr.split("\\|");
        if (results.length != 3)
            return false;
        defaults.setEncLastUpdateTime(Long.parseLong(results[0]));
        defaults.setRemainTime(Integer.parseInt(results[1]));
        defaults.setEncRemainTime(defaults.getRemainTime());
        defaults.setEnc(results[2]);
        return true;
    }

    public static void saveCheckCode(String path) {
        session.get("http://passport2.chaoxing.com/num/code?" + System.currentTimeMillis()).proxy(proxy).send().writeToFile(path);
    }

    /**
     * Thanks to m.3gmfw.cn for database support
     *
     * @param quizData
     * @return
     */
    public static List<OptionInfo> getQuizAnswer(QuizData quizData) {
        List<OptionInfo> options = new ArrayList<>();
        String[] descriptions = quizData.getDescription().replaceAll("【.*?】", "").split("[\\pP\\pS\\pZ]");
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(descriptions, 0, descriptions.length > 8 ? descriptions.length / 2 : descriptions.length).forEach(stringBuilder::append);
        String description = stringBuilder.toString();
        try {
            description = URLEncoder.encode(description, "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
            return options;
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
                String script = StringUtil.subStringBetweenFirst(document.select("script").html(), "setTimeout(function(){", "f.submit();");
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
        Element div = document.selectFirst("div.searchTopic");
        if (!Optional.ofNullable(div).isPresent())
            return options;
        document = Jsoup.parse(Requests.get("https://m.3gmfw.cn/" + div.selectFirst("a").attr("href")).proxy(proxy).send().charset("GBK").readToText());
        Elements p = document.select("div.content p");
        Map<String, String> answers = new HashMap<>();
        p.stream()
                .flatMap(element -> element.textNodes().stream())
                .filter(textNode -> !textNode.isBlank())
                .map(TextNode::text)
                .forEach(text -> {
                    if (!text.trim().contains("答案：")) {
                        Matcher matcher = Pattern.compile("[a-zA-Z]").matcher(text);
                        if (matcher.find())
                            answers.put(matcher.group(), text.trim());
                    } else
                        p.last().text(text);
                });
        String rightAnswers = p.last().text();
        if (rightAnswers.contains("答案："))
            rightAnswers = rightAnswers.substring(rightAnswers.indexOf("答案：") + "答案：".length()).trim();
        if (rightAnswers.equalsIgnoreCase("√") || rightAnswers.equalsIgnoreCase("✔") || rightAnswers.equalsIgnoreCase("T") || rightAnswers.equalsIgnoreCase("TRUE") || rightAnswers.equalsIgnoreCase("对"))
            rightAnswers = "true";
        if (rightAnswers.equalsIgnoreCase("X") || rightAnswers.equalsIgnoreCase("F") || rightAnswers.equalsIgnoreCase("FALSE") || rightAnswers.equalsIgnoreCase("错"))
            rightAnswers = "false";
        String finalRightAnswers = rightAnswers;
        finalRightAnswers.chars()
                .mapToObj(i -> Character.toString((char) i))
                .forEach(c -> {
                    List<OptionInfo> rightOptions = Arrays.stream(quizData.getOptions())
                            .filter(optionInfo -> answers.containsKey(c) && answers.get(c).contains(optionInfo.getDescription()) || optionInfo.getName().equals(finalRightAnswers))
                            .map(OptionInfo::new)
                            .collect(Collectors.toList());
                    rightOptions.forEach(optionInfo -> optionInfo.setRight(true));
                    if (quizData.getQuestionType().equals("1"))
                        options.addAll(rightOptions);
                    else if (!rightOptions.isEmpty() && options.isEmpty())
                        options.add(rightOptions.get(0));
                });
        return options;
    }

    private static TaskInfo<ExamTaskData> getExamInfo(String baseUri, String examUri, Map<String, String> params) throws CheckCodeException {
        RawResponse response = session.get(baseUri + examUri).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        Document document = Jsoup.parse(response.readToText());
        Elements lis = document.selectFirst("div.ulDiv ul").getElementsByTag("li");
        String classId = document.getElementById("classId").val();
        String moocTeacherId = document.getElementById("moocTeacherId").val();
        String examsystem = document.getElementById("examsystem").val();
        String examEnc = document.getElementById("examEnc").val();
        TaskInfo<ExamTaskData> examInfo = new TaskInfo<>();
        examInfo.setDefaults(new TaskConfig());
        examInfo.setAttachments(new ExamTaskData[lis.size()]);
        IntStream.range(0, lis.size()).forEach(i -> {
            Element examElement = lis.get(i).selectFirst("div.titTxt");
            Element dataElement = examElement.selectFirst("p a");
            String statusStr = examElement.wholeText();
            boolean isPassed = !statusStr.substring(statusStr.indexOf("状态：")).contains("待做");
            String paramStr = StringUtil.subStringBetweenFirst(dataElement.attr("onclick"), "(", ")");
            String[] funcParams = paramStr.split(",");
            examInfo.getDefaults().setClazzId(!classId.isEmpty() ? classId : params.get("classId"));
            examInfo.getAttachments()[i] = new ExamTaskData();
            examInfo.getAttachments()[i].setPassed(isPassed);
            examInfo.getAttachments()[i].setEnc(params.get("enc"));
            examInfo.getAttachments()[i].setProperty(new ExamDataProperty());
            if (funcParams.length >= 4) {
                examInfo.getDefaults().setCourseid(funcParams[0].replaceAll("'", ""));
                examInfo.getAttachments()[i].getProperty().settId(funcParams[1].isEmpty() ? "0" : funcParams[1]);
                examInfo.getAttachments()[i].getProperty().setId(funcParams[2]);
                examInfo.getAttachments()[i].getProperty().setEndTime(funcParams[3]);
            } else {
                examInfo.getDefaults().setCourseid(params.get("courseId"));
                examInfo.getAttachments()[i].getProperty().setEndTime(StringUtil.subStringBetweenFirst(StringUtil.subStringAfterFirst(statusStr, "时间："), "至", "考试").trim());
            }
            examInfo.getAttachments()[i].getProperty().setMoocTeacherId(moocTeacherId);
            examInfo.getAttachments()[i].getProperty().setExamsystem(examsystem);
            examInfo.getAttachments()[i].getProperty().setExamEnc(examEnc);
            examInfo.getAttachments()[i].getProperty().setTitle(dataElement.attr("title"));
        });
        return examInfo;
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
    private static boolean sendLog(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, int playSecond, int dragStatus) throws CheckCodeException {
        /*
        don't send when review mode
        */
//        if (taskInfo.getDefaults().isFiled() || 1 == taskInfo.getDefaults().getState())
//            return false;
        Map<String, String> params = new HashMap<>();
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ignored) {
            return false;
        }
        String chapterId = taskInfo.getDefaults().getChapterId();
        if (!Optional.ofNullable(chapterId).orElse("").isEmpty()) {
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
        if (!Optional.ofNullable(videoInfo.getDtoken()).orElse("").isEmpty())
            response = session.get(taskInfo.getDefaults().getReportUrl() + "/" + videoInfo.getDtoken()).params(params).followRedirect(false).proxy(proxy).send();
        else
            response = session.get(taskInfo.getDefaults().getReportUrl()).params(params).followRedirect(false).proxy(proxy).send();
        if (response.getStatusCode() == StatusCodes.FOUND)
            throw new CheckCodeException(session, response.getHeader("location"));
        return JSON.parseObject(response.readToText()).getBoolean("isPassed");
    }
}
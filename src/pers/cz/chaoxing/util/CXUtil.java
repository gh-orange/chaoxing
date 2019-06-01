package pers.cz.chaoxing.util;

import com.alibaba.fastjson.*;
import net.dongliu.requests.Cookie;
import net.dongliu.requests.Parameter;
import net.dongliu.requests.Response;
import net.dongliu.requests.StatusCodes;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pers.cz.chaoxing.common.OptionInfo;
import pers.cz.chaoxing.common.ReadInfo;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.quiz.QuizInfo;
import pers.cz.chaoxing.common.quiz.data.QuizData;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizConfig;
import pers.cz.chaoxing.common.quiz.data.exam.ExamQuizData;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizConfig;
import pers.cz.chaoxing.common.quiz.data.homework.HomeworkQuizData;
import pers.cz.chaoxing.common.quiz.data.player.VideoQuizData;
import pers.cz.chaoxing.common.school.SchoolInfo;
import pers.cz.chaoxing.common.task.ReadCardInfo;
import pers.cz.chaoxing.common.task.TaskConfig;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.common.task.data.TaskData;
import pers.cz.chaoxing.common.task.data.exam.ExamDataProperty;
import pers.cz.chaoxing.common.task.data.exam.ExamTaskData;
import pers.cz.chaoxing.common.task.data.homework.HomeworkTaskData;
import pers.cz.chaoxing.common.task.data.player.PlayerTaskData;
import pers.cz.chaoxing.common.task.data.player.ReadTaskData;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.ExamStateException;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.util.io.StringUtil;
import pers.cz.chaoxing.util.net.ApiURL;
import pers.cz.chaoxing.util.net.JsoupResponseHandler;
import pers.cz.chaoxing.util.net.NetUtil;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class CXUtil {
    private static final Type TASK_INFO_TYPE = new TypeReference<TaskInfo<TaskData>>() {
    }.getType();

    private static final Type PLAYER_INFO_TYPE = new TypeReference<TaskInfo<PlayerTaskData>>() {
    }.getType();

    private static final Type HOMEWORK_INFO_TYPE = new TypeReference<TaskInfo<HomeworkTaskData>>() {
    }.getType();

    private static final Type PLAYER_QUIZ_INFO_TYPE = new TypeReference<List<QuizInfo<VideoQuizData, Void>>>() {
    }.getType();

    public static final JsoupResponseHandler RESPONSE_HANDLER = new JsoupResponseHandler();

    public static SchoolInfo searchSchool(String schoolName) throws CheckCodeException {
        Document document = NetUtil.get(ApiURL.LOGIN_NORMAL_ABS.buildURL()).toResponse(RESPONSE_HANDLER).getBody();
        if (document.wholeText().isEmpty())
            return new SchoolInfo(false);
        String productId = document.getElementById("productid").val();
        Map<String, String> body = new HashMap<>();
        if (!productId.isEmpty())
            body.put("productid", productId);
        body.put("pid", document.getElementById("pid").val());
        body.put("allowJoin", "0");
        body.put("filter", schoolName);
        try {
            return NetUtil.post(ApiURL.SCHOOL_SEARCH.buildURL(document.baseUri()), body).toJsonResponse(SchoolInfo.class).getBody();
        } catch (JSONException ignored) {
        }
        return new SchoolInfo(false);
    }

    public static String getNotice(int fid) throws CheckCodeException {
        try {
            return Optional.ofNullable(JSON.parseObject(NetUtil.get(ApiURL.SCHOOL_NOTICE_ABS.buildURL(fid)).toTextResponse().getBody()).getString("content"))
                    .map(s -> s.replace("</p>", "\n</p>"))
                    .map(Jsoup::parse)
                    .map(Element::wholeText)
                    .map(String::trim)
                    .orElse("");
        } catch (JSONException ignored) {
        }
        return "";
    }

    public static String login(int fid, String username, String password, String checkCode) throws WrongAccountException, CheckCodeException {
        Document document = NetUtil.get(ApiURL.LOGIN_SCHOOL_ABS.buildURL(fid)).toResponse(RESPONSE_HANDLER).getBody();
        if (document.wholeText().isEmpty())
            return "";
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
        Response<String> response = NetUtil.post(document.selectFirst("form#form").absUrl("action"), body).toTextResponse();
        if (response.getBody().contains("密码错误") || response.getBody().contains("参数为空"))
            throw new WrongAccountException();
        if (response.getBody().contains("用户登录"))
            throw new CheckCodeException(ApiURL.LOGIN_CHECK_CODE.buildURL(NetUtil.getOriginal(response.getURL()), System.currentTimeMillis()));
        return response.getURL();
    }

    public static String getCoursesURL(String indexURL) throws CheckCodeException {
        Document document = NetUtil.get(indexURL, 0).toResponse(RESPONSE_HANDLER).getBody();
        NetUtil.get(document.selectFirst("div.headbanner_new script").absUrl("src"));
        return NetUtil.get(document.selectFirst("div.mainright iframe").absUrl("src"), 1).getURL();
    }

    public static List<String> getCourseURLs(String coursesURL) throws CheckCodeException {
        Document document = NetUtil.get(coursesURL, 0).toResponse(RESPONSE_HANDLER).getBody();
        if (document.baseUri().contains("chaoxing.com"))
            document.setBaseUri(document.baseUri().replaceFirst("http://", "https://"));
        return document.select("div.httpsClass.Mconright a").stream()
                .map(element -> element.absUrl("href"))
                .collect(Collectors.toList());
    }

    public static Map<String, List<String>> getCourseInfo(String courseURL) throws CheckCodeException {
        Document document = NetUtil.get(courseURL, 0).toResponse(RESPONSE_HANDLER).getBody();
        document.select("script[method=get]").stream()
                .filter(script -> !script.hasText())
                .forEach(script -> {
                    try {
                        NetUtil.get(script.absUrl("src"));
                    } catch (Exception ignored) {
                    }
                });
        document.select("script[src~=log]").stream()
                .map(script -> script.absUrl("src"))
                .forEach(src -> {
                    try {
                        NetUtil.get(src).toTextResponse().getBody().contains("success");
                    } catch (Exception ignored) {
                    }
                });
        Map<String, List<String>> courseInfo = new HashMap<>();
        courseInfo.put("chapterURLs", document.select("h3.clearfix").stream()
                .filter(element -> !Optional.ofNullable(element.selectFirst("em.openlock")).isPresent())
                .map(element -> element.select("span.articlename a"))
                .flatMap(Collection::stream)
                .map(element -> element.absUrl("href"))
                .collect(Collectors.toList()));
        courseInfo.put("examURLs", document.select("div.navshow ul li a:contains(考试)").stream()
                .map(element -> element.absUrl("data"))
                .collect(Collectors.toList()));
        return courseInfo;
    }

    public static String getUtEnc(String taskURL) throws CheckCodeException {
        Document document = NetUtil.get(taskURL, 0).toResponse(RESPONSE_HANDLER).getBody();
        Elements scripts = document.select("script");
        scripts.select("[src~=log]").forEach(script -> {
            try {
                NetUtil.get(script.absUrl("src"), 0).toTextResponse().getBody().contains("success");
            } catch (Exception ignored) {
            }
        });
        return StringUtil.subStringBetweenFirst(scripts.html(), "utEnc=\"", "\";");
    }

    public static <T extends TaskData> TaskInfo<T> getTaskInfo(String taskOrExamURL, InfoType infoType) throws CheckCodeException {
        final Map<String, String> body = NetUtil.getQueries(taskOrExamURL).stream().collect(Collectors.toMap(Parameter::getName, Parameter::getValue));
        if (infoType.equals(InfoType.EXAM))
            return (TaskInfo<T>) CXUtil.getExamInfo(taskOrExamURL, body);
        body.put("verificationcode", body.remove("code"));
        Document titleDocument = NetUtil.post(ApiURL.TITLE_INFO.buildURL(NetUtil.getOriginal(taskOrExamURL)), body, 0).toResponse(RESPONSE_HANDLER).getBody();
        Document taskDocument;
        boolean isLocked = false;
        do {
            titleDocument.select("script[src~=log]").stream()
                    .map(script -> script.absUrl("src"))
                    .forEach(src -> {
                        try {
                            NetUtil.getQueries(src).stream().filter(parameter -> parameter.getName().equals("personid")).map(Parameter::getValue).findAny().ifPresent(cpi -> body.put("cpi", cpi));
                            NetUtil.get(src + "&_=" + System.currentTimeMillis()).toTextResponse().getBody().contains("success");
                        } catch (Exception ignored) {
                        }
                    });
            if (Optional.ofNullable(titleDocument.getElementById("mainid")).map(Element::html).map(s -> s.contains("showChapterVerificationCode")).orElse(false))
                throw new CheckCodeException(ApiURL.CHAPTER_CHECK_CODE_IMG.buildURL(NetUtil.getOriginal(taskOrExamURL), body.get("cpi")));
            taskDocument = NetUtil.get(ApiURL.TASK_INFO.buildURL(NetUtil.getOriginal(taskOrExamURL),
                    body.get("clazzid"),
                    body.get("courseId"),
                    body.get("chapterId"),
                    body.get("cpi"),
                    infoType.getId()
            ), 0).toResponse(RESPONSE_HANDLER).getBody();
            if (Optional.ofNullable(taskDocument.getElementById("openlock")).isPresent() && titleDocument.getElementById("cardcount").val().equals("1"))
                if (NetUtil.get(ApiURL.CHAPTER_LOCK_OPEN.buildURL(NetUtil.getOriginal(taskOrExamURL),
                        body.get("clazzid"),
                        body.get("courseId"),
                        body.get("chapterId"))).toTextResponse().getBody().contains("true")) {
                    isLocked = true;
                    body.put("knowledgestr", Optional.ofNullable(taskDocument.getElementById("knowledgestr")).map(Element::val).orElse(""));
                    /*
                    previous: 0 next: 1
                     */
                    body.put("type", "1");
                    body.put("date", ZonedDateTime.now().format(DateTimeFormatter.ofPattern("EEE MMM dd uuuu HH:mm:ss 'GMT'Z (中国标准时间)", Locale.ENGLISH)));
                    titleDocument = NetUtil.post(ApiURL.CHAPTER_CHANGE.buildURL(NetUtil.getOriginal(taskOrExamURL)), body, 0).toResponse(RESPONSE_HANDLER).getBody();
                }
        } while (isLocked);
        String jsonStr = StringUtil.subStringBetweenFirst(StringUtil.subStringAfterFirst(taskDocument.html(), "try{"), "mArg = ", ";");
        try {
            switch (infoType) {
                case PLAYER:
                    return JSON.parseObject(jsonStr, PLAYER_INFO_TYPE);
                case HOMEWORK:
                    return JSON.parseObject(jsonStr, HOMEWORK_INFO_TYPE);
                default:
                    return JSON.parseObject(jsonStr, TASK_INFO_TYPE);
            }
        } catch (JSONException e) {
            TaskInfo<T> taskInfo = new TaskInfo<>();
            switch (infoType) {
                case PLAYER:
                    taskInfo.setAttachments((T[]) new PlayerTaskData[]{new PlayerTaskData(true)});
                    break;
                case HOMEWORK:
                    taskInfo.setAttachments((T[]) new HomeworkTaskData[]{new HomeworkTaskData()});
                    break;
            }
            return taskInfo;
        }
    }

    public static VideoInfo getVideoInfo(String taskURL, String objectId, String fid) throws CheckCodeException {
        return NetUtil.get(ApiURL.VIDEO_INFO.buildURL(
                NetUtil.getOriginal(taskURL),
                objectId,
                fid,
                System.currentTimeMillis()
        ), 0).toJsonResponse(VideoInfo.class).getBody();
    }

    public static ReadInfo getReadInfo(String taskURL, String clazzId, String courseId, String knowledgeId, String jobId, String enc, String utEnc) throws CheckCodeException {
        ReadInfo readInfo = new ReadInfo();
        Document document = NetUtil.get(ApiURL.READ_INFO.buildURL(NetUtil.getOriginal(taskURL),
                clazzId,
                courseId,
                knowledgeId,
                jobId,
                enc,
                utEnc)).toResponse(RESPONSE_HANDLER).getBody();
        String times = document.select("div.content p").text();
        Matcher matcher = Pattern.compile("[\\d.]+").matcher(times);
        if (matcher.find())
            readInfo.setDuration((int) (Float.parseFloat(matcher.group()) * 60));
        if (matcher.find())
            readInfo.setHeadOffset((int) (Float.parseFloat(matcher.group()) * 60));
        readInfo.setFrom(StringUtil.subStringBetweenFirst(document.select("script").html().replaceAll("\\s", ""), "from=\"", "\";"));
        return readInfo;
    }

    public static void getListInfo(String taskURL, String clazzId, String courseId, String chapterId) throws CheckCodeException {
        NetUtil.get(ApiURL.LIST_INFO.buildURL(NetUtil.getOriginal(taskURL),
                clazzId,
                courseId,
                chapterId
        ), 0, response -> !Objects.requireNonNull(response.getHeader("location")).contains("study"));
    }

    /**
     * onCheckCode since player loaded
     *
     * @param taskURL
     * @return
     * @throws CheckCodeException
     */
    public static boolean startPlayer(String taskURL) throws CheckCodeException {
        final Map<String, String> params = NetUtil.getQueries(taskURL).stream().collect(Collectors.toMap(Parameter::getName, Parameter::getValue));
        return NetUtil.get(ApiURL.PLAYER_VALIDATE.buildURL(NetUtil.getOriginal(taskURL), params.get("clazzid"), params.get("courseId"), params.get("chapterId"))).toTextResponse().getBody().contains("true");
    }

    public static boolean startExam(String examURL, TaskInfo<ExamTaskData> taskInfo, ExamTaskData attachment) throws CheckCodeException, ExamStateException {
        try {
            JSONObject result = NetUtil.get(ApiURL.EXAM_VALIDATE.buildURL(NetUtil.getOriginal(examURL),
                    taskInfo.getDefaults().getClazzId(),
                    taskInfo.getDefaults().getCourseid(),
                    attachment.getProperty().gettId(),
                    attachment.getProperty().getEndTime(),
                    attachment.getProperty().getMoocTeacherId(),
                    attachment.getProperty().getCpi()
            )).toJsonResponse(JSONObject.class).getBody();
            switch (result.getIntValue("status")) {
                case 0:
                    throw new ExamStateException(result.getIntValue("finishStandard"));
                case 1:
                    throw new CheckCodeException(ApiURL.EXAM_CHECK_CODE_IMG.buildURL(NetUtil.getOriginal(examURL)));
                case 2:
                    return true;
            }
        } catch (JSONException ignored) {
        }
        return false;
    }

    public static ReadCardInfo onReadStart(TaskInfo<ReadTaskData> taskInfo, ReadTaskData attachment, ReadInfo readInfo) throws CheckCodeException {
        try {
            JSON.parseObject(NetUtil.get(ApiURL.READ_VALIDATE.buildURL(NetUtil.getOriginal(taskInfo.getDefaults().getInitdataUrl()),
                    taskInfo.getDefaults().getClazzId(),
                    taskInfo.getDefaults().getCourseid(),
                    taskInfo.getDefaults().getKnowledgeid(),
                    attachment.getProperty().getJobid(),
                    attachment.getJtoken(),
                    System.currentTimeMillis())).toTextResponse().getBody()).getBooleanValue("status");
        } catch (JSONException ignored) {
        }
        Document document = NetUtil.get(ApiURL.READ_OPEN.buildURL(NetUtil.getOriginal(taskInfo.getDefaults().getInitdataUrl()),
                attachment.getProperty().getId(),
                readInfo.getFrom())).toResponse(RESPONSE_HANDLER).getBody();
        int height = (int) (document.getElementById("courseMainBox").getElementsByAttribute("height").eachAttr("height").stream()
                        .map(value -> value.replaceAll("[^-\\d.]", ""))
                        .mapToDouble(Double::parseDouble)
                        .sum() + 15 + 1 + 20);
        String nextKnowledgeId = StringUtil.subStringBetweenFirst(document.select("script").html().replaceAll("\\s", ""),
                "ctid=", ";");
        return new ReadCardInfo(nextKnowledgeId, 0, height, nextKnowledgeId);
    }

    public static ReadCardInfo getReadCard(String taskURL, ReadInfo readInfo, ReadCardInfo readCardInfo) throws CheckCodeException {
        Document document = NetUtil.get(ApiURL.READ_CARD.buildURL(NetUtil.getOriginal(taskURL),
                readInfo.getCourseId(),
                readCardInfo.getNextKnowledgeId(),
                readInfo.getFrom())).toResponse(RESPONSE_HANDLER).getBody();
        int height = document.wholeText().getBytes().length / 90;
        String nextKnowledgeId = StringUtil.subStringBetweenFirst(document.getElementById("loadbutton").attr("onclick"),
                "(", ")");
        return new ReadCardInfo(readCardInfo.getNextKnowledgeId(), readCardInfo.getStart() + readCardInfo.getHeight(), height, nextKnowledgeId);
    }

    public static void onReadProgress(String taskURL, ReadInfo readInfo, ReadCardInfo readCardInfo,int height) throws CheckCodeException {
        JSON.parseObject(NetUtil.get(ApiURL.READ_PROCESS.buildURL(NetUtil.getOriginal(taskURL),
                readInfo.getCourseId(),
                readCardInfo.getKnowledgeId(),
                readInfo.getFrom(),
                readInfo.getHeight(),
                height)).toTextResponse().getBody());
    }

    /**
     * onCheckCode since video loaded first
     *
     * @param taskInfo
     * @param videoInfo
     * @return
     * @throws CheckCodeException
     */
    public static boolean onVideoStart(TaskInfo<PlayerTaskData> taskInfo, PlayerTaskData attachment, VideoInfo videoInfo) throws CheckCodeException {
        return sendVideoLog(taskInfo, attachment, videoInfo, (int) (attachment.getHeadOffset() / 1000), 0);
    }

    /**
     * onCheckCode since video finished
     *
     * @param taskInfo
     * @param videoInfo
     * @return
     * @throws CheckCodeException
     */
    public static boolean onVideoEnd(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo) throws CheckCodeException {
        return sendVideoLog(taskInfo, attachment, videoInfo, videoInfo.getDuration(), 4);
    }

    /**
     * onCheckCode since video clicked to play
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     * @throws CheckCodeException
     */
    public static boolean onVideoPlay(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        return sendVideoLog(taskInfo, attachment, videoInfo, playSecond, 0);
    }

    /**
     * onCheckCode since video clicked to pause
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     * @throws CheckCodeException
     */
    public static boolean onVideoPause(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        if (!Optional.ofNullable(taskInfo.getDefaults().getChapterId()).orElse("").isEmpty())
            return sendVideoLog(taskInfo, attachment, videoInfo, playSecond, 0);
        return false;
    }

    /**
     * onCheckCode each intervalTime since video playing
     *
     * @param taskInfo
     * @param videoInfo
     * @param playSecond
     * @return
     * @throws CheckCodeException
     */
    public static boolean onVideoProgress(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        return sendVideoLog(taskInfo, attachment, videoInfo, playSecond, 0);
    }

    /**
     * onCheckCode since player start playing
     *
     * @param initDataUrl
     * @param mid
     * @return
     * @throws CheckCodeException
     */
    public static List<QuizInfo<VideoQuizData, Void>> getVideoQuizzes(String initDataUrl, String mid) throws CheckCodeException {
        Response<String> response = NetUtil.get(ApiURL.VIDEO_QUIZ.buildURL(initDataUrl, mid), 0).toTextResponse();
        List<QuizInfo<VideoQuizData, Void>> quizInfoList = JSON.parseArray(response.getBody()).toJavaObject(PLAYER_QUIZ_INFO_TYPE);
        final String baseURL = NetUtil.getOriginal(response.getURL());
        quizInfoList.stream()
                .map(QuizInfo::getDatas)
                .flatMap(Arrays::stream)
                .forEach(quizData -> quizData.setValidationUrl(baseURL + quizData.getValidationUrl()));
        return quizInfoList;
    }

    public static QuizInfo<HomeworkQuizData, HomeworkQuizConfig> getHomeworkQuiz(String taskURL, TaskInfo<HomeworkTaskData> taskInfo, HomeworkTaskData attachment) throws CheckCodeException {
        Document document = NetUtil.get(ApiURL.HOMEWORK_QUIZ.buildURL(NetUtil.getOriginal(taskURL),
                taskInfo.getDefaults().getClazzId(),
                taskInfo.getDefaults().getCourseid(),
                taskInfo.getDefaults().getKnowledgeid(),
                attachment.getProperty().getWorkid(),
                attachment.getJobid(),
                attachment.getEnc(),
                attachment.getUtEnc(),
                "workB".equals(attachment.getProperty().getWorktype()) ? "b" : ""
        ), 2, response -> !Optional.ofNullable(response.getHeader("location")).orElse("work").contains("work")).toResponse(RESPONSE_HANDLER).getBody();
        Element element = document.selectFirst("div.CeYan");
        final Elements questions = element.select("div.TiMu");
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
            homeworkQuizInfo.getDefaults().setAnswerwqbid(StringUtil.subStringBetweenLast(StringUtil.subStringBeforeFirst(document.html(), "$(\"#answerwqbid\")"), "= \"", "\""));
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
            Optional.ofNullable(element.selectFirst("form#form1")).ifPresent(form -> homeworkQuizInfo.getDatas()[i].setValidationUrl(form.absUrl("action")));
            if (Optional.ofNullable(homeworkQuizInfo.getDatas()[i].getValidationUrl()).orElse("").isEmpty())
                Optional.ofNullable(questions.get(i).selectFirst("form[id~=questionErrorForm]")).ifPresent(errorForm -> homeworkQuizInfo.getDatas()[i].setValidationUrl(errorForm.absUrl("action")));
            Optional.ofNullable(questions.get(i).selectFirst("input[id~=answertype]")).ifPresent(inputAnswerType -> {
                Element inputAnswerCheck = inputAnswerType.previousElementSibling();
                homeworkQuizInfo.getDatas()[i].setAnswerTypeId(inputAnswerType.id());
                if (inputAnswerCheck.tagName().equals("input"))
                    homeworkQuizInfo.getDatas()[i].setAnswerCheckName(inputAnswerCheck.attr("name"));
                homeworkQuizInfo.getDatas()[i].setQuestionType(inputAnswerType.val());
            });
            homeworkQuizInfo.getDatas()[i].setDescription(questions.get(i).selectFirst("div.Zy_TItle div.clearfix").text());
            Elements ul = questions.get(i).getElementsByTag("ul");
            if (ul.isEmpty()) {
                homeworkQuizInfo.getDatas()[i].setOptions(new OptionInfo[]{new OptionInfo()});
                String answerStr = questions.get(i).selectFirst("div.Py_answer i").text();
                if (answerStr.matches("(?i)[√✓✔对是]|正确|T(RUE)?|Y(ES)?|RIGHT|CORRECT"))
                    answerStr = "true";
                else if (answerStr.matches("(?i)[X×✖错否]|错误|F(ALSE)?|N(O)?|WRONG|INCORRECT"))
                    answerStr = "false";
                homeworkQuizInfo.getDatas()[i].getOptions()[0].setDescription(answerStr);
                homeworkQuizInfo.getDatas()[i].getOptions()[0].setName(answerStr);
            } else {
                Elements lis = ul.first().getElementsByTag("li");
                homeworkQuizInfo.getDatas()[i].setOptions(new OptionInfo[lis.size()]);
                IntStream.range(0, lis.size()).forEach(j -> {
                    homeworkQuizInfo.getDatas()[i].getOptions()[j] = new OptionInfo();
                    Optional.ofNullable(lis.get(j).selectFirst("label input")).ifPresent(inputAnswer -> {
                        if (Optional.ofNullable(homeworkQuizInfo.getDatas()[i].getAnswerId()).orElse("").isEmpty())
                            homeworkQuizInfo.getDatas()[i].setAnswerId(inputAnswer.attr("name"));
                        homeworkQuizInfo.getDatas()[i].getOptions()[j].setRight(inputAnswer.hasAttr("checked"));
                        if (homeworkQuizInfo.getDatas()[i].getOptions()[j].isRight())
                            homeworkQuizInfo.getDatas()[i].setAnswered(true);
                        homeworkQuizInfo.getDatas()[i].getOptions()[j].setName(inputAnswer.val());
                    });
                    if (Optional.ofNullable(homeworkQuizInfo.getDatas()[i].getOptions()[j].getName()).orElse("").isEmpty())
                        homeworkQuizInfo.getDatas()[i].getOptions()[j].setName(lis.get(j).selectFirst("i").text().replaceAll("、", ""));
                    if (!lis.isEmpty())
                        homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription(lis.get(j).select("a").text());
                    if (Optional.ofNullable(homeworkQuizInfo.getDatas()[i].getOptions()[j].getDescription()).orElse("").isEmpty())
                        homeworkQuizInfo.getDatas()[i].getOptions()[j].setDescription(homeworkQuizInfo.getDatas()[i].getOptions()[j].getName());
                });
            }
        });
        return homeworkQuizInfo;
    }

    public static QuizInfo<ExamQuizData, ExamQuizConfig> getExamQuiz(String examURL, QuizInfo<ExamQuizData, ExamQuizConfig> examQuizInfo) throws CheckCodeException {
        Document document = NetUtil.get(ApiURL.EXAM_QUIZ.buildURL(NetUtil.getOriginal(examURL),
                examQuizInfo.getDefaults().getClassId(),
                examQuizInfo.getDefaults().getCourseId(),
                examQuizInfo.getDefaults().gettId(),
                examQuizInfo.getDefaults().getTestUserRelationId(),
                examQuizInfo.getDefaults().getExamsystem(),
                examQuizInfo.getDefaults().getEnc(),
                examQuizInfo.getDefaults().getStart(),
                examQuizInfo.getDefaults().getRemainTime(),
                examQuizInfo.getDefaults().getEncLastUpdateTime(),
                examQuizInfo.getDefaults().getCpi()
        ), 0).toResponse(RESPONSE_HANDLER).getBody();
        Element form = document.selectFirst("form#submitTest");
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

    public static boolean storeHomeworkQuiz(String taskURL, HomeworkQuizConfig defaults, Map<HomeworkQuizData, List<OptionInfo>> answers) throws CheckCodeException, WrongAccountException {
        defaults.setPyFlag("1");
        return answerHomeworkQuiz(taskURL, defaults, answers);
    }

    public static boolean storeExamQuiz(ExamQuizConfig defaults, Map<ExamQuizData, List<OptionInfo>> answers) throws CheckCodeException {
        defaults.setTempSave(true);
        return answerExamQuiz(defaults, answers);
    }

    public static boolean answerVideoQuiz(String validationUrl, String resourceId, String answer) throws CheckCodeException {
        JSONObject jsonObject = NetUtil.get(ApiURL.VIDEO_ANSWER.buildURL(validationUrl, resourceId, "'" + answer + "'"), 0).toJsonResponse(JSONObject.class).getBody();
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
    public static boolean answerHomeworkQuiz(String taskURL, HomeworkQuizConfig defaults, Map<HomeworkQuizData, List<OptionInfo>> answers) throws CheckCodeException, WrongAccountException {
        HomeworkQuizData first = null;
        Iterator<HomeworkQuizData> iterator = answers.keySet().iterator();
        if (iterator.hasNext())
            first = iterator.next();
        if (!Optional.ofNullable(first).isPresent())
            return false;
        if (Optional.ofNullable(defaults.getEnc()).orElse("").isEmpty()) {
            JSONObject jsonObject = NetUtil.get(ApiURL.HOMEWORK_VALIDATE.buildURL(NetUtil.getOriginal(taskURL),
                    defaults.getClassId(),
                    defaults.getCourseId(),
                    System.currentTimeMillis()
            ), 0).toJsonResponse(JSONObject.class).getBody();
            switch (jsonObject.getIntValue("status")) {
                case 1:
                    throw new WrongAccountException();
                case 2:
                    throw new CheckCodeException(ApiURL.HOMEWORK_CHECK_CODE_IMG.buildURL(NetUtil.getOriginal(taskURL)));
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
        Response<String> response = NetUtil.post(ApiURL.HOMEWORK_ANSWER.buildURL(first.getValidationUrl(),
                version,
                pos,
                random,
                value,
                defaults.getWorkRelationId()
        ), body, 0).toTextResponse();
        if (response.getStatusCode() != StatusCodes.OK)
            return false;
        return !response.getBody().contains("提交失败") && !response.getBody().contains("false");
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
        int version = 1;
        Matcher matcher = Pattern.compile("version=(\\d)").matcher(first.getValidationUrl());
        if (matcher.find())
            version += Integer.valueOf(matcher.group(1));
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
        HashMap<String, String> body = new HashMap<>();
        body.put("userId", defaults.getUserId());
        body.put("classId", defaults.getClassId());
        body.put("courseId", defaults.getCourseId());
        body.put("tId", defaults.gettId());
        body.put("testUserRelationId", defaults.getTestUserRelationId());
        body.put("examsystem", defaults.getExamsystem());
        body.put("enc", defaults.getEnc());
        body.put("cpi", defaults.getCpi());
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
                    .flatMap(Collection::stream)
                    .collect(Collectors.toList());
            if (options.isEmpty())
                options = answers.keySet().stream()
                        .map(ExamQuizData::getOptions)
                        .flatMap(Arrays::stream)
                        .filter(OptionInfo::isRight)
                        .collect(Collectors.toList());
            options.stream()
                    .map(OptionInfo::getName)
                    .forEach(name -> body.put(new String(("answer" + body.get("questionId")).getBytes()), name));
        }
        String responseStr = NetUtil.post(ApiURL.EXAM_ANSWER.buildURL(first.getValidationUrl(),
                defaults.isTempSave() ? "true" : "false",
                version,
                pos.toString(),
                random,
                value,
                first.getQuestionId()), body, 0).toTextResponse().getBody();
        if (!defaults.isTempSave())
            return responseStr.contains("success");
        try {
            String[] results = Optional.ofNullable(JSON.parseObject(responseStr).getString("data")).orElse(responseStr).split("\\|");
            if (results.length != 3)
                return false;
            defaults.setEncLastUpdateTime(Long.parseLong(results[0]));
            defaults.setRemainTime(Integer.parseInt(results[1]));
            defaults.setEncRemainTime(defaults.getRemainTime());
            defaults.setEnc(results[2]);
            return true;
        } catch (JSONException ignored) {
        }
        return false;
    }

    public static List<OptionInfo> getQuizAnswer(QuizData quizData) {
        List<OptionInfo> options = new ArrayList<>(getAnswersFrom3GMFW(quizData));
        if (options.isEmpty())
            options.addAll(getAnswersFromTensorFlow(quizData));
        return options;
    }

    /**
     * Thanks to m.3gmfw.cn for database support
     *
     * @param quizData
     * @return
     */
    private static List<OptionInfo> getAnswersFrom3GMFW(QuizData quizData) {
        List<OptionInfo> options = new ArrayList<>();
        String[] descriptions = quizData.getDescription().replaceAll("【.*?】", "").split("[\\pP\\pS\\pZ]");
        StringBuilder stringBuilder = new StringBuilder();
        Arrays.stream(descriptions, 0, descriptions.length > 8 ? descriptions.length / 2 : descriptions.length).forEach(stringBuilder::append);
        try {
            Document document;
            /*
            circumvent protection
             */
            while (true) {
                document = NetUtil.get(ApiURL.ANSWER_QUIZ_3GMFW_ABS.buildURL(stringBuilder.toString())).toResponse(RESPONSE_HANDLER).getBody();
                Element form = document.selectFirst("form#challenge-form");
                if (!Optional.ofNullable(form).isPresent())
                    break;
                try {
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
                    Thread.sleep(4 * 1000);
                    NetUtil.get(form.absUrl("action") +
                            "?jschl_answer=" + answer +
                            "&jschl_vc=" + form.select("input[name=jschl_vc]").val() +
                            "&pass=" + form.select("input[name=pass]").val()
                    );
                } catch (Exception e) {
                    break;
                }
            }
            Optional.ofNullable(document.selectFirst("div.searchTopic")).ifPresent(div -> {
                try {
                    Elements p = NetUtil.get(div.selectFirst("a").absUrl("href")).toResponse(RESPONSE_HANDLER.setDefaultCharset("gbk")).getBody().select("div.content p");
                    Map<String, String> answers = new HashMap<>();
                    p.stream()
                            .map(Element::textNodes)
                            .flatMap(Collection::stream)
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
                    String rightAnswerStr = p.last().text();
                    if (rightAnswerStr.contains("答案："))
                        rightAnswerStr = rightAnswerStr.substring(rightAnswerStr.indexOf("答案：") + "答案：".length()).trim();
                    if (rightAnswerStr.matches("(?i)[√✓✔对是]|正确|T(RUE)?|Y(ES)?|RIGHT|CORRECT"))
                        rightAnswerStr = "true";
                    else if (rightAnswerStr.matches("(?i)[X×✖错否]|错误|F(ALSE)?|N(O)?|WRONG|INCORRECT"))
                        rightAnswerStr = "false";
                    else {
                        List<String> rightAnswerValues = rightAnswerStr.replaceAll("\\s", "").chars()
                                .mapToObj(i -> Character.toString((char) i))
                                .map(answers::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        for (OptionInfo option : quizData.getOptions()) {
                            if (rightAnswerValues.stream().anyMatch(rightValue -> rightValue.contains(option.getDescription()))) {
                                options.add(new OptionInfo(option));
                                if (!quizData.getQuestionType().equals("1"))
                                    break;
                            }
                        }
                    }
                    if (options.isEmpty())
                        for (OptionInfo option : quizData.getOptions())
                            if (rightAnswerStr.equalsIgnoreCase(option.getName())) {
                                options.add(new OptionInfo(option));
                                break;
                            }
                } catch (CheckCodeException ignored) {
                }
            });
        } catch (CheckCodeException ignored) {
        }
        return options;
    }

    /**
     * Thanks to api.tensor-flow.club for database support
     *
     * @param quizData
     * @return
     */
    private static List<OptionInfo> getAnswersFromTensorFlow(QuizData quizData) {
        List<OptionInfo> options = new ArrayList<>();
        try {
            String responseStr;
            while (true) {
                responseStr = NetUtil.get(ApiURL.ANSWER_QUIZ_TF_ABS.buildURL(StringUtil.subStringBeforeFirst(quizData.getDescription().replaceAll("【.*?】", "").trim(), "%"))).toTextResponse().getBody();
                if (!responseStr.contains("try again later"))
                    break;
                try {
                    Thread.sleep(4 * 1000);
                } catch (Exception e) {
                    break;
                }
            }
            JSONArray jsonArray = JSON.parseArray(responseStr);
            if (!jsonArray.isEmpty()) {
                String rightAnswerStr = jsonArray.getJSONObject(0).getString("answer").trim();
                if (rightAnswerStr.matches("(?i)[√✓✔对是]|正确|T(RUE)?|Y(ES)?|RIGHT|CORRECT"))
                    rightAnswerStr = "true";
                else if (rightAnswerStr.matches("(?i)[X×✖错否]|错误|F(ALSE)?|N(O)?|WRONG|INCORRECT"))
                    rightAnswerStr = "false";
                else {
                    List<String> rightAnswerValues = Arrays.asList(rightAnswerStr.split("[#\\s]"));
                    for (OptionInfo option : quizData.getOptions()) {
                        if (rightAnswerValues.stream().anyMatch(rightValue -> rightValue.contains(option.getDescription()))) {
                            options.add(new OptionInfo(option));
                            if (!quizData.getQuestionType().equals("1"))
                                break;
                        }
                    }
                }
                if (options.isEmpty())
                    for (OptionInfo option : quizData.getOptions())
                        if (rightAnswerStr.equalsIgnoreCase(option.getName())) {
                            options.add(new OptionInfo(option));
                            break;
                        }
            }
        } catch (CheckCodeException | JSONException ignored) {
        }
        return options;
    }

    private static TaskInfo<ExamTaskData> getExamInfo(String examURL, Map<String, String> params) throws CheckCodeException {
        Document document = NetUtil.get(examURL, 0).toResponse(RESPONSE_HANDLER).getBody();
        Elements lis = document.selectFirst("div.ulDiv ul").getElementsByTag("li");
        String classId = document.getElementById("classId").val();
        String moocTeacherId = document.getElementById("moocTeacherId").val();
        String examsystem = document.getElementById("examsystem").val();
        String examEnc = document.getElementById("examEnc").val();
        String cpi = document.getElementById("cpi").val();
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
                examInfo.getAttachments()[i].getProperty().setEndTime(funcParams[3].replaceAll("'", ""));
            } else {
                examInfo.getDefaults().setCourseid(params.get("courseId"));
                examInfo.getAttachments()[i].getProperty().setEndTime(StringUtil.subStringBetweenFirst(StringUtil.subStringAfterFirst(statusStr, "时间："), "至", "考试").trim().replaceAll("'", ""));
            }
            examInfo.getAttachments()[i].getProperty().setMoocTeacherId(moocTeacherId);
            examInfo.getAttachments()[i].getProperty().setExamsystem(examsystem);
            examInfo.getAttachments()[i].getProperty().setExamEnc(examEnc);
            examInfo.getAttachments()[i].getProperty().setCpi(cpi);
            examInfo.getAttachments()[i].getProperty().setTitle(dataElement.attr("title"));
        });
        return examInfo;
    }

    /**
     * JavaScript Code:
     * var sendLog_ = function (player, isdrag, currentTimeSec, callback) {
     * if (!params.reportUrl) {
     * return
     * }
     * var format = '[{0}][{1}][{2}][{3}][{4}][{5}][{6}][{7}]',
     * clipTime = (params.startTime || '0') + '_' + (params.endTime || params.duration);
     * var enc = Ext.String.format(format, params.clazzId, params.userid, params.jobid, params.objectId, currentTimeSec * 1000, 'd_yHJ!$pdA~5', params.duration * 1000, clipTime);
     * var rurl = [params.reportUrl, '/', params.dtoken, '?clazzId=', params.clazzId, '&playingTime=', currentTimeSec, '&duration=', params.duration, '&clipTime=', clipTime, '&objectId=', params.objectId, '&otherInfo=', params.otherInfo, '&jobid=', params.jobid, '&userid=', params.userid, '&isdrag=', isdrag, '&view=pc', '&enc=', md5(enc), '&rt=', params.rt, '&dtype=Video'].join('');
     * logFunc(player, rurl, callback)
     * };
     * {
     * singleton: function (c) {
     * var f = this,
     * e = parseInt(Math.random() * 9999999);
     * c.on('play', function () {
     * f.setCookie('videojs_id', e)
     * });
     * c.setInterval(function () {
     * var g = f.getCookie('videojs_id');
     * if (g != e) {
     * c.pause()
     * }
     * }, 1000)
     * }
     * setCookie: function (f, h) {
     * var c = arguments,
     * k = arguments.length,
     * e = (k > 2) ? c[2] : null,
     * j = (k > 3) ? c[3] : '/',
     * g = (k > 4) ? c[4] : null,
     * i = (k > 5) ? c[5] : false;
     * document.cookie = f + '=' + escape(h) + ((e === null) ? '' : ('; expires=' + e.toGMTString())) + ((j === null) ? '' : ('; path=' + j)) + ((g === null) ? '' : ('; domain=' + g)) + ((i === true) ? '; secure' : '')
     * }
     * getCookie: function (g) {
     * var e = g + '=',
     * k = e.length,
     * c = document.cookie.length,
     * h = 0,
     * f = 0;
     * while (h < c) {
     * f = h + k;
     * if (document.cookie.substring(h, f) == e) {
     * return this.getCookieVal(f)
     * }
     * h = document.cookie.indexOf(' ', h) + 1;
     * if (h === 0) {
     * break
     * }
     * }
     * return null
     * }
     * getCookieVal: function (e) {
     * var c = document.cookie.indexOf(';', e);
     * if (c == - 1) {
     * c = document.cookie.length
     * }
     * return unescape(document.cookie.substring(e, c))
     * }
     * }
     */
    private static boolean sendVideoLog(TaskInfo taskInfo, PlayerTaskData attachment, VideoInfo videoInfo, int playSecond, int dragStatus) throws CheckCodeException {
        if (taskInfo.getAttachments().length == 0)
            return false;
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
        String clipTime = videoInfo.getStartTime() + "_" + (videoInfo.getEndTime() != 0 ? videoInfo.getEndTime() : videoInfo.getDuration());
        md5.update(("[" + taskInfo.getDefaults().getClazzId() + "]" + "[" + taskInfo.getDefaults().getUserid() + "]" + "[" + attachment.getJobid() + "]" + "[" + videoInfo.getObjectid() + "]" + "[" + playSecond * 1000 + "]" + "[d_yHJ!$pdA~5]" + "[" + videoInfo.getDuration() * 1000 + "]" + "[" + clipTime + "]").getBytes());
        StringBuilder md5Str = new StringBuilder(new BigInteger(1, md5.digest()).toString(16));
        while (md5Str.length() < 32)
            md5Str.insert(0, "0");
        String url = taskInfo.getDefaults().getReportUrl();
        if (Optional.ofNullable(videoInfo.getDtoken()).isPresent())
            url += "/" + videoInfo.getDtoken();
        NetUtil.addCookie(new Cookie(NetUtil.getHost(taskInfo.getDefaults().getReportUrl()), "/", "videojs_id", String.valueOf(attachment.getVideoJSId()), -1, false, true));
        return NetUtil.get(ApiURL.VIDEO_PROCESS.buildURL(url,
                taskInfo.getDefaults().getClazzId(),
                videoInfo.getObjectid(),
                taskInfo.getDefaults().getUserid(),
                attachment.getJobid(),
                attachment.getOtherInfo(),
                playSecond,
                dragStatus,
                videoInfo.getDuration(),
                clipTime,
                attachment.getType(),
                (videoInfo.getRt() != 0.0f ? videoInfo.getRt() : 0.9f),
                md5Str,
                System.currentTimeMillis()), 0).toJsonResponse(JSONObject.class).getBody().getBoolean("isPassed");
    }
}
package pers.cz.chaoxing.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.Requests;
import net.dongliu.requests.Session;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import pers.cz.chaoxing.common.PlayerInfo;
import pers.cz.chaoxing.common.QuestionInfo;
import pers.cz.chaoxing.common.PlayerData;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.exception.CheckCodeException;

import java.awt.*;
import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.List;

public class ChaoxingUtil {

    private static Session session = Requests.session();

    public static boolean login(String username, String password, String checkCode) {
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
        RawResponse response = session.post("http://passport2.chaoxing.com/login?refer=http://i.mooc.chaoxing.com/space/index.shtml").body(postBody).send();
        return !response.readToText().contains("用户登录");
    }

    public static String getClassesUri(String baseUri) throws CheckCodeException {
        RawResponse response = session.get("http://i.mooc.chaoxing.com/space/index.shtml").send();
        Document document = Jsoup.parse(response.readToText());
        String src = document.select("div.mainright iframe").attr("src");
        if (session.get(src).send().readToText().contains("操作出现异常"))
            throw new CheckCodeException(baseUri, src, session);
        return src;
    }

    public static List<String> getClasses(String uri) {
        RawResponse response = session.get(uri).send();
        Document document = Jsoup.parse(response.readToText());
        return document.select("div.httpsClass.Mconright a").eachAttr("href");
    }

    public static List<String> getVideos(String uri) {
        Document document = Jsoup.parse(session.get(uri).send().readToText());
        return document.select("span.articlename a").eachAttr("href");
    }

    public static String getCardUriModel(String baseUri, String uri, Map<String, String> params) throws CheckCodeException {
        String cardUri = session.get(baseUri + uri).params(params).send().readToText();
        if (cardUri.contains("操作出现异常"))
            throw new CheckCodeException(baseUri, baseUri + uri, params, session);
        String beginStr = "document.getElementById(\"iframe\").src=\"";
        String endStr = "\";";
        int beginIndex = cardUri.indexOf(beginStr, cardUri.indexOf("getElementById(\"mainid\")")) + beginStr.length();
        return cardUri.substring(beginIndex, cardUri.indexOf(endStr, beginIndex)).replaceAll("[\"+|+\"]", "");
    }

    public static PlayerInfo getPlayerInfo(String baseUri, String cardUri, Map<String, String> params) throws CheckCodeException {
        for (Map.Entry<String, String> param : params.entrySet())
            cardUri = cardUri.replaceAll("(?i)=" + param.getKey(), "=" + param.getValue());
        String responseStr = session.get(baseUri + cardUri).send().readToText();
        if (responseStr.contains("操作出现异常"))
            throw new CheckCodeException(baseUri, baseUri + cardUri, session);
        String beginStr = "mArg = ";
        String endStr = ";";
        int beginIndex = responseStr.indexOf(beginStr, responseStr.indexOf("try{")) + beginStr.length();
        try {
            return JSON.parseObject(responseStr.substring(beginIndex, responseStr.indexOf(endStr, beginIndex)), PlayerInfo.class);
        } catch (JSONException ignored) {
            /*
            none video
             */
            PlayerInfo playerInfo = new PlayerInfo();
            PlayerData playerData = new PlayerData();
            playerData.setPassed(true);
            playerInfo.setAttachments(new PlayerData[]{playerData});
            return playerInfo;
        }
    }

    public static VideoInfo getVideoInfo(String baseUri, String uri, String objectId, String fid) {
        Map<String, String> params = new HashMap<>();
        params.put("k", fid);
        params.put("_dc", String.valueOf(System.currentTimeMillis()));
        return session.get(baseUri + uri + "/" + objectId).params(params).send().readToJson(VideoInfo.class);
    }

    /**
     * call each intervalTime since video playing
     *
     * @param playerInfo
     * @param videoInfo
     * @param playSecond
     * @return
     */
    public static boolean onPlayProgress(PlayerInfo playerInfo, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        return sendLog(playerInfo, videoInfo, playSecond, 0);
    }

    /**
     * call since video loaded first
     *
     * @param playerInfo
     * @param videoInfo
     * @return
     */
    public static boolean onStart(PlayerInfo playerInfo, VideoInfo videoInfo) throws CheckCodeException {
        return sendLog(playerInfo, videoInfo, (int) (playerInfo.getAttachments()[0].getHeadOffset() / 1000), 3);
    }

    /**
     * call since video finished
     *
     * @param playerInfo
     * @param videoInfo
     * @return
     */
    public static boolean onEnd(PlayerInfo playerInfo, VideoInfo videoInfo) throws CheckCodeException {
        return sendLog(playerInfo, videoInfo, videoInfo.getDuration(), 4);
    }

    /**
     * call since video clicked to play
     *
     * @param playerInfo
     * @param videoInfo
     * @param playSecond
     * @return
     */
    public static boolean onPlay(PlayerInfo playerInfo, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        return sendLog(playerInfo, videoInfo, playSecond, 3);
    }

    /**
     * call since video clicked to pause
     *
     * @param playerInfo
     * @param videoInfo
     * @param playSecond
     * @return
     */
    public static boolean onPause(PlayerInfo playerInfo, VideoInfo videoInfo, int playSecond) throws CheckCodeException {
        if (playerInfo.getDefaults().getChapterId() != null && !playerInfo.getDefaults().getChapterId().isEmpty())
            return sendLog(playerInfo, videoInfo, playSecond, 2);
        return false;
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
    private static boolean sendLog(PlayerInfo playerInfo, VideoInfo videoInfo, int playSecond, int dragStatus) throws CheckCodeException {
        /*
        don't send when review mode
        */
//        if (playerInfo.getDefaults().isFiled() || playerInfo.getDefaults().getState() == 1)
//            return false;
        Map<String, String> params = new HashMap<>();
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ignored) {
            return false;
        }
        String chapterId = playerInfo.getDefaults().getChapterId();
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
            md5.update(("[" + playerInfo.getDefaults().getChapterId() + "]" + "[" + playerInfo.getDefaults().getClazzId() + "]" + "[" + videoInfo.getDuration() + "]" + "[0]" + "[" + videoInfo.getObjectid() + "]" + "[" + state + "]" + "[535e933c498001]").getBytes());
            StringBuilder md5Str = new StringBuilder(new BigInteger(1, md5.digest()).toString(16));
            while (md5Str.length() < 32)
                md5Str.insert(0, "0");
            params.put("u", playerInfo.getDefaults().getUserid());
            params.put("s", playerInfo.getDefaults().getClazzId());
            params.put("c", playerInfo.getDefaults().getChapterId());
            params.put("o", videoInfo.getObjectid());
            params.put("st", String.valueOf(state));
            params.put("m", String.valueOf(0));
            params.put("d", String.valueOf(videoInfo.getDuration()));
            params.put("enc", md5Str.toString());
            session.get("http://data.xxt.aichaoxing.com/analysis/datalog").params(params).send();
            params.clear();
        }
        if (playerInfo.getAttachments().length == 0)
            return false;
        String clipTime = videoInfo.getStartTime() + "_" + (videoInfo.getEndTime() != 0 ? videoInfo.getEndTime() : videoInfo.getDuration());
        params.put("clazzId", playerInfo.getDefaults().getClazzId());
        params.put("objectId", videoInfo.getObjectid());
        params.put("userid", playerInfo.getDefaults().getUserid());
        params.put("jobid", playerInfo.getAttachments()[0].getJobid());
        params.put("otherInfo", playerInfo.getAttachments()[0].getOtherInfo());
        params.put("playingTime", String.valueOf(playSecond));
        params.put("isdrag", String.valueOf(dragStatus));
        params.put("duration", String.valueOf(videoInfo.getDuration()));
        params.put("clipTime", clipTime);
        params.put("dtype", playerInfo.getAttachments()[0].getType());
        params.put("rt", String.valueOf(videoInfo.getRt() != 0.0f ? videoInfo.getRt() : 0.9f));
        params.put("view", "pc");
        md5.update(("[" + playerInfo.getDefaults().getClazzId() + "]" + "[" + playerInfo.getDefaults().getUserid() + "]" + "[" + playerInfo.getAttachments()[0].getJobid() + "]" + "[" + videoInfo.getObjectid() + "]" + "[" + playSecond * 1000 + "]" + "[d_yHJ!$pdA~5]" + "[" + videoInfo.getDuration() * 1000 + "]" + "[" + clipTime + "]").getBytes());
        StringBuilder md5Str = new StringBuilder(new BigInteger(1, md5.digest()).toString(16));
        while (md5Str.length() < 32)
            md5Str.insert(0, "0");
        params.put("enc", md5Str.toString());
        String responseStr;
        if (videoInfo.getDtoken() != null && !videoInfo.getDtoken().isEmpty())
            responseStr = session.get(playerInfo.getDefaults().getReportUrl() + "/" + videoInfo.getDtoken()).params(params).send().readToText();
        else
            responseStr = session.get(playerInfo.getDefaults().getReportUrl()).params(params).send().readToText();
        if (session.get(responseStr).send().readToText().contains("操作出现异常"))
            throw new CheckCodeException("https://mooc1-1.chaoxing.com", playerInfo.getDefaults().getReportUrl(), session);
        return JSONObject.parseObject(responseStr).getBoolean("isPassed");
    }

    /**
     * call since video start playing
     *
     * @param initDataUrl
     * @param mid
     * @return
     */
    public static List<QuestionInfo> getQuestionInfos(String initDataUrl, String mid) {
        HashMap<String, String> params = new HashMap<>();
        params.put("mid", mid);
        params.put("start", "undefined");
        return JSONArray.parseArray(session.get(initDataUrl).params(params).send().readToText(), QuestionInfo.class);
    }

    public static boolean answerQuestion(String baseUri, String validationUrl, String resourceid, String answer) {
        HashMap<String, String> params = new HashMap<>();
        params.put("resourceid", resourceid);
        params.put("answer", "'" + answer + "'");
        JSONObject jsonObject = JSONObject.parseObject(session.get(baseUri + validationUrl).params(params).send().readToText());
        return jsonObject.getString("answer").equals(answer) && jsonObject.getBoolean("isRight");
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

    public static void saveCheckCode(String path) {
        session.get("http://passport2.chaoxing.com/num/code?" + System.currentTimeMillis()).send().writeToFile(path);
    }

    public static boolean openFile(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return false;
    }

}

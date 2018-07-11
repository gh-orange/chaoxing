package pers.cz.chaoxing.util;

import net.dongliu.requests.Requests;
import net.dongliu.requests.URIEncoder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import pers.cz.chaoxing.common.quiz.HomeworkQuizInfo;
import pers.cz.chaoxing.common.quiz.OptionInfo;
import pers.cz.chaoxing.common.quiz.QuizConfig;

import java.io.UnsupportedEncodingException;
import java.net.Proxy;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AnswerUtil {
    private static Proxy proxy = CXUtil.proxy;

    public static void getAnswer(QuizConfig[] quizConfigs) throws UnsupportedEncodingException {
        for (QuizConfig quizConfig : quizConfigs) {
            String description = quizConfig.getDescription().replaceAll("【.*?】", "").replaceAll("[\\pP\\pS\\pZ]", "");
//            description = description.substring(0, description.length() / 2 + 1);
            Document document = Jsoup.parse(Requests.get("https://m.3gmfw.cn/so/" + URLEncoder.encode(description, "UTF-8") + "/").proxy(proxy).send().charset("GBK").readToText());
            Element ul = document.selectFirst("ul.article-list");
            if (ul == null) {
                for (OptionInfo optionInfo : quizConfig.getOptions()) {
                    optionInfo.setRight(true);
                    if (quizConfig.getQuestionType().equals("0"))
                        break;
                }
                continue;
            }
            Element li = ul.selectFirst("li");
            document = Jsoup.parse(Requests.get("https://m.3gmfw.cn/" + li.selectFirst("a").attr("href")).proxy(proxy).send().charset("GBK").readToText());
            Elements p = document.select("div.content p");
            Map<String, String> answers = new HashMap<>();
            String rightAnswers = "";
            rightAnswers = p.last().text().trim();
            for (Element element : p)
                for (TextNode textNode : element.textNodes())
                    if (!textNode.isBlank())
                        if (!textNode.text().trim().contains("答案：")) {
                            Matcher matcher = Pattern.compile("[a-zA-Z]").matcher(textNode.text());
                            if (matcher.find())
                                answers.put(matcher.group(), textNode.text().trim());
                        } else
                            rightAnswers = textNode.text().trim();
            if (rightAnswers.contains("答案："))
                rightAnswers = rightAnswers.substring(rightAnswers.indexOf("答案：") + "答案：".length());
            for (char c : rightAnswers.toCharArray())
                for (OptionInfo optionInfo : quizConfig.getOptions())
                    if (answers.get(Character.toString(c)).contains(optionInfo.getDescription())) {
                        optionInfo.setRight(true);
                        if (quizConfig.getQuestionType().equals("0"))
                            break;
                    }
        }
    }
}

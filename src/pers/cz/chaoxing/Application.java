package pers.cz.chaoxing;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.CallBack;
import pers.cz.chaoxing.callback.CheckCodeSingletonFactory;
import pers.cz.chaoxing.common.school.SchoolData;
import pers.cz.chaoxing.common.school.SchoolInfo;
import pers.cz.chaoxing.thread.manager.ExamManager;
import pers.cz.chaoxing.thread.manager.HomeworkManager;
import pers.cz.chaoxing.thread.manager.ManagerModel;
import pers.cz.chaoxing.thread.manager.PlayerManager;
import pers.cz.chaoxing.util.*;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

/**
 * ChaoxingPlugin - A hands-free tool for watching video and doing homework&exam faster
 * Copyright (C) 2018  orange
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms once the GNU General Public License as published by
 * the Free Software Foundation, either version 3 once the License, or
 * any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty once
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy once the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @author 橙子
 * @version 1.3.2
 */
public class Application {
    private static String baseUri = "https://mooc1-1.chaoxing.com";
    private static String classesUri = "";
    private static String cardUriModel = "";
    private static String examUriModel = "";

    public static void main(String[] args) {
        Application.copyright();
        try (IOUtil.ScanJob scanJob = new IOUtil.ScanJob()) {
            CheckCodeSingletonFactory.setProxy(CXUtil.proxy);
            new Thread(scanJob).start();
            Application.login();
            IOUtil.print("Using fast mode (may got WARNING, suggest you DO NOT USE) [y/n]:");
            boolean hasSleep = !IOUtil.next().equalsIgnoreCase("y");
            IOUtil.print("Doing review mode homework&exam (may got WARNING, suggest you DO NOT USE) [y/n]:");
            boolean skipReview = !IOUtil.next().equalsIgnoreCase("y");
            IOUtil.print("Storing only matching answers in homework&exam (completing all answers if choose no) [y/n]:");
            CompleteStyle completeStyle = !IOUtil.next().equalsIgnoreCase("y") ? CompleteStyle.NONE : CompleteStyle.MANUAL;
            if (!CompleteStyle.NONE.equals(completeStyle)) {
                IOUtil.print("Auto-completing all answers (may got lower mark, manual-completing if not) [y/n]:");
                if (IOUtil.next().equalsIgnoreCase("y"))
                    completeStyle = CompleteStyle.AUTO;
            }
            Semaphore semaphore = hasSleep ? new Semaphore(4) : null;
            IOUtil.print("Input size of playerThreadPool(suggest max size is 3):");
            int playerThreadPoolSize = IOUtil.nextInt();
            IOUtil.print("Input size of homeworkThreadPool(suggest max size is 1):");
            int homeworkThreadPoolSize = IOUtil.nextInt();
            IOUtil.print("Input size of examThreadPool(suggest max size is 1):");
            int examThreadPoolSize = IOUtil.nextInt();
//            IOUtil.println("Press 'p' to pause, press 's' to stop, press any key to continue");
            try (
                    PlayerManager playerManager = new PlayerManager(playerThreadPoolSize);
                    HomeworkManager homeworkManager = new HomeworkManager(homeworkThreadPoolSize);
                    ExamManager examManager = new ExamManager(examThreadPoolSize)
            ) {
                while (classesUri.isEmpty())
                    classesUri = Try.ever(CXUtil::getClassesUri, CheckCodeSingletonFactory.CUSTOM.get());
                List<Map<String, String>> taskParamsList = new ArrayList<>();
                List<Map<String, String>> examParamsList = new ArrayList<>();
                String finalClassesUri = classesUri;
                Try.ever(() -> CXUtil.getClasses(finalClassesUri), CheckCodeSingletonFactory.CUSTOM.get()).forEach(classUri -> {
                    taskParamsList.addAll(Application.getTaskParams(classUri, CheckCodeSingletonFactory.CUSTOM.get()));
                    examParamsList.addAll(Application.getExamParams(classUri));
                });
                ManagerModel[] managers = {playerManager, homeworkManager, examManager};
                Application.initManagers(
                        managers,
                        BASE_URI, cardUriModel, examUriModel,
                        taskParamsList, examParamsList,
                        hasSleep, skipReview, completeStyle,
                        semaphore
                );
                Application.startManagers(managers);
            }
        } catch (RequestsException e) {
            String message = StringUtil.subStringAfterFirst(e.getLocalizedMessage(), "Exception:").trim();
            IOUtil.println("Net connection error: " + message);
        } catch (Exception ignored) {
        }
    }

    private static void copyright() {
        IOUtil.println("ChaoxingPlugin v1.3.2 - powered by orange");
        IOUtil.println("License - GPLv3: This is a free & share software");
        IOUtil.println("You can checking source code from: https://github.com/cz111000/chaoxing");
    }

    private static void login() {
        SchoolData schoolData = Application.getSchoolData();
        Application.notice(schoolData.getId());
        IOUtil.print("Input account:");
        String username = IOUtil.nextLine();
        IOUtil.print("Input password:");
        String password = IOUtil.nextLine();
        Try.ever(() -> CXUtil.login(schoolData.getId(), username, password),
                CheckCodeSingletonFactory.LOGIN.get(),
                String.valueOf(schoolData.getId()), username, password);
    }

    private static void notice(int fid) {
        switch (fid) {
            case 1581:
                IOUtil.println("请内蒙古赤峰学院的同学用 \"A+学号\"(如Axxx) 作为账号登录，谢谢合作！");
                break;
            case 1630:
                IOUtil.println("请南阳师范学院的同学用 \"z+学号\"(如zxxx) 作为账号登录，谢谢合作！");
                break;
            case 1639:
                IOUtil.println("此学院坞城校区学生登陆时学号前面需要加数字0 其它校区不需要加数字0");
                break;
            case 1661:
                IOUtil.println("请宁波工程学院的同学用 \" s + 学号\"(如sxxx) 作为账号登录，谢谢合作！");
                break;
            case 2025:
            case 2073:
            case 1588:
            case 878:
            case 11766:
                IOUtil.println("学号为11位的用户，登录时请在学号前加 s");
                break;
            case 1694:
            case 2125:
            case 183:
            case 1748:
            case 1267:
            case 10749:
                IOUtil.println("通知：由于本校学号为11位纯数字，与手机号码冲突，麻烦各位同学在登录时学号前面加小写字母“s”，辛苦各位同学。同学们在任何时间段都可以在线学习。");
                break;
            case 1254:
                IOUtil.println("请河北医科大学的用户，登录时请在学号前加 s");
                break;
            case 1192:
                IOUtil.println("请广东科贸职业学院的用户，登录时请在学号前加 s");
                break;
            case 8523:
                IOUtil.println("请伊犁师范学院的用户，登录时请在学号前加 s");
                break;
            case 1491:
                IOUtil.println("请九江学院的用户，登录时请在学号前加 s");
                break;
            case 13472:
                IOUtil.println("学号是11位数字，并且是1开头，登陆时需要加小写s ，学号不是11位数字的不需要加");
                break;
            case 13491:
                IOUtil.println("学号是11位数字，并且是1开头，登陆时需要加小写s ，学号不是11位数字的不需要加");
                break;
            case 13496:
                IOUtil.println("学号是11位数字，并且是1开头，登陆时需要加小写s ，学号不是11位数字的不需要加");
                break;
            case 13659:
                IOUtil.println("请黔东南民族职业技术学院夏令营的用户，登录时请在学号前加 s");
                break;
            case 13673:
                IOUtil.println("学号11位数字，并且是1开头，登陆时需要学号前面加小写s");
                break;
            case 13133:
                IOUtil.println("学号11位数字，并且是1开头，登陆时需要学号前面加小写s");
                break;
            case 13148:
                IOUtil.println("学号11位数字，并且是1开头，登陆时需要学号前面加小写s");
                break;
            case 13895:
                IOUtil.println("学号11位数字，并且是1开头，登陆时需要学号前面加小写s");
                break;
            case 14046:
                IOUtil.println("学号11位数字，并且是1开头，登陆时需要学号前面加小写s");
                break;
            case 14345:
                IOUtil.println("学号11位数字，并且是1开头，登陆时需要学号前面加小写s");
                break;
            case 1606:
                IOUtil.println("请廊坊师范学院的用户，登录时请在学号前加 s");
                break;
            case 14286:
                IOUtil.println("学号11位数字，并且是1开头，登陆时需要学号前面加小写s");
                break;
            case 13113:
                IOUtil.println("学号11位数字，并且是1开头，登陆时需要学号前面加小写s");
                break;
        }
    }

    private static SchoolData getSchoolData() {
        SchoolData schoolData = new SchoolData();
        schoolData.setId(1322);
        while (true) {
            IOUtil.print("Input school name or fid (default is 1322):");
            schoolData.setName(IOUtil.nextLine().trim());
            if (schoolData.getName().matches(".*\\D.*")) {
                SchoolInfo schoolInfo = CXUtil.searchSchool(schoolData.getName());
                if (schoolInfo.isResult()) {
                    IntStream.range(0, schoolInfo.getFromNums()).forEach(i -> IOUtil.println((i + 1) + ":" + schoolInfo.getFroms()[i].getName()));
                    IOUtil.print("Choose school index:");
                    int index = IOUtil.nextInt();
                    if (index > 0 && index <= schoolInfo.getFromNums())
                        schoolData.setId(schoolInfo.getFroms()[index - 1].getId());
                    else continue;
                } else continue;
            } else if (!schoolData.getName().isEmpty())
                schoolData.setId(Integer.parseInt(schoolData.getName()));
            break;
        }
        return schoolData;
    }

    private static List<Map<String, String>> getTaskParams(String classUri, CallBack checkCodeCallBack) {
        List<Map<String, String>> taskParamsList = new ArrayList<>();
        for (String taskUri : CXUtil.getTasks(baseUri, classUri)) {
            //parse uri to params
            String[] taskUris = taskUri.split("\\?", 2);
            Map<String, String> taskParams = new HashMap<>();
            Arrays.stream(taskUris[1].split("&"))
                    .map(param -> param.split("="))
                    .forEach(strings -> taskParams.put(strings[0], strings[1]));
            taskParamsList.add(taskParams);
            if (cardUriModel.isEmpty())
                cardUriModel = Try.ever(() -> CXUtil.getCardUriModel(Application.BASE_URI, taskUris[0], taskParams), checkCodeCallBack);
        }
        return taskParamsList;
    }

    private static List<Map<String, String>> getExamParams(String baseUri, String classUri) {
        List<Map<String, String>> examParamsList = new ArrayList<>();
        for (String examUri : CXUtil.getExams(baseUri, classUri)) {
            //parse uri to params
            String[] examUris = examUri.split("\\?", 2);
            Map<String, String> examParams = new HashMap<>();
            Arrays.stream(examUris[1].split("&"))
                    .map(param -> param.split("="))
                    .forEach(strings -> examParams.put(strings[0], strings[1]));
            examParamsList.add(examParams);
            if (examUriModel.isEmpty())
                examUriModel = examUris[0];
        }
        return examParamsList;
    }

    private static void startManagers(ManagerModel[] managers) throws InterruptedException {
        List<Thread> commonThreads = new ArrayList<>();
        Thread examThread = null;
        for (ManagerModel manager : managers)
            if (manager instanceof ExamManager)
                examThread = new Thread(manager);
            else
                commonThreads.add(new Thread(manager));
        commonThreads.forEach(Thread::start);
        commonThreads.forEach(Try.once(Thread::join));
        if (examThread != null) {
            examThread.start();
            examThread.join();
        }
    }

    private static void initManagers(
            ManagerModel[] managers,
            String baseUri, String cardUriModel, String examUriModel,
            List<Map<String, String>> taskParamsList, List<Map<String, String>> examParamsList,
            boolean hasSleep, boolean skipReview, CompleteStyle completeStyle,
            Semaphore semaphore
    ) {
        for (ManagerModel manager : managers) {
            if (manager instanceof ExamManager) {
                manager.setUriModel(examUriModel);
                manager.setParamsList(examParamsList);
            } else {
                manager.setUriModel(cardUriModel);
                manager.setParamsList(taskParamsList);
            }
            manager.setBaseUri(baseUri);
            manager.setHasSleep(hasSleep);
            manager.setSkipReview(skipReview);
            manager.setSemaphore(semaphore);
            manager.setCompleteStyle(completeStyle);
        }
    }
}
package pers.cz.chaoxing;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.impl.CustomCheckCodeCallBack;
import pers.cz.chaoxing.common.other.SchoolData;
import pers.cz.chaoxing.common.other.SchoolInfo;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.thread.manager.ExamManager;
import pers.cz.chaoxing.thread.manager.HomeworkManager;
import pers.cz.chaoxing.thread.manager.Manager;
import pers.cz.chaoxing.thread.manager.PlayerManager;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.IOLock;
import pers.cz.chaoxing.util.StringUtil;
import pers.cz.chaoxing.util.Try;

import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.regex.Pattern;
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
 * @version 1.3.1
 */
public class Application {
    private static String baseUri = "https://mooc1-1.chaoxing.com";
    private static String classesUri = "";
    private static String cardUriModel = "";
    private static String examUriModel = "";

    public static void main(String[] args) {
        Application.copyright();
        try (Scanner scanner = new Scanner(System.in)) {
            CustomCheckCodeCallBack customCallBack = new CustomCheckCodeCallBack("./checkCode-custom.jpeg");
            customCallBack.setScanner(scanner);
            Application.login(scanner, customCallBack);
            System.out.print("Using fast mode (may got WARNING, suggest you DO NOT USE) [y/n]:");
            boolean hasSleep = !scanner.next().equalsIgnoreCase("y");
            System.out.print("Doing review mode homework&exam (may got WARNING, suggest you DO NOT USE) [y/n]:");
            boolean skipReview = !scanner.next().equalsIgnoreCase("y");
            System.out.print("Checking all answers to auto-complete homework&exam (may got lower mark, store answers if not) [y/n]:");
            boolean autoComplete = scanner.next().equalsIgnoreCase("y");
            Semaphore semaphore = hasSleep ? new Semaphore(4) : null;
            System.out.print("Input size of playerThreadPool(suggest max size is 3):");
            int playerThreadPoolSize = scanner.nextInt();
            System.out.print("Input size of homeworkThreadPool(suggest max size is 1):");
            int homeworkThreadPoolSize = scanner.nextInt();
            System.out.print("Input size of examThreadPool(suggest max size is 1):");
            int examThreadPoolSize = scanner.nextInt();
//            System.out.println("Press 'p' to pause, press 's' to stop, press any key to continue");
            try (
                    PlayerManager playerManager = new PlayerManager(playerThreadPoolSize);
                    HomeworkManager homeworkManager = new HomeworkManager(homeworkThreadPoolSize);
                    ExamManager examManager = new ExamManager(examThreadPoolSize)
            ) {
                while (classesUri.isEmpty())
                    classesUri = Try.ever(CXUtil::getClassesUri, customCallBack);
                List<Map<String, String>> taskParamsList = new ArrayList<>();
                List<Map<String, String>> examParamsList = new ArrayList<>();
                String finalClassesUri = classesUri;
                Try.ever(() -> CXUtil.getClasses(finalClassesUri), customCallBack).forEach(classUri -> {
                    taskParamsList.addAll(Application.getTaskParams(baseUri, classUri, customCallBack));
                    examParamsList.addAll(Application.getExamParams(baseUri, classUri));
                });
                Manager[] managers = {playerManager, homeworkManager, examManager};
                Application.initManagers(managers, baseUri, cardUriModel, examUriModel, taskParamsList, examParamsList, hasSleep, skipReview, autoComplete, semaphore, customCallBack);
                Application.startManagers(managers);
            }
        } catch (RequestsException e) {
            String message = StringUtil.subStringAfterFirst(e.getLocalizedMessage(), "Exception:").trim();
            IOLock.output(() -> System.out.println("Net connection error: " + message));
        } catch (Exception ignored) {
        }
    }

    private static void copyright() {
        System.out.println("ChaoxingPlugin v1.3.1 - powered by orange");
        System.out.println("License - GPLv3: This is a free & share software");
        System.out.println("You can checking source code from: https://github.com/cz111000/chaoxing");
    }

    private static void login(Scanner scanner, CustomCheckCodeCallBack customCallBack) {
        String username;
        String password;
        String checkCode;
        SchoolData schoolData = Application.getSchoolData(scanner);
        while (true)
            try {
                System.out.print("Input account:");
                username = scanner.nextLine();
                System.out.print("Input password:");
                password = scanner.nextLine();
                do {
                    CXUtil.saveCheckCode(customCallBack.getCheckCodePath());
                    if (!customCallBack.openFile(customCallBack.getCheckCodePath()))
                        System.out.println("CheckCode image path:" + customCallBack.getCheckCodePath());
                    System.out.print("Input checkCode:");
                    checkCode = scanner.nextLine().replaceAll("\\s", "");
                } while (!CXUtil.login(schoolData.getId(), username, password, checkCode));
                break;
            } catch (WrongAccountException e) {
                System.out.println(e.getLocalizedMessage());
            }
    }

    private static SchoolData getSchoolData(Scanner scanner) {
        SchoolData schoolData = new SchoolData();
        schoolData.setId(1322);
        while (true) {
            System.out.print("Input school name or fid (default is 1322):");
            schoolData.setName(scanner.nextLine().trim());
            if (schoolData.getName().matches(".*\\D.*")) {
                SchoolInfo schoolInfo = CXUtil.searchSchool(schoolData.getName());
                if (schoolInfo.isResult()) {
                    IntStream.range(0, schoolInfo.getFromNums()).forEach(i -> System.out.println((i + 1) + ":" + schoolInfo.getFroms()[i].getName()));
                    System.out.print("Choose school index:");
                    int index = scanner.nextInt();
                    scanner.nextLine();
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

    private static List<Map<String, String>> getTaskParams(String baseUri, String classUri, CustomCheckCodeCallBack customCallBack) {
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
                cardUriModel = Try.ever(() -> CXUtil.getCardUriModel(baseUri, taskUris[0], taskParams), customCallBack);
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

    private static void startManagers(Manager[] managers) throws InterruptedException {
        List<Thread> commonThreads = new ArrayList<>();
        Thread examThread = null;
        for (Manager manager : managers)
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

    private static void initManagers(Manager[] managers, String baseUri, String cardUriModel, String examUriModel, List<Map<String, String>> taskParamsList, List<Map<String, String>> examParamsList, boolean hasSleep, boolean skipReview, boolean autoComplete, Semaphore semaphore, CustomCheckCodeCallBack customCallBack) {
        for (Manager manager : managers) {
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
            manager.setAutoComplete(autoComplete);
            manager.setCustomCallBack(customCallBack);
        }
    }
}
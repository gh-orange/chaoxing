package pers.cz.chaoxing;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.impl.CustomCheckCodeCallBack;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.thread.manager.ExamManager;
import pers.cz.chaoxing.thread.manager.HomeworkManager;
import pers.cz.chaoxing.thread.manager.PlayerManager;
import pers.cz.chaoxing.thread.task.PlayTask;
import pers.cz.chaoxing.util.CXUtil;

import java.util.*;
import java.util.concurrent.Semaphore;

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
 * @version 1.2.2
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("ChaoxingPlugin v1.2.2 - powered by orange");
        System.out.println("License - GPLv3: This is a free & share software");
        System.out.println("You can checking source code from: https://github.com/cz111000/chaoxing");
        try {
            CustomCheckCodeCallBack customCallBack = new CustomCheckCodeCallBack("./checkCode-custom.jpeg");
            Scanner scanner = new Scanner(System.in);
            String username;
            String password;
            String checkCode;
            while (true)
                try {
                    System.out.print("Input account:");
                    username = scanner.nextLine();
                    System.out.print("Input password:");
                    password = scanner.nextLine();
                    do {
                        CXUtil.saveCheckCode(customCallBack.getCheckCodePath());
                        if (customCallBack.openFile(customCallBack.getCheckCodePath()))
                            System.out.println("CheckCode image path:" + customCallBack.getCheckCodePath());
                        System.out.print("Input checkCode:");
                        checkCode = scanner.nextLine();
                    } while (!CXUtil.login(username, password, checkCode));
                    break;
                } catch (WrongAccountException ignored) {
                    System.out.println("Wrong account or password");
                }
            final String baseUri = "https://mooc1-1.chaoxing.com";
            System.out.print("Using fast mode (may got WARNING, suggest you DO NOT USE) [y/n]:");
            boolean hasSleep = !scanner.next().equalsIgnoreCase("y");
            System.out.print("Checking all answers to auto-complete homework&exam (may got lower mark, store answers if not) [y/n]:");
            boolean autoComplete = scanner.next().equalsIgnoreCase("y");
            Semaphore semaphore = hasSleep ? new Semaphore(4) : null;
            System.out.print("Input size of playerThreadPool(suggest max size is 3):");
            PlayerManager playerManager = new PlayerManager(scanner.nextInt());
            playerManager.setBaseUri(baseUri);
            playerManager.setHasSleep(hasSleep);
            playerManager.setSemaphore(semaphore);
            System.out.print("Input size of homeworkThreadPool(suggest max size is 1):");
            HomeworkManager homeworkManager = new HomeworkManager(scanner.nextInt());
            homeworkManager.setBaseUri(baseUri);
            homeworkManager.setHasSleep(hasSleep);
            homeworkManager.setSemaphore(semaphore);
            homeworkManager.setAutoComplete(autoComplete);
            System.out.print("Input size of examThreadPool(suggest max size is 1):");
            ExamManager examManager = new ExamManager(scanner.nextInt());
            examManager.setBaseUri(baseUri);
            examManager.setHasSleep(hasSleep);
            examManager.setSemaphore(semaphore);
            examManager.setAutoComplete(autoComplete);
//            System.out.println("Press 'p' to pause, press 's' to stop, press any key to continue");
            String classesUri = "";
            while (classesUri.isEmpty())
                try {
                    classesUri = CXUtil.getClassesUri();
                } catch (CheckCodeException e) {
                    customCallBack.call(e.getSession(), e.getUri());
                }
            String cardUriModel = "";
            String examUriModel = "";
            List<Map<String, String>> taskParamsList = new ArrayList<>();
            List<Map<String, String>> examParamsList = new ArrayList<>();
            for (String classUri : CXUtil.getClasses(classesUri)) {
                for (String taskUri : CXUtil.getTasks(baseUri, classUri)) {
                    //parse uri to params
                    String[] taskUris = taskUri.split("\\?", 2);
                    Map<String, String> taskParams = new HashMap<>();
                    Arrays.stream(taskUris[1].split("&"))
                            .map(param -> param.split("="))
                            .forEach(strings -> taskParams.put(strings[0], strings[1]));
                    taskParamsList.add(taskParams);
                    while (cardUriModel.isEmpty())
                        try {
                            cardUriModel = CXUtil.getCardUriModel(baseUri, taskUris[0], taskParams);
                        } catch (CheckCodeException e) {
                            customCallBack.call(e.getSession(), e.getUri());
                        }
                }
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
            }
            playerManager.setUriModel(cardUriModel);
            playerManager.setParamsList(taskParamsList);
            playerManager.setCustomCallBack(customCallBack);
            homeworkManager.setUriModel(cardUriModel);
            homeworkManager.setParamsList(taskParamsList);
            homeworkManager.setCustomCallBack(customCallBack);
            examManager.setUriModel(examUriModel);
            examManager.setParamsList(examParamsList);
            examManager.setCustomCallBack(customCallBack);
            Thread playerThread = new Thread(playerManager);
            Thread homeworkThread = new Thread(homeworkManager);
            playerThread.start();
            homeworkThread.start();
            playerThread.join();
            homeworkThread.join();
            playerManager.close();
            homeworkManager.close();
            Thread examThread = new Thread(examManager);
            examThread.start();
            examThread.join();
            examManager.close();
            scanner.close();
        } catch (RequestsException e) {
            System.out.println("Net connection error");
        } catch (Exception ignored) {
        }
    }

    private static void inputCheck(Scanner scanner, List<PlayTask> threadList) {
        if (scanner.hasNextByte()) {
            boolean stop = false;
            boolean pause = false;
            switch (scanner.nextByte()) {
                case 's':
                case 'S':
                    stop = true;
                    break;
                case 'p':
                case 'P':
                    pause = true;
                    break;
            }
            for (PlayTask task : threadList) {
                task.setStop(stop);
                task.setPause(pause);
            }
        }
    }

}
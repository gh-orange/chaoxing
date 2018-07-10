package pers.cz.chaoxing;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.impl.CheckCodeCallBack;
import pers.cz.chaoxing.common.task.HomeworkData;
import pers.cz.chaoxing.common.task.PlayerData;
import pers.cz.chaoxing.common.VideoInfo;
import pers.cz.chaoxing.common.task.TaskInfo;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.thread.LimitedBlockingQueue;
import pers.cz.chaoxing.thread.PlayTask;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.InfoType;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * ChaoxingVideoTool - a tool to view faster
 * Copyright (C) 2018  orange
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * @author 橙子
 * @version 1.0.1
 */
public class Application {

    public static void main(String[] args) {
        System.out.println("ChaoxingVideoTool v1.0.1 - powered by orange");
        System.out.println("License - GPLv3: This is a free & share software");
        System.out.println("You can checking source code from: https://github.com/cz111000/chaoxing");
        try {
            CheckCodeCallBack callBack = new CheckCodeCallBack("./checkCode.jpeg");
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
                        CXUtil.saveCheckCode(callBack.getCheckCodePath());
                        if (callBack.openFile(callBack.getCheckCodePath()))
                            System.out.println("CheckCode image path:" + callBack.getCheckCodePath());
                        System.out.print("Input checkCode:");
                        checkCode = scanner.nextLine();
                    } while (!CXUtil.login(username, password, checkCode));
                    break;
                } catch (WrongAccountException ignored) {
                    System.out.println("Wrong account or password");
                }
            String baseUri = "https://mooc1-1.chaoxing.com";
            String classesUri = null;
            while (classesUri == null || classesUri.isEmpty())
                try {
                    classesUri = CXUtil.getClassesUri();
                } catch (CheckCodeException e) {
                    callBack.call(e.getUri(), e.getSession());
                }
            String cardUriModel = null;
            System.out.print("Input size of threadPool(suggest max size is 4):");
            int threadPoolCount = scanner.nextInt();
            System.out.print("Using fast mode (may got WARNING, suggest you DO NOT USE) [y/n]:");
            boolean hasSleep = !scanner.next().equalsIgnoreCase("y");
            ExecutorService threadPool = new ThreadPoolExecutor(threadPoolCount, threadPoolCount, 0L, TimeUnit.MILLISECONDS, new LimitedBlockingQueue<>(1));
            int threadCount = 0;
            CompletionService<Boolean> completionService = new ExecutorCompletionService<>(threadPool);
//            System.out.println("Press 'p' to pause, press 's' to stop, press any key to continue");
            int clickCount = 0;
            for (String classUri : CXUtil.getClasses(classesUri))
                for (String taskUri : CXUtil.getTasks(baseUri + classUri)) {
                    //parse uri to params
                    String[] taskUris = taskUri.split("\\?", 2);
                    Map<String, String> params = new HashMap<>();
                    for (String param : taskUris[1].split("&")) {
                        String[] strings = param.split("=");
                        params.put(strings[0], strings[1]);
                    }
                    while (true)
                        try {
                            if (cardUriModel == null || cardUriModel.isEmpty())
                                cardUriModel = CXUtil.getCardUriModel(baseUri, taskUris[0], params);
                            TaskInfo<HomeworkData> homeworkInfo = CXUtil.getTaskInfo(baseUri, cardUriModel, params, InfoType.Homework);
                            CXUtil.answerHomeworkQuiz(baseUri, CXUtil.getHomeworkQuiz(baseUri, homeworkInfo));
                            TaskInfo<PlayerData> taskInfo = CXUtil.getTaskInfo(baseUri, cardUriModel, params, InfoType.Video);
                            if (taskInfo.getAttachments().length > 0 && !taskInfo.getAttachments()[0].isPassed())
                                if (CXUtil.startRecord(baseUri, params)) {
                                    VideoInfo videoInfo = CXUtil.getVideoInfo(baseUri, "/ananas/status", taskInfo.getAttachments()[0].getObjectId(), taskInfo.getDefaults().getFid());
                                    String videoName = videoInfo.getFilename();
                                    try {
                                        videoName = URLDecoder.decode(videoName, "utf-8");
                                    } catch (UnsupportedEncodingException ignored) {
                                    }
                                    System.out.println("Video did not pass:" + videoName);
                                    char[] charArray = taskInfo.getAttachments()[0].getType().toCharArray();
                                    charArray[0] -= 32;
                                    taskInfo.getAttachments()[0].setType(String.valueOf(charArray));
                                    PlayTask playTask = new PlayTask(taskInfo, videoInfo, baseUri);
                                    playTask.setCheckCodeCallBack(callBack);
                                    playTask.setHasSleep(hasSleep);
                                    completionService.submit(playTask);
                                    threadCount++;
                                    System.out.println("Added playTask to ThreadPool:" + videoName);
                                }
                            /*
                            imitate human click
                             */
                            if (hasSleep && ++clickCount % 15 == 0)
                                Thread.sleep(30 * 1000);
                            break;
                        } catch (CheckCodeException e) {
                            callBack.call(e.getUri(), e.getSession());
                        }
                }
            try {
                for (int i = 0; i < threadCount; i++)
                    completionService.take().get();
            } catch (Exception ignored) {
            }
            threadPool.shutdown();
            scanner.close();
            System.out.println("Finished playTask count:" + threadCount);
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
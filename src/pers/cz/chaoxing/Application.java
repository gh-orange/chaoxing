package pers.cz.chaoxing;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.common.*;
import pers.cz.chaoxing.exception.CheckCodeException;
import pers.cz.chaoxing.thread.PlayTask;
import pers.cz.chaoxing.util.ChaoxingUtil;

import java.awt.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * ChaoxingVideoTool - a tool for view faster
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
public class Application {

    public static void main(String[] args) {
        try {
            String checkCodePath = "./checkCode.jpeg";
            Scanner scanner = new Scanner(System.in);
            String username;
            String password;
            String checkCode;
            System.out.print("Input username:");
            username = scanner.nextLine();
            System.out.print("Input password:");
            password = scanner.nextLine();
            do {
                ChaoxingUtil.saveCheckCode(checkCodePath);
                if (openFile(checkCodePath))
                    System.out.println("CheckCode image path:" + checkCodePath);
                System.out.print("Input checkCode:");
                checkCode = scanner.nextLine();
            } while (!ChaoxingUtil.login(username, password, checkCode));
            String baseUri = "https://mooc1-1.chaoxing.com";
            String classesUri = null;
            while (classesUri == null || classesUri.isEmpty())
                try {
                    classesUri = ChaoxingUtil.getClassesUri(baseUri);
                } catch (CheckCodeException e) {
                    e.saveCheckCode(checkCodePath);
                    if (openFile(checkCodePath))
                        System.out.println("CheckCode image path:" + checkCodePath);
                    System.out.print("Input checkCode:");
                    checkCode = scanner.nextLine();
                    e.setCheckCode(checkCode);
                }
            String cardUriModel = null;
            System.out.print("Input size of threadPool:");
            int threadCount = scanner.nextInt();
            System.out.print("Using fast mode (may got WARNING, suggest you DO NOT USE) [y/n]:");
            boolean hasSleep = !scanner.next().equalsIgnoreCase("y");
            ExecutorService threadPool = Executors.newFixedThreadPool(threadCount);
            List<PlayTask> threadList = new ArrayList<>();
//            System.out.println("Press 'p' to pause, press 's' to stop, press any key to continue");
            for (String classUri : ChaoxingUtil.getClasses(classesUri))
                for (String videoUri : ChaoxingUtil.getVideos(baseUri + classUri)) {
                    //parse uri to params
                    String[] videoUris = videoUri.split("\\?", 2);
                    Map<String, String> params = new HashMap<>();
                    for (String param : videoUris[1].split("&")) {
                        String[] strings = param.split("=");
                        params.put(strings[0], strings[1]);
                    }
                    while (true)
                        try {
                            if (cardUriModel == null || cardUriModel.isEmpty())
                                cardUriModel = ChaoxingUtil.getCardUriModel(baseUri, videoUris[0], params);
                            PlayerInfo playerInfo = ChaoxingUtil.getPlayerInfo(baseUri, cardUriModel, params);
                            if (playerInfo.getAttachments().length > 0 && !playerInfo.getAttachments()[0].isPassed()) {
                                VideoInfo videoInfo = ChaoxingUtil.getVideoInfo(baseUri, "/ananas/status", playerInfo.getAttachments()[0].getObjectId(), playerInfo.getDefaults().getFid());
                                String videoName = videoInfo.getFilename();
                                try {
                                    videoName = URLDecoder.decode(videoName, "utf-8");
                                } catch (UnsupportedEncodingException ignored) {
                                }
                                System.out.println("Video did not pass:" + videoName);
                                if (ChaoxingUtil.startRecord(baseUri, params)) {
                                    System.out.println("Add playTask to ThreadPool:" + videoName);
                                    char[] charArray = playerInfo.getAttachments()[0].getType().toCharArray();
                                    charArray[0] -= 32;
                                    playerInfo.getAttachments()[0].setType(String.valueOf(charArray));
                                    PlayTask playTask = new PlayTask(playerInfo, videoInfo, baseUri);
                                    playTask.setHasSleep(hasSleep);
                                    threadPool.execute(playTask);
                                    threadList.add(playTask);
                                }
                            }
                            break;
                        } catch (CheckCodeException e) {
                            e.saveCheckCode(checkCodePath);
                            if (openFile(checkCodePath))
                                System.out.println("CheckCode image path:" + checkCodePath);
                            System.out.print("Input checkCode:");
                            checkCode = scanner.nextLine();
                            e.setCheckCode(checkCode);
                        }
                    if (threadList.size() != 0 && threadList.size() % threadCount == 0)
                        try {
                            do {
                                printTasks(threadList);
//                                inputCheck(scanner, threadList);
                            } while (!threadPool.awaitTermination(1, TimeUnit.MINUTES));
                        } catch (InterruptedException ignored) {
                        }
                }
            if (threadList.size() != 0)
                try {
                    do {
                        printTasks(threadList);
//                                inputCheck(scanner, threadList);
                    } while (!threadPool.awaitTermination(1, TimeUnit.MINUTES));
                } catch (InterruptedException ignored) {
                }
            threadPool.shutdown();
            System.out.println("Finished task count:" + threadList.size());
        } catch (RequestsException e) {
            System.out.println("Net connection error");
        }
    }

    private static boolean openFile(String path) {
        try {
            Desktop.getDesktop().open(new File(path));
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
        return false;
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

    private static void printTasks(List<PlayTask> threadList) {
//        try {
//            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
//        } catch (Exception ignored) {
//        }
        for (PlayTask task : threadList)
            System.out.println(task.getVideoName() + "[" + task.getPlayedPercent() + "%]");
    }

}
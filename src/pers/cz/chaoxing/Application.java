package pers.cz.chaoxing;

import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.thread.LimitedBlockingQueue;
import pers.cz.chaoxing.thread.PauseThreadPoolExecutor;
import pers.cz.chaoxing.thread.manager.HomeworkManager;
import pers.cz.chaoxing.thread.manager.PlayerManager;
import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.common.control.Control;
import pers.cz.chaoxing.common.school.SchoolData;
import pers.cz.chaoxing.common.school.SchoolInfo;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.thread.manager.ExamManager;
import pers.cz.chaoxing.thread.manager.ManagerModel;
import pers.cz.chaoxing.util.*;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.io.StateControlFilter;
import pers.cz.chaoxing.util.io.StringUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
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
 * @version 1.4.3
 */
public class Application {

    private static void copyright() {
        IOUtil.println("ChaoxingPlugin v1.4.3 - powered by orange");
        IOUtil.println("License - GPLv3: This is a free & share software");
        IOUtil.println("You can checking source code from: https://github.com/cz111000/chaoxing");
    }

    private static String login() {
        SchoolData schoolData = Application.getSchoolData();
        Application.notice(schoolData.getId());
        while (true)
            try {
                String username = IOUtil.printAndNextLine("Input account:");
                String password = IOUtil.printAndNextLine("Input password:");
                return Try.ever(() -> CXUtil.login(schoolData.getId(), username, password, ""),
                        CheckCodeFactory.LOGIN.get(),
                        String.valueOf(schoolData.getId()), username, password);
            } catch (WrongAccountException e) {
                IOUtil.println(e.getLocalizedMessage());
            }
    }

    private static void config(final String indexURL, IOUtil.ScanJob scanJob) {
        Control control = new Control();
        control.setSleep(!IOUtil.printAndNext("Using fast mode (may got WARNING, suggest you DO NOT USE) [y/n]:").equalsIgnoreCase("y"));
        control.setReview(IOUtil.printAndNext("Doing review mode homework&exam (may got WARNING, suggest you DO NOT USE) [y/n]:").equalsIgnoreCase("y"));
        if (IOUtil.printAndNext("Storing only matching answers in homework&exam (completing all answers if choose no) [y/n]:").equalsIgnoreCase("y"))
            control.setCompleteStyle(CompleteStyle.NONE);
        else if (IOUtil.printAndNext("Auto-completing all answers (may got lower mark, manual-completing if not) [y/n]:").equalsIgnoreCase("y"))
            control.setCompleteStyle(CompleteStyle.AUTO);
        else
            control.setCompleteStyle(CompleteStyle.MANUAL);
        try (
                PlayerManager playerManager = new PlayerManager(
                        IOUtil.printAndNextInt("Input size of playerThreadPool(suggest max size is 3):"));
                HomeworkManager homeworkManager = new HomeworkManager(
                        IOUtil.printAndNextInt("Input size of homeworkThreadPool(suggest max size is 1):"));
                ExamManager examManager = new ExamManager(
                        IOUtil.printAndNextInt("Input size of examThreadPool(suggest max size is 1):"))
        ) {
            final String coursesURL = Try.ever(() -> CXUtil.getCoursesURL(indexURL), CheckCodeFactory.CUSTOM.get());
            List<String> courseURLs = Try.ever(() -> CXUtil.getCourseURLs(coursesURL), CheckCodeFactory.CUSTOM.get());
            Map<String, List<List<String>>> urlMap = courseURLs.stream()
                    .map(Try.ever(CXUtil::getCourseInfo, CheckCodeFactory.CUSTOM.get()))
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> {
                                List<List<String>> list = new ArrayList<>();
                                list.add(entry.getValue());
                                return list;
                            },
                            (oldList, newList) -> {
                                oldList.addAll(newList);
                                return oldList;
                            }
                    ));
            urlMap.put("courseURLs", Collections.singletonList(courseURLs));
            List<ManagerModel> managers = Arrays.asList(playerManager, homeworkManager, examManager);
            final PauseThreadPoolExecutor threadPool = new PauseThreadPoolExecutor(managers.size(), managers.size(), 0L, TimeUnit.MILLISECONDS, new LimitedBlockingQueue<>(1));
            scanJob.setInputFilter(new StateControlFilter(threadPool));
            IOUtil.println("Press 'p' to pause, press 's' to stop, press any key to continue");
            Application.initManagers(managers, urlMap, control);
            Application.startManagers(managers, threadPool).join();
            threadPool.shutdown();
        }
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
        SchoolData schoolData = new SchoolData(1322, "");
        while (schoolData.getName().isEmpty()) {
            schoolData.setName(IOUtil.printAndNextLine("Input school name or fid (default is 1322):").trim());
            if (schoolData.getName().isEmpty())
                break;
            else if (schoolData.getName().matches("-?[\\d\\s]+"))
                schoolData.setId(Integer.parseInt(schoolData.getName()));
            else {
                final SchoolData finalSchoolData = schoolData;
                SchoolInfo schoolInfo = Try.ever(() -> CXUtil.searchSchool(finalSchoolData.getName()), CheckCodeFactory.CUSTOM.get());
                if (schoolInfo.isResult()) {
                    IntStream.range(0, schoolInfo.getFromNums()).forEach(i -> IOUtil.println((i + 1) + ":" + schoolInfo.getFroms()[i].getName()));
                    int index = IOUtil.printAndNextInt("Choose school index:");
                    if (index > 0 && index <= schoolInfo.getFromNums()) {
                        schoolData = schoolInfo.getFroms()[index - 1];
                        break;
                    }
                }
                schoolData.setName("");
            }
        }
        return schoolData;
    }

    private static void initManagers(List<ManagerModel> managers, Map<String, List<List<String>>> urlMap, Control control) {
        managers.forEach(manager -> {
            manager.setControl(control);
            if (manager instanceof ExamManager)
                manager.setUrls(urlMap.get("courseURLs"));
            else
                manager.setUrls(urlMap.get("chapterURLs"));
        });
    }

    private static CompletableFuture<Void> startManagers(List<ManagerModel> managers, final ThreadPoolExecutor threadPool) {
        final ManagerModel lastManager = managers.get(managers.size() - 1);
        CompletableFuture[] futures = managers.stream()
                .filter(manager -> !manager.equals(lastManager))
                .map(runnable -> CompletableFuture.runAsync(runnable, threadPool))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures).thenRunAsync(lastManager, threadPool);
    }

    public static void main(String[] args) {
        Application.copyright();
        try (IOUtil.ScanJob scanJob = new IOUtil.ScanJob()) {
            CompletableFuture.runAsync(scanJob);
            try {
                Application.config(Application.login(), scanJob);
            } catch (RequestsException e) {
                String message = StringUtil.subStringAfterFirst(e.getLocalizedMessage(), "Exception:").trim();
                IOUtil.println("Net connection error: " + message);
            } catch (Exception ignored) {
            }
        }
    }
}
/*
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
package pers.cz.chaoxing;

import net.dongliu.requests.exception.RequestsException;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.common.control.Control;
import pers.cz.chaoxing.common.school.SchoolData;
import pers.cz.chaoxing.common.school.SchoolInfo;
import pers.cz.chaoxing.exception.WrongAccountException;
import pers.cz.chaoxing.thread.LimitedBlockingQueue;
import pers.cz.chaoxing.thread.PauseThreadPoolExecutor;
import pers.cz.chaoxing.thread.manager.ExamManager;
import pers.cz.chaoxing.thread.manager.HomeworkManager;
import pers.cz.chaoxing.thread.manager.ManagerModel;
import pers.cz.chaoxing.thread.manager.PlayerManager;
import pers.cz.chaoxing.util.CXUtil;
import pers.cz.chaoxing.util.CompleteStyle;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.io.StateControlFilter;
import pers.cz.chaoxing.util.io.StringUtil;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @author 橙子
 * @version 1.5.1
 */
public class Application {

    private static void copyright() {
        IOUtil.println("application_name", "v1.5.1");
        IOUtil.println("application_copyright");
    }

    private static String login() {
        SchoolData schoolData = Application.getSchoolData();
        Application.notice(schoolData.getId());
        while (true)
            try {
                String username = IOUtil.printAndNextLine("input_account");
                String password = IOUtil.printAndNextLine("input_password");
                return Try.ever(() -> CXUtil.login(schoolData.getId(), username, password, ""),
                        CheckCodeFactory.LOGIN.get(),
                        String.valueOf(schoolData.getId()), username, password);
            } catch (WrongAccountException e) {
                IOUtil.println("exception_account", e.getLocalizedMessage());
            }
    }

    private static void config(final String indexURL, IOUtil.ScanJob scanJob) {
        Control control = new Control();
        control.setSleep(!IOUtil.printAndNext("input_fast_mode").equalsIgnoreCase("y"));
        control.setReview(IOUtil.printAndNext("input_review_mode").equalsIgnoreCase("y"));
        if (IOUtil.printAndNext("input_store_mode").equalsIgnoreCase("y"))
            control.setCompleteStyle(CompleteStyle.NONE);
        else if (IOUtil.printAndNext("input_auto_complete_mode").equalsIgnoreCase("y"))
            control.setCompleteStyle(CompleteStyle.AUTO);
        else
            control.setCompleteStyle(CompleteStyle.MANUAL);
        try (
                PlayerManager playerManager = new PlayerManager(
                        IOUtil.printAndNextInt("input_player_pool_size"));
                HomeworkManager homeworkManager = new HomeworkManager(
                        IOUtil.printAndNextInt("input_homework_pool_size"));
                ExamManager examManager = new ExamManager(
                        IOUtil.printAndNextInt("input_exam_pool_size"))
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
            IOUtil.println("application_start");
            Application.initManagers(managers, urlMap, control);
            Application.startManagers(managers, threadPool).join();
            threadPool.shutdown();
        }
    }

    private static void notice(int fid) {
        String notice = Try.ever(() -> CXUtil.getNotice(fid), CheckCodeFactory.CUSTOM.get());
        if (!notice.isEmpty())
            IOUtil.println("notice_start", notice);
        switch (fid) {
            case 1581:
                IOUtil.println("notice_nmgcfxy");
                break;
            case 1630:
                IOUtil.println("notice_nysfxy");
                break;
            case 1639:
                IOUtil.println("notice_wcxq");
                break;
            case 1661:
                IOUtil.println("notice_nbgcxy");
                break;
            case 2025:
            case 2073:
            case 1588:
            case 878:
            case 11766:
                IOUtil.println("notice_11+s1");
                break;
            case 1694:
            case 2125:
            case 183:
            case 1748:
            case 1267:
            case 10749:
                IOUtil.println("notice_11+s2");
                break;
            case 1254:
                IOUtil.println("notice_hbykdx");
                break;
            case 1192:
                IOUtil.println("notice_gdkmzyxy");
                break;
            case 8523:
                IOUtil.println("notice_ylsfxy");
                break;
            case 1491:
                IOUtil.println("notice_jjxy");
                break;
            case 13472:
                IOUtil.println("notice_11+s3");
                break;
            case 13491:
                IOUtil.println("notice_11+s3");
                break;
            case 13496:
                IOUtil.println("notice_11+s3");
                break;
            case 13659:
                IOUtil.println("notice_qdnmzzyjsxyxly");
                break;
            case 13673:
                IOUtil.println("notice_11_start1+s");
                break;
            case 13133:
                IOUtil.println("notice_11_start1+s");
                break;
            case 13148:
                IOUtil.println("notice_11_start1+s");
                break;
            case 13895:
                IOUtil.println("notice_11_start1+s");
                break;
            case 14046:
                IOUtil.println("notice_11_start1+s");
                break;
            case 14345:
                IOUtil.println("notice_11_start1+s");
                break;
            case 1606:
                IOUtil.println("notice_lfsfxy");
                break;
            case 14286:
                IOUtil.println("notice_11_start1+s");
                break;
            case 13113:
                IOUtil.println("notice_11_start1+s");
                break;
        }
    }

    private static SchoolData getSchoolData() {
        SchoolData schoolData = new SchoolData(1322, "");
        while (schoolData.getName().isEmpty()) {
            schoolData.setName(IOUtil.printAndNextLine("input_school").trim());
            if (schoolData.getName().isEmpty())
                break;
            else if (schoolData.getName().matches("-?[\\d\\s]+"))
                schoolData.setId(Integer.parseInt(schoolData.getName()));
            else {
                final SchoolData finalSchoolData = schoolData;
                SchoolInfo schoolInfo = Try.ever(() -> CXUtil.searchSchool(finalSchoolData.getName()), CheckCodeFactory.CUSTOM.get());
                if (schoolInfo.isResult()) {
                    IntStream.range(0, schoolInfo.getFromNums()).forEach(i -> IOUtil.println("school_list_item", i + 1, schoolInfo.getFroms()[i].getName(), schoolInfo.getFroms()[i].getId()));
                    int index = IOUtil.printAndNextInt("input_school_choose");
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
                manager.setUrls(urlMap.getOrDefault("courseURLs", Collections.emptyList()));
            else
                manager.setUrls(urlMap.getOrDefault("chapterURLs", Collections.emptyList()));
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
                IOUtil.println("exception_network", StringUtil.subStringAfterFirst(e.getLocalizedMessage(), ":").trim());
            } catch (Exception ignored) {
            }
        }
    }
}
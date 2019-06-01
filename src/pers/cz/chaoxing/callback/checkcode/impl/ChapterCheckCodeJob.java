package pers.cz.chaoxing.callback.checkcode.impl;

import net.dongliu.requests.Parameter;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.StatusCodes;
import pers.cz.chaoxing.callback.checkcode.CheckCodeCallBack;
import pers.cz.chaoxing.callback.checkcode.CheckCodeData;
import pers.cz.chaoxing.callback.checkcode.CheckCodeFactory;
import pers.cz.chaoxing.util.Try;
import pers.cz.chaoxing.util.io.IOUtil;
import pers.cz.chaoxing.util.net.NetUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class ChapterCheckCodeJob extends CheckCodeJobModel implements CheckCodeCallBack<CheckCodeData> {
    private CheckCodeData checkCodeData;

    public ChapterCheckCodeJob(String checkCodePath) {
        super(checkCodePath);
        this.checkCodeData = new CheckCodeData(true, "", "0");
    }

    @Override
    public CheckCodeData onCheckCode(String... param) {
        if (lock.writeLock().tryLock()) {
            receiveURL = param[0];
            NetUtil.getQueries(receiveURL).stream().filter(parameter -> parameter.getName().equals("cpi")).map(Parameter::getValue).findAny().ifPresent(checkCodeData::setCpi);
            baseURL = NetUtil.getOriginal(receiveURL);
            try {
                do {
                    if (!receiveCheckCode(checkCodePath))
                        break;
                    if (!checkCodeData.getCpi().equals("0")) {
                        if (!readCheckCode(checkCodePath))
                            IOUtil.println("check_code_image_path", checkCodePath);
                        checkCodeData = sendCheckCode(IOUtil.printAndNext("input_check_code"));
                    }
                } while (!checkCodeData.isStatus());
            } finally {
                lock.writeLock().unlock();
            }
        }
        lock.readLock().lock();
        try {
            return checkCodeData;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    protected boolean receiveCheckCode(String path) {
        do {
            RawResponse response = Try.ever(() -> NetUtil.get(receiveURL, 0), CheckCodeFactory.CUSTOM.get());
            if (StatusCodes.NOT_FOUND == response.getStatusCode())
                return false;
            response.writeToFile(path);
            try (Stream<String> lines = Files.lines(Paths.get(path), StandardCharsets.ISO_8859_1)) {
                if (lines.anyMatch(line -> line.contains("<html")))
                    CheckCodeFactory.CUSTOM.get().onCheckCode(receiveURL);
                else
                    break;
            } catch (IOException ignored) {
            }
        } while (!checkCodeData.getCpi().equals("0"));
        return true;
    }

    @Override
    protected CheckCodeData sendCheckCode(String checkCode) {
        checkCodeData.setCode(checkCode);
        return checkCodeData;
    }
}

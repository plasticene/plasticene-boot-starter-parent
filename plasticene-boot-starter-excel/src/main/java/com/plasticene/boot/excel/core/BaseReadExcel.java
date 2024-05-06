package com.plasticene.boot.excel.core;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.util.StringUtils;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.excel.core.listener.CheckExcelListener;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/4/22 15:09
 * 公共读取（导入）数据处理
 */
public abstract class BaseReadExcel implements ReadExcel {

    @Override
    public void checkFile(MultipartFile file) {
        if (Objects.isNull(file)) {
            throw new BizException("文件对象不能为null");
        }
        if (file.isEmpty()) {
            throw new BizException("文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (!fileName.endsWith(".xls") && !fileName.endsWith(".xlsx")) {
            throw new BizException("excel格式不正确");
        }
    }

    @Override
    public void checkFileHead(MultipartFile file, List<String> headList) {
        checkFileHead(file, headList, 1);
    }

    @Override
    public void checkFileHead(MultipartFile file, List<String> headList, Integer headRowNumber) {
        if (Objects.isNull(headRowNumber)) {
            headRowNumber = 1;
        }
        CheckExcelListener checkExcelListener = new CheckExcelListener(headList);
        try {
            EasyExcel.read(file.getInputStream(), new CheckExcelListener(headList))
                    .sheet().headRowNumber(headRowNumber).doRead();
        } catch (Exception e) {
            // 抛出异常结束excel解析，所以这里不做任何处理
        }
        if (StringUtils.isNotBlank(checkExcelListener.getErrMsg())) {
            throw new BizException(checkExcelListener.getErrMsg());
        }
    }

    @Override
    public void checkFileDataIsEmpty(MultipartFile file) {
        checkFileDataIsEmpty(file, 1);
    }

    @Override
    public void checkFileDataIsEmpty(MultipartFile file, Integer headRowNumber) {
        if (Objects.isNull(headRowNumber)) {
            headRowNumber = 1;
        }
        CheckExcelListener checkExcelListener = new CheckExcelListener(null);
        try {
            EasyExcel.read(file.getInputStream(), new CheckExcelListener(null))
                    .sheet().headRowNumber(headRowNumber).doRead();
        } catch (Exception e) {
            // 抛出异常结束excel解析，所以这里不做任何处理
        }
        if (CollectionUtils.isEmpty(checkExcelListener.getDataList())) {
            throw new BizException("excel文件数据内容不能为空");
        }
    }

    @Override
    public void checkFileHeadAndDataIsEmpty(MultipartFile file, List<String> headList) {
        checkFileHead(file, headList);
        checkFileDataIsEmpty(file);
    }

    @Override
    public void checkFileHeadAndDataIsEmpty(MultipartFile file, List<String> headList, Integer headRowNum) {
        checkFileHead(file, headList,headRowNum);
        checkFileDataIsEmpty(file, headRowNum);
    }

    @Override
    public String uploadFile(MultipartFile file) {
        return null;
    }

    @Override
    public void analysisFile(MultipartFile file) {

    }

    @Override
    public void handleFinish() {

    }
}

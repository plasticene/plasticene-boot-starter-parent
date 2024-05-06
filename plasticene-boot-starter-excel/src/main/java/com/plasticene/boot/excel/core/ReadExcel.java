package com.plasticene.boot.excel.core;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/4/22 15:55
 * 读取excel最订场接口封装
 */
public interface ReadExcel {

    /** =============>>>> 文件检查  */

    void checkFile(MultipartFile file);

    void checkFileHead(MultipartFile file, List<String> headList);

    void checkFileHead(MultipartFile file, List<String> headList, Integer headRowNum);

    void checkFileDataIsEmpty(MultipartFile file);

    void checkFileDataIsEmpty(MultipartFile file, Integer headRowNum);

    void checkFileHeadAndDataIsEmpty(MultipartFile file, List<String> headList);

    void checkFileHeadAndDataIsEmpty(MultipartFile file, List<String> headList, Integer headRowNum);


    /** =============>>>> 上传文件  */
    String uploadFile(MultipartFile file);

    /** =============>>>> 解析文件  */
    void analysisFile(MultipartFile file);

    /** =============>>>> 完成收尾工作  */
    void handleFinish();






}

package com.plasticene.boot.license.web;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.plasticene.boot.common.exception.BizException;
import com.plasticene.boot.license.core.LicenseCreator;
import com.plasticene.boot.license.core.param.LicenseCreatorParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.web.bind.annotation.*;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 19:03
 */
@RestController
@Tag(name = "license管理")
@RequestMapping("/license")
@Slf4j
public class LicenseController {

    @Resource
    private LicenseCreator licenseCreator;

    @PostMapping("/create")
    @Operation(summary = "生成license")
    public void create(@RequestBody LicenseCreatorParam creatorParam, HttpServletResponse response) throws IOException {
        File tempFile = new File("license_" + System.currentTimeMillis() + ".lic");
        try {
            licenseCreator.generateLicense(creatorParam, tempFile);
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", "attachment;filename=license.lic");
            BufferedInputStream inputStream = FileUtil.getInputStream(tempFile);
            IoUtil.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            log.error("生成license文件失败:", e);
            throw new BizException("生产license文件失败");
        } finally {
            FileUtils.delete(tempFile);
        }
    }
}

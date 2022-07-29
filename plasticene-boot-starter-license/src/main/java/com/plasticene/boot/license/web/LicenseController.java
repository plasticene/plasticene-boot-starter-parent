package com.plasticene.boot.license.web;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import com.plasticene.boot.license.core.LicenseCreator;
import com.plasticene.boot.license.core.param.LicenseCreatorParam;
import com.plasticene.boot.license.core.prop.LicenseProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/28 19:03
 */
@RestController
@Api(tags = "license管理")
@RequestMapping("/license")
public class LicenseController {

    @Resource
    private LicenseCreator licenseCreator;
    @Resource
    private LicenseProperties licenseProperties;

    @GetMapping
    @ApiOperation("生成license")
    public void create(LicenseCreatorParam creatorParam, HttpServletResponse response) throws IOException {
        boolean flag = licenseCreator.generateLicense(creatorParam);
        response.setContentType("application/octet-stream");
        BufferedInputStream inputStream = FileUtil.getInputStream(licenseProperties.getLicensePath());
        IoUtil.copy(inputStream, response.getOutputStream());
    }
}

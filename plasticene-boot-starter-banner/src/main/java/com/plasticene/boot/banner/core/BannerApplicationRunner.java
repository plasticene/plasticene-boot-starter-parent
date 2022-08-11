package com.plasticene.boot.banner.core;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.plasticene.boot.common.constant.OrderConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.core.annotation.Order;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/7/12 15:11
 */
@Slf4j
@Order(OrderConstant.RUNNER_BANNER)
public class BannerApplicationRunner implements ApplicationRunner {

    @Resource
    private ServerProperties serverProperties;

    @Value("${spring.application.name}")
    private String name;

    private static final String URL_PREFIX = "http://127.0.0.1:";

    @Override
    public void run(ApplicationArguments args) {
        StringBuilder sb = new StringBuilder(URL_PREFIX + serverProperties.getPort());
        String contextPath = serverProperties.getServlet().getContextPath();
        if (StrUtil.isNotBlank(contextPath)) {
            if (! contextPath.startsWith("/")) {
                sb.append("/");
            }
            sb.append(contextPath);
        }
        sb.append("/doc.html");
        ThreadUtil.execute(() -> {
            ThreadUtil.sleep(1, TimeUnit.SECONDS); // 延迟 1 秒，保证输出到结尾
            log.info("\n----------------------------------------------------------\n\t" +
                            "(♥◠‿◠)ﾉﾞ  {}启动成功   ლ(´ڡ`ლ)ﾞ\n\t" +
                            "接口文档:  {} \n" +
                            "----------------------------------------------------------",
                    name, sb.toString());
        });
    }

}

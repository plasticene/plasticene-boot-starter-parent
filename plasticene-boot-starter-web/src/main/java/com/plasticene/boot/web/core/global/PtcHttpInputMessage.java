package com.plasticene.boot.web.core.global;

import cn.hutool.core.io.IoUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2024/7/10 15:32
 */
public class PtcHttpInputMessage implements HttpInputMessage {
    private HttpHeaders headers;
    private String body;



    public PtcHttpInputMessage(HttpHeaders headers, String body) {
        this.headers = headers;
        this.body = body;
    }

    @Override
    public InputStream getBody() {
        return IoUtil.toUtf8Stream(body);
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }
}

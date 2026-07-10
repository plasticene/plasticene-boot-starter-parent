package com.plasticene.boot.web.core.global;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.Getter;
import org.springframework.util.StreamUtils;


import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2023/4/27 14:01
 */
@Getter
public class RequestBodyWrapper extends HttpServletRequestWrapper {

    //用于将流保存下来
    private final byte[] body;

    public RequestBodyWrapper(HttpServletRequest request) throws IOException {
        super(request);
        body = StreamUtils.copyToByteArray(request.getInputStream());
    }

    /**
     * 重写getInputStream， 从body中获取请求参数
     */
    @Override
    public ServletInputStream getInputStream() throws IOException {
        final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body);
        return
                new ServletInputStream() {
                    @Override
                    public boolean isFinished() {
                        return false;
                    }

                    @Override
                    public boolean isReady() {
                        return false;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {

                    }

                    @Override
                    public int read() throws IOException {
                        return byteArrayInputStream.read();
                    }
                };
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}
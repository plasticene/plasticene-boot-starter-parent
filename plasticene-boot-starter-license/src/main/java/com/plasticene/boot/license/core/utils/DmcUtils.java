package com.plasticene.boot.license.core.utils;

import com.plasticene.boot.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 获取linux服务器cpu序列号
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/1 18:26
 */
@Slf4j
public class DmcUtils {
    public static String getCpuId() {
        String result = "";
        String cmd = "dmidecode -t 4 | grep ID |sort -u |awk -F': ' '{print $2}'";
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "sh", "-c", cmd });// 管道
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = reader.readLine().trim();
            if(StringUtils.isNotBlank(line)){
                result  = line;
            }
            reader.close();
            return result.trim();

        } catch (IOException e) {
            log.error("获取服务器cpu id错误：", e);
            throw new BizException("获取服务器cpuId失败");
        }
    }

    public static String getSystemUuid() {
        String result = "";
        String cmd = "dmidecode -s system-uuid | tr 'A-Z' 'a-z'";
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "sh", "-c", cmd });// 管道
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine().trim();
            if(StringUtils.isNotBlank(line)){
                result  = line;
            }
            reader.close();
            return result.trim();

        } catch (IOException e) {
            log.error("获取系统uuid错误：", e);
            throw new BizException("获取系统uuid失败");
        }
    }

    public static String getMainBordId() {
        String result = "";
        String cmd = "dmidecode | grep 'Serial Number' | awk '{print $3}' | tail -1";
        try {
            Process p = Runtime.getRuntime().exec(new String[] { "sh", "-c", cmd });// 管道
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = reader.readLine().trim();
            if(StringUtils.isNotBlank(line)){
                result  = line;
            }
            reader.close();
            return result.trim();

        } catch (IOException e) {
            log.error("获取主板id错误：", e);
            throw new BizException("获取主板id失败");
        }
    }
}

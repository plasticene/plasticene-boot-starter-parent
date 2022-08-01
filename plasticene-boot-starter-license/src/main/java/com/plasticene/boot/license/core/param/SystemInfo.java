package com.plasticene.boot.license.core.param;

import lombok.Data;

import java.io.Serializable;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/8/1 18:16
 */
@Data
public class SystemInfo implements Serializable {

    /**
     * 系统uuid全局唯一
     * linux命令：dmidecode -s system-uuid | tr 'A-Z' 'a-z'
     */
    private String uuid;

    /**
     * 可被允许的CPU序列号
     *
     * linux命令：dmidecode -t 4 | grep ID |sort -u |awk -F': ' '{print $2}'
     */
    private String cpuId;

    /**
     * 可被允许的主板序列号
     */
    private String mainBoardSerial;
}

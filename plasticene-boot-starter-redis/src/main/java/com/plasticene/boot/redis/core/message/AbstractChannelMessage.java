package com.plasticene.boot.redis.core.message;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author fjzheng
 * @version 1.0
 * @date 2022/9/13 14:47
 */
public abstract class AbstractChannelMessage{

    /**
     * 获得 Redis Channel
     *
     * @return Channel
     */
    @JsonIgnore // 避免序列化。原因是，Redis 发布 Channel 消息的时候，已经会指定。
    public abstract String getChannel();

}
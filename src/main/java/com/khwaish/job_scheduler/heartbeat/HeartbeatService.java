package com.khwaish.job_scheduler.heartbeat;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class HeartbeatService {

    private final StringRedisTemplate redisTemplate;

    public HeartbeatService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private static final Duration HEARTBEAT_TTL = Duration.ofSeconds(6);

    public void sendHeartbeat(String workerId) {
        String key = "worker:heartbeat:" + workerId;
        redisTemplate.opsForValue().set(key, "ALIVE", HEARTBEAT_TTL);
    }

    public boolean isWorkerAlive(String workerId) {
        String key = "worker:heartbeat:" + workerId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }
}
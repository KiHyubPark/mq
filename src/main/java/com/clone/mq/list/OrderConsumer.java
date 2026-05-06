package com.clone.mq.list;

import com.clone.mq.config.RedisConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderConsumer {

    private final StringRedisTemplate redisTemplate;

    public OrderConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 1초마다 큐에서 메시지를 꺼내 처리 (FIFO: leftPop)
    @Scheduled(fixedDelay = 1000)
    public void consumeOrder() {
        String message = redisTemplate.opsForList().leftPop(RedisConfig.ORDER_QUEUE_KEY);
        if (message != null) {
            System.out.println("[Consumer] 주문 처리: " + message);
        }
    }
}

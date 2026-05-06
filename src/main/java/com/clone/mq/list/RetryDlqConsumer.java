package com.clone.mq.list;

import com.clone.mq.config.RedisConfig;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RetryDlqConsumer {

    private static final String DIVIDER =
            "────────────────────────────────────────────────────────────";

    private final StringRedisTemplate redisTemplate;
    private final AtomicLong seq = new AtomicLong();

    public RetryDlqConsumer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 1초마다 retry 큐에서 메시지를 꺼내 처리 (학습용 폴링 방식)
    @Scheduled(fixedDelay = 1000)
    public void consumeRetryQueue() {
        String message = redisTemplate.opsForList().leftPop(RedisConfig.RETRY_QUEUE_KEY);
        if (message == null) {
            return;
        }

        long messageSeq = seq.incrementAndGet();
        String payload = parsePayload(message);
        int retry = parseRetry(message);

        System.out.println(DIVIDER);
        System.out.printf(" [#%d] POP   %s  ←  '%s'%n",
                messageSeq, RedisConfig.RETRY_QUEUE_KEY, message);
        System.out.printf("       parse payload=%s, retry=%d/%d%n",
                payload, retry, RedisConfig.MAX_RETRY);

        // 학습용 강제 실패 트리거: payload 가 "FAIL_" 로 시작하면 실패로 간주
        boolean failed = payload.startsWith("FAIL_");

        if (!failed) {
            System.out.printf("       OK    → 처리 완료 (총 시도 %d회)%n", retry + 1);
            return;
        }

        System.out.println("       FAIL  → 강제 실패 트리거 감지");

        int nextRetry = retry + 1;
        if (nextRetry < RedisConfig.MAX_RETRY) {
            String requeued = payload + "|retry=" + nextRetry;
            redisTemplate.opsForList().rightPush(RedisConfig.RETRY_QUEUE_KEY, requeued);
            Long retrySize = redisTemplate.opsForList().size(RedisConfig.RETRY_QUEUE_KEY);
            System.out.printf("       RETRY → RPUSH %s  ←  '%s'%n",
                    RedisConfig.RETRY_QUEUE_KEY, requeued);
            System.out.printf("               다음 시도 %d/%d, %s 큐 크기=%d%n",
                    nextRetry, RedisConfig.MAX_RETRY - 1,
                    RedisConfig.RETRY_QUEUE_KEY, retrySize);
        } else {
            redisTemplate.opsForList().rightPush(RedisConfig.DLQ_KEY, payload);
            Long dlqSize = redisTemplate.opsForList().size(RedisConfig.DLQ_KEY);
            System.out.printf("       DLQ   → MAX_RETRY(%d) 초과 → RPUSH %s  ←  '%s'%n",
                    RedisConfig.MAX_RETRY, RedisConfig.DLQ_KEY, payload);
            System.out.printf("               DLQ 크기=%d  (조회: redis-cli LRANGE %s 0 -1)%n",
                    dlqSize, RedisConfig.DLQ_KEY);
        }
    }

    // "ORDER-1|retry=2" -> "ORDER-1"
    private String parsePayload(String message) {
        int idx = message.indexOf('|');
        return idx < 0 ? message : message.substring(0, idx);
    }

    // "ORDER-1|retry=2" -> 2, 없으면 0
    private int parseRetry(String message) {
        int idx = message.indexOf("retry=");
        if (idx < 0) {
            return 0;
        }
        try {
            return Integer.parseInt(message.substring(idx + "retry=".length()).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}

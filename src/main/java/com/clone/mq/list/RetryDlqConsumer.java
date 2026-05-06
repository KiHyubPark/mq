package com.clone.mq.list;

import com.clone.mq.config.RedisKeys;
import com.clone.mq.enums.RedisQueue;
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
        String message = redisTemplate.opsForList().leftPop(RedisQueue.RETRY.key);
        if (message == null) {
            return;
        }

        long messageSeq = seq.incrementAndGet();
        String payload = parsePayload(message);
        int retry = parseRetry(message);

        System.out.println(DIVIDER);
        System.out.printf(" [#%d] POP   %s  ←  '%s'  (retry=%d/%d)%n",
                messageSeq, RedisQueue.RETRY.key, payload, retry, RedisKeys.MAX_RETRY);

        // 학습용 강제 실패 트리거: payload 가 "FAIL_" 로 시작하면 실패로 간주
        boolean failed = payload.startsWith("FAIL_");

        if (!failed) {
            System.out.printf("       OK    → 처리 완료 (총 시도 %d회)%n", retry + 1);
            return;
        }

        System.out.println("       FAIL  → 강제 실패 트리거 감지");

        int nextRetry = retry + 1;
        if (nextRetry < RedisKeys.MAX_RETRY) {
            String requeued = payload + "|retry=" + nextRetry;
            redisTemplate.opsForList().rightPush(RedisQueue.RETRY.key, requeued);
            Long retrySize = redisTemplate.opsForList().size(RedisQueue.RETRY.key);
            System.out.printf("       RETRY → RPUSH %s  ←  '%s'%n",
                    RedisQueue.RETRY.key, requeued);
            System.out.printf("               다음 시도 %d/%d, %s 큐 크기=%d%n",
                    nextRetry, RedisKeys.MAX_RETRY - 1,
                    RedisQueue.RETRY.key, retrySize);
        } else {
            redisTemplate.opsForList().rightPush(RedisQueue.DLQ.key, payload);
            Long dlqSize = redisTemplate.opsForList().size(RedisQueue.DLQ.key);
            System.out.printf("       DLQ   → MAX_RETRY(%d) 초과 → RPUSH %s  ←  '%s'%n",
                    RedisKeys.MAX_RETRY, RedisQueue.DLQ.key, payload);
            System.out.printf("               DLQ 크기=%d  (조회: redis-cli LRANGE %s 0 -1)%n",
                    dlqSize, RedisQueue.DLQ.key);
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

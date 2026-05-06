package com.clone.mq.config;

/**
 * Redis 관련 설정 상수.
 * 키/채널 식별자는 RedisQueue, RedisChannel enum 으로 관리하고,
 * enum 으로 표현하기 어색한 수치 설정만 여기에 둔다.
 */
public final class RedisKeys {

    private RedisKeys() {}

    /** 재시도 최대 횟수 — 이 횟수를 초과하면 DLQ 로 이동 */
    public static final int MAX_RETRY = 3;
}

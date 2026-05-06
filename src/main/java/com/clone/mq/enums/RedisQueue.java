package com.clone.mq.enums;

public enum RedisQueue {

    ORDER("order:queue", "기본 주문 처리 큐"),
    RETRY("order:retry", "재시도 큐 — 처리 실패 메시지를 재적재"),
    DLQ("order:dlq", "Dead Letter Queue — MAX_RETRY 초과 후 격리");

    public final String key;
    public final String description;

    RedisQueue(String key, String description) {
        this.key = key;
        this.description = description;
    }
}

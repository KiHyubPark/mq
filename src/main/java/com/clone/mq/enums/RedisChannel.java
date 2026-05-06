package com.clone.mq.enums;

public enum RedisChannel {

    ORDER_EVENT("order:event", "주문 완료 이벤트 Pub/Sub 채널");

    public final String key;
    public final String description;

    RedisChannel(String key, String description) {
        this.key = key;
        this.description = description;
    }
}

package com.clone.mq.pubsub;

import org.springframework.stereotype.Component;

@Component
public class OrderEventSubscriber {

    public void onStockUpdate(String message) {
        System.out.println("[재고 서비스] 수신: " + message + " → 재고 차감 처리");
    }

    public void onNotification(String message) {
        System.out.println("[알림 서비스] 수신: " + message + " → 고객 알림 발송");
    }

    public void onDelivery(String message) {
        System.out.println("[배송 서비스] 수신: " + message + " → 배송 준비 시작");
    }
}

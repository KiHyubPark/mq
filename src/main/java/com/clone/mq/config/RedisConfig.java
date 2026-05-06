package com.clone.mq.config;

import com.clone.mq.pubsub.OrderEventSubscriber;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    public static final String ORDER_QUEUE_KEY = "order:queue";
    public static final String ORDER_EVENT_CHANNEL = "order:event";

    // DLQ + 재시도 실습용 키
    public static final String RETRY_QUEUE_KEY = "order:retry";
    public static final String DLQ_KEY = "order:dlq";
    public static final int MAX_RETRY = 3;

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }

    @Bean
    public MessageListenerAdapter stockAdapter(OrderEventSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onStockUpdate");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    @Bean
    public MessageListenerAdapter notificationAdapter(OrderEventSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onNotification");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    @Bean
    public MessageListenerAdapter deliveryAdapter(OrderEventSubscriber subscriber) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(subscriber, "onDelivery");
        adapter.setSerializer(new StringRedisSerializer());
        return adapter;
    }

    // Pub/Sub: 채널에 메시지 수신 시 처리할 리스너 컨테이너
    @Bean
    public RedisMessageListenerContainer listenerContainer(
            RedisConnectionFactory factory,
            MessageListenerAdapter stockAdapter,
            MessageListenerAdapter notificationAdapter,
            MessageListenerAdapter deliveryAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        // 구독할 토픽 명
        ChannelTopic topic = new ChannelTopic(ORDER_EVENT_CHANNEL);

        // 해당 토픽을 구독하는 서비스
        // 재고 서비스
        container.addMessageListener(stockAdapter, topic);
        // 알림 서비스
        container.addMessageListener(notificationAdapter, topic);
        // 배송 서비스
        container.addMessageListener(deliveryAdapter, topic);

        return container;
    }
}

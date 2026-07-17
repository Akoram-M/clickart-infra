package com.clickart.notificationservice.event;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.clickart.notificationservice.dto.OrderPlacedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationListener {
    private final ObjectMapper objectMapper;

    // This annotation tells Spring to continuously poll the "notificationTopic"
    @KafkaListener(topics = "order-placed-topic", groupId = "notification-group")
    public void handleNotification(String orderPlacedEvent) {
        log.info("NotificationListener :: handleNotification <--");
        try {
            OrderPlacedEvent event = objectMapper.readValue(orderPlacedEvent, OrderPlacedEvent.class);
            log.info("Received Notification for Order - Sending Email for Order Number: {}", event.getOrderNumber());
            log.info("NotificationListener :: handleNotification -->");
        } catch (Exception e) {
            log.error("Error while parse notification data : ", e);
        }
    }
}
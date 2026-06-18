package com.clickart.notificationservice.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class NotificationListener {

    // This annotation tells Spring to continuously poll the "notificationTopic"
    @KafkaListener(topics = "notificationTopic")
    public void handleNotification(String orderNumber) {
        // In a real app, this triggers AWS SES or SendGrid. Here, we log it.
        log.info("Received Notification for Order - Sending Email for Order Number: {}", orderNumber);
    }
}
package com.clickart.inventoryservice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;

class InventoryDlqIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static final String MAIN_TOPIC = "order-placed-topic";
    private static final String DLQ_TOPIC = "order-placed-topic.DLQ";

    @Test
    @DisplayName("Should isolate Poison Pill and route directly to DLQ without retries")
    void shouldRoutePoisonPillDirectlyToDlq() {
        String poisonPayload = "{\"orderNumber\":\"BROKEN-JSON\",\"skuQuantities\":";
        String eventId = "POISON-" + System.currentTimeMillis();

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                KAFKA_CONTAINER.getBootstrapServers(), "test-dlq-group-" + UUID.randomUUID(), "true");
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> consumerFactory = new DefaultKafkaConsumerFactory<>(consumerProps,
                new StringDeserializer(), new StringDeserializer());
        Consumer<String, String> testDlqConsumer = consumerFactory.createConsumer();
        testDlqConsumer.subscribe(Collections.singletonList(DLQ_TOPIC));

        // CRITICAL: Force initial poll to trigger partition assignment and avoid race
        // condition
        testDlqConsumer.poll(Duration.ofMillis(500));

        // Publish Poison Pill
        ProducerRecord<String, String> poisonRecord = new ProducerRecord<>(MAIN_TOPIC, poisonPayload);
        poisonRecord.headers().add(new RecordHeader("eventId", eventId.getBytes(StandardCharsets.UTF_8)));
        kafkaTemplate.send(poisonRecord);

        // Poll with a safer, larger duration
        ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(testDlqConsumer, Duration.ofSeconds(20));
        assertFalse(records.isEmpty(), "DLQ should contain the failed poison pill record");

        boolean poisonPillFound = false;
        for (var record : records) {
            if (record.value().equals(poisonPayload)) {
                poisonPillFound = true;
                break;
            }
        }

        assertTrue(poisonPillFound, "Exact malformed payload must be present in DLQ");
        testDlqConsumer.close();
    }
}
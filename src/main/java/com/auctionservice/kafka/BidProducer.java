package com.auctionservice.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class BidProducer {

    private static final Logger log = LoggerFactory.getLogger(BidProducer.class);
    static final String TOPIC = "bid-events";

    private final KafkaTemplate<String, BidEvent> kafkaTemplate;

    public BidProducer(KafkaTemplate<String, BidEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(BidEvent event) {
        // Partition key = auctionId ensures FIFO ordering per auction
        kafkaTemplate.send(TOPIC, event.auctionId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish bid event {} for auction {}", event.eventId(), event.auctionId(), ex);
                    } else {
                        log.debug("Bid event {} published to partition {}", event.eventId(),
                                result.getRecordMetadata().partition());
                    }
                });
    }
}

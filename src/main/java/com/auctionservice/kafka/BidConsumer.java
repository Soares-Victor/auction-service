package com.auctionservice.kafka;

import com.auctionservice.service.BidProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class BidConsumer {

    private static final Logger log = LoggerFactory.getLogger(BidConsumer.class);

    private final BidProcessingService bidProcessingService;

    public BidConsumer(BidProcessingService bidProcessingService) {
        this.bidProcessingService = bidProcessingService;
    }

    @KafkaListener(topics = BidProducer.TOPIC, groupId = "bid-processors", containerFactory = "bidKafkaListenerContainerFactory")
    public void consume(BidEvent event, Acknowledgment ack) {
        try {
            bidProcessingService.process(event);
            ack.acknowledge();
        } catch (Exception ex) {
            // Do not ack — Kafka will redeliver, preventing bid loss on unexpected failures
            log.error("Unrecoverable error processing bid event {}: {}", event.eventId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}

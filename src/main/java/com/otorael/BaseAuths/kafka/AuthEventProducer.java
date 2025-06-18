package com.otorael.BaseAuths.kafka;

import com.otorael.BaseAuths.dto.UserDetails;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuthEventProducer {

    private static final String TOPIC = "auth-events"; // Kafka topic name

    private final KafkaTemplate<String, UserDetails> kafkaTemplate;

    public AuthEventProducer(KafkaTemplate<String, UserDetails> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }
    public void sendAuthEvent(UserDetails event) {
        kafkaTemplate.send(TOPIC, event);
        System.out.println("Produced auth event: " + event.getFirstName());
    }
}
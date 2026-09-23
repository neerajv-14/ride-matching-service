package com.rideshare.matching_service.service;

import com.rideshare.matching_service.event.RideMatchingFailedEvent;
import com.rideshare.matching_service.event.RideMatchingRetryEvent;
import com.rideshare.matching_service.event.RideRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideEventConsumer {

    private final MatchingService matchingService;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Listens to ride.requested kafka topic.
     * Triggered every time Ride Service published a new ride request
     *
     * FLOW:
     * Ride Service -> Kafka (ride.requested) -> This Consumer -> MatchingService
     */

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(
                    delay = 2000,
                    multiplier = 2.0
            )
    )
    @KafkaListener(
            topics = "ride.requested",
            groupId = "matching-service-group"
    )
    public void consumeRideRequestedEvent(RideRequestedEvent event){
        matchingService.matchDriverForRide(event);
    }

    @DltHandler
    public void handleMatchingFailure(
            RideRequestedEvent event) {

        RideMatchingFailedEvent failedEvent =
                new RideMatchingFailedEvent(
                        event.getRideId(),
                        event.getRiderId(),
                        "NO_DRIVER_AVAILABLE"
                );

        kafkaTemplate.send(
                "ride.matching.failed",
                event.getRideId(),
                failedEvent
        );
    }




}
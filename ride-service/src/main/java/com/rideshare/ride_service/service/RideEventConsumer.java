package com.rideshare.ride_service.service;


import com.rideshare.ride_service.event.RideMatchedEvent;
import com.rideshare.ride_service.event.RideMatchingFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class RideEventConsumer {

    private final RideService rideService;

    @KafkaListener(
            topics = "ride.matched",
            groupId = "ride-service-group"
    )
    public void consumeRideMatchedEvent(RideMatchedEvent event){
        rideService.updateRideWithDriver(
                event.getRideId(),
                event.getDriverId()
        );
    }

    @KafkaListener(
            topics = "ride.matching.failed",
            groupId = "ride-service-group"
    )
    public void consumeMatchingFailedEvent(
            RideMatchingFailedEvent event) {

        rideService.markMatchingFailed(
                event.getRideId(),
                event.getReason()
        );
    }
}

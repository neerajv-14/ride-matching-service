package com.rideshare.matching_service.service;

import com.rideshare.matching_service.client.LocationServiceClient;
import com.rideshare.matching_service.dto.NearByDriverResponse;
import com.rideshare.matching_service.event.RideMatchedEvent;
import com.rideshare.matching_service.event.RideMatchingFailedEvent;
import com.rideshare.matching_service.event.RideMatchingRetryEvent;
import com.rideshare.matching_service.event.RideRequestedEvent;
import com.rideshare.matching_service.exception.NoDriverException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class MatchingService {

    private final LocationServiceClient locationServiceClient;

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String RIDE_MATCHED_TOPIC = "ride.matched";

    private static final String RIDE_MATCH_RETRY_TOPIC = "ride.match.retry";

    private static final String RIDE_MATCHING_FAILED_TOPIC = "ride.matching.failed";

    private static final double DEFAULT_SEARCH_RADIUS = 5.0;

    private static final int MAX_RETRIES = 3;
    // called when RideRequestEvent is consumed by kafka

    public void matchDriverForRide(RideRequestedEvent event){
        // Step 1: get the nearby drivers by calling LocationServiceClient
        List<NearByDriverResponse> nearByDriverResponses = locationServiceClient.getNearByDrivers(event.getPickupLatitude(),event.getPickupLongitude(),DEFAULT_SEARCH_RADIUS);

        if(nearByDriverResponses.isEmpty()){
            log.warn("No drivers found for ride: {}",event.getRideId());
            return;
        }

        // Step 2: Find the best drivers

        List<NearByDriverResponse> bestDrivers = findBestDrivers(nearByDriverResponses);

        NearByDriverResponse assignedDriver = null;

        for (NearByDriverResponse driver : bestDrivers) {

            boolean reserved = locationServiceClient.reserveDriver(
                    driver.getDriverId(),
                    event.getRideId()
            );

            if (reserved) {
                assignedDriver = driver;
                break;
            }
        }

        if(assignedDriver == null){
            log.warn("No Driver is available at this moment + " + event.getRideId() + " Please try again")
            throw new NoDriverException("No Driver is available at this moment. Please try again");
        }
        // Step 3: Publish RideMatchedEvent to kafka

        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideId(),
                event.getRiderId(),
                assignedDriver.getDriverId(),
                assignedDriver.getLatitude(),
                assignedDriver.getLongitude(),
                assignedDriver.getDistanceInKm()
        );
        log.info("Publishing RideMatchedEvent");
        kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId(), matchedEvent);

    }


    private List<NearByDriverResponse> findBestDrivers(
            List<NearByDriverResponse> drivers) {

        double distanceWeight = 0.7;
        double ratingWeight = 0.3;

        return drivers.stream()
                .sorted(Comparator.comparingDouble(driver -> {

                    // Distance score: closer = higher score
                    double distanceScore =
                            1.0 / (driver.getDistanceInKm() + 0.1);

                    // Simulated rating between 4.0 and 5.0
                    double simulatedRating =
                            4.0 + Math.random();

                    // Final weighted score
                    return (distanceScore * distanceWeight)
                            + (simulatedRating * ratingWeight);

                }).reversed())
                .toList();
    }

}

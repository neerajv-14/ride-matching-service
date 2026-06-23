package com.rideshare.matching_service.service;

import com.rideshare.matching_service.client.LocationServiceClient;
import com.rideshare.matching_service.dto.NearByDriverResponse;
import com.rideshare.matching_service.event.RideMatchedEvent;
import com.rideshare.matching_service.event.RideRequestedEvent;
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

    private final KafkaTemplate<String, RideMatchedEvent> kafkaTemplate;

    private static final String RIDE_MATCHED_TOPIC = "ride.matched";

    private static final double DEFAULT_SEARCH_RADIUS = 5.0;

    // called when RideRequestEvent is consumed by kafka

    public void matchDriverForRide(RideRequestedEvent event){
        // Step 1: get the nearby drivers by calling LocationServiceClient
        List<NearByDriverResponse> nearByDriverResponses = locationServiceClient.getNearByDrivers(event.getPickupLatitude(), event.getPickupLongitude(),DEFAULT_SEARCH_RADIUS);

        if(nearByDriverResponses.isEmpty()){
            log.warn("No drivers found for ride: {}",event.getRideId());
            return;
        }

        // Step 2: Find the best drivers

        Optional<NearByDriverResponse> bestDriver = findBestDriver(nearByDriverResponses);

        if(bestDriver.isEmpty()){
            log.warn("Could not find best driver for ride id:{}",event.getRideId());
        }

       //Assign the best driver

        NearByDriverResponse assignedDriver = bestDriver.get();

        // Step 3: Publish RideMatchedEvent to kafka

        RideMatchedEvent matchedEvent = new RideMatchedEvent(
                event.getRideId(),
                event.getRiderId(),
                assignedDriver.getDriverId(),
                assignedDriver.getLatitude(),
                assignedDriver.getLongitude(),
                assignedDriver.getDistanceInKm()
        );

        kafkaTemplate.send(RIDE_MATCHED_TOPIC, event.getRideId(), matchedEvent);
        log.info("RideMatchedEvent published");




    }

    private Optional<NearByDriverResponse> findBestDriver(
            List<NearByDriverResponse> drivers){

        double distanceWeight = 0.7;
        double ratingWeight = 0.3;

        return drivers.stream()
                .max(Comparator.comparingDouble(driver -> {
                    //Distance score: closer = higher score
                    // Add 0.1 to avoid division by zero
                    double distanceScore = 1.0/(driver.getDistanceInKm() + 0.1);

                    // Simulated rating between 4.0 and 5.0

                    double simulatedRating = 4.0 + Math.random();

                    //Final weighted score
                    return (distanceScore * distanceWeight)
                            + (simulatedRating * ratingWeight);
                }));
    }

}

package com.rideshare.ride_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event published to kafka when a ride is requested
 * Matching service consumes this event
 * TOPIC: ride.requested
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RideRequestedEvent {
    private String rideId;
    private String riderId;

    //PICKUP
    private double pickupLatitude;
    private double pickupLongitude;
    private String pickupAddress;

    //DROP - used to estimate fare
    private double dropLatitude;
    private double dropLongitude;
    private String dropAddress;
}

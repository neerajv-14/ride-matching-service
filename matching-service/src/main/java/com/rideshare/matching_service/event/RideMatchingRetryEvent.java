package com.rideshare.matching_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RideMatchingRetryEvent {

    private String rideId;
    private String riderId;
    private double pickupLatitude;
    private double pickupLongitude;
    private int retryCount;
}
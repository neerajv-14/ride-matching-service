package com.rideshare.ride_service.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "location-service",
        url = "${location.service.url}"
)
public interface LocationServiceClient {

    @PostMapping("/api/v1/locations/drivers/{driverId}/on-trip")
    void markDriverOnTrip(
            @PathVariable String driverId,
            @RequestParam String rideId
    );

    @PostMapping("/api/v1/locations/drivers/{driverId}/ride-complete")
    void releaseDriver(
            @PathVariable String driverId
    );
}
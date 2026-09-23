package com.rideshare.location_service.contoller;

import com.rideshare.location_service.dto.DriverLocationRequest;
import com.rideshare.location_service.dto.NearByDriverResponse;
import com.rideshare.location_service.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/locations")
@Slf4j
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    // driver's mobile call this method every 3 seconds.
    @PostMapping("/drivers/update")
    public ResponseEntity<String> updateDriverLocation(@RequestBody DriverLocationRequest driverLocationRequest){
        locationService.updateDriverLocation(driverLocationRequest);
        return ResponseEntity.ok("Driver location is updated");
    }
    // Matching service calls this when ride is requested
    @GetMapping("/drivers/nearby")
    public ResponseEntity<List<NearByDriverResponse>> getNearByDriversResponse(@RequestParam double latitude,@RequestParam double longitude,@RequestParam(defaultValue = "5.0") double radius){
        return ResponseEntity.ok(locationService.findNearByDrivers(latitude,longitude,radius));
    }

    // When driver goes offline, remove him.
    @DeleteMapping("/drivers/{driverId}")
    public ResponseEntity<String> removeDriverByID(@PathVariable String driverId){
        locationService.removeDriverByID(driverId);
        return ResponseEntity.ok("Driver is removed");
    }

    @PostMapping("/drivers/{driverId}/on-trip")
    public void markDriverOnTrip(
            @PathVariable String driverId,
            @RequestParam String rideId) {

        locationService.markDriverOnTrip(driverId, rideId);
    }

    @PostMapping("/drivers/{driverId}/ride-complete")
    public void markDriverAvailableAfterTripCompletion(
            @PathVariable String driverId) {

        locationService.markDriverAvailableAfterTripCompletion(driverId);
    }
}

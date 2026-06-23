package com.rideshare.location_service.service;

import com.rideshare.location_service.dto.DriverLocationRequest;
import com.rideshare.location_service.dto.NearByDriverResponse;
import jakarta.websocket.PongMessage;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LocationService {
    private final RedisTemplate<String,String> redisTemplate;

    private static final String DRIVERS_GEO_KEY = "drivers:locations";

    public LocationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest) {
        log.info("updating location for driver with id: {}",driverLocationRequest.getDriverId());

        // Geo Spatial standard to have longitude first, then latitude
        Point driverPoint = new Point(driverLocationRequest.getLongitude(),driverLocationRequest.getLatitude());

        redisTemplate.opsForGeo().add(DRIVERS_GEO_KEY,driverPoint, driverLocationRequest.getDriverId());

        log.info("Location updated for driver id: {}",driverLocationRequest.getDriverId());



    }

    public @Nullable List<NearByDriverResponse> findNearByDrivers(double latitude, double longitude, double radius) {
        log.info("Finding drivers near by {} {} within {}",latitude,longitude,radius);

        Circle searchArea = new Circle(new Point(longitude,latitude),new Distance(radius, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().
                radius(DRIVERS_GEO_KEY,searchArea,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeCoordinates()
                                .includeDistance()
                                .sortAscending()
                                .limit(10));

        List<NearByDriverResponse> nearByDriverResponses = new ArrayList<>();
        if(results !=null){
            results.getContent().forEach(result->{
                RedisGeoCommands.GeoLocation<String> location = result.getContent();
                nearByDriverResponses.add(new NearByDriverResponse(location.getName(), location.getPoint().getX(),location.getPoint().getY(),result.getDistance().getValue()));


            });
        }

        log.info("Found {} drivers",nearByDriverResponses.size());
        return nearByDriverResponses;
    }

    public void removeDriverByID(String driverId) {
        log.info("removing driver with id: {}",driverId);

        redisTemplate.opsForGeo().remove(DRIVERS_GEO_KEY,driverId);
    }
}

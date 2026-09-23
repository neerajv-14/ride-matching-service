package com.rideshare.location_service.service;

import com.rideshare.location_service.dto.DriverLocationRequest;
import com.rideshare.location_service.dto.NearByDriverResponse;
import com.rideshare.location_service.model.DriverStatus;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LocationService {
    private final RedisTemplate<String,String> redisTemplate;

    private static final String DRIVERS_GEO_KEY = "drivers:locations";

    private static final String DRIVERS_STATUS_KEY = "drivers:status";

    public LocationService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void updateDriverLocation(DriverLocationRequest driverLocationRequest) {
        log.info("updating location for driver with id: {}",driverLocationRequest.getDriverId());

        // Geo Spatial standard to have longitude first, then latitude
        Point driverPoint = new Point(driverLocationRequest.getLongitude(),driverLocationRequest.getLatitude());

        redisTemplate.opsForGeo().add(DRIVERS_GEO_KEY,driverPoint, driverLocationRequest.getDriverId());

        redisTemplate.opsForHash()
                .putIfAbsent(
                        DRIVERS_STATUS_KEY,
                        driverLocationRequest.getDriverId(),
                        DriverStatus.AVAILABLE
                );

        log.info("Location updated for driver id: {}",driverLocationRequest.getDriverId());



    }

    public @Nullable List<NearByDriverResponse> findNearByDrivers(double latitude, double longitude, double radius) {
        log.info("Finding drivers near by {} {} within {}", latitude, longitude, radius);

        Circle searchArea = new Circle(new Point(longitude, latitude), new Distance(radius, Metrics.KILOMETERS));

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().
                radius(DRIVERS_GEO_KEY, searchArea,
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeCoordinates()
                                .includeDistance()
                                .sortAscending()
                                .limit(10));

        List<NearByDriverResponse> drivers = new ArrayList<>();

        if (results != null) {
            results.getContent().forEach(result -> {

                String driverId = result.getContent().getName();

                Object status = redisTemplate.opsForHash()
                        .get(DRIVERS_STATUS_KEY, driverId);

                if (DriverStatus.AVAILABLE.name().equals(status)) {

                    Point point = result.getContent().getPoint();

                    drivers.add(
                            new NearByDriverResponse(
                                    driverId,
                                    point.getX(),
                                    point.getY(),
                                    result.getDistance().getValue()
                            )
                    );
                }
            });

        }

        log.info("Found {} drivers", drivers.size());
        return drivers;

    }

    public boolean reserveDriver(String driverId, String rideId) {

        String luaScript = """
            local status = redis.call('HGET', KEYS[1], ARGV[1])

            if status == ARGV[2] then
                redis.call('HSET', KEYS[1], ARGV[1], ARGV[3] .. ':' .. ARGV[4])
                return 1
            end

            return 0
            """;

        RedisScript<Long> script =
                new DefaultRedisScript<>(luaScript, Long.class);

        Long result = redisTemplate.execute(
                script,
                List.of(DRIVERS_STATUS_KEY),
                driverId,
                DriverStatus.AVAILABLE.name(),
                DriverStatus.RESERVED.name(),
                rideId
        );

        if (Long.valueOf(1).equals(result)) {
            log.info("Driver {} reserved for ride {}", driverId, rideId);
            return true;
        }

        log.info("Driver {} is not available for ride {}", driverId, rideId);
        return false;
    }

    public void removeDriverByID(String driverId) {
        log.info("removing driver with id: {}", driverId);

        redisTemplate.opsForGeo()
                .remove(DRIVERS_GEO_KEY, driverId);

        redisTemplate.opsForHash()
                .delete(DRIVERS_STATUS_KEY, driverId);
    }

    public void markDriverOnTrip(String driverId, String rideId) {

        String currentStatus = (String) redisTemplate.opsForHash()
                .get(DRIVERS_STATUS_KEY, driverId);

        if ((DriverStatus.RESERVED.name() + ":" + rideId).equals(currentStatus)) {

            redisTemplate.opsForHash()
                    .put(DRIVERS_STATUS_KEY, driverId, "ON_TRIP");

            log.info("Driver {} is now on trip {}", driverId, rideId);
        }
    }

    public void markDriverAvailableAfterTripCompletion(String driverId) {

        redisTemplate.opsForHash()
                .put(
                        DRIVERS_STATUS_KEY,
                        driverId,
                        DriverStatus.AVAILABLE.name()
                );

        log.info("Driver {} is available again", driverId);
    }
}

package net.lausiv1024.realelevator.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.Nullable;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * ワールドごとに読み込まれているかごと乗場の扉の一覧。当たり判定と運搬で、近くのものを毎回エンティティ検索せずに引くために使う。
 */
public final class ElevatorTracker {
    private static final Map<Level, List<ElevatorCarEntity>> CARS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Level, List<LandingDoorEntity>> LANDING_DOORS = Collections.synchronizedMap(new WeakHashMap<>());

    private ElevatorTracker() {
    }

    public static List<ElevatorCarEntity> cars(Level level) {
        List<ElevatorCarEntity> cars = CARS.get(level);
        return cars != null ? cars : List.of();
    }

    public static List<LandingDoorEntity> landingDoors(Level level) {
        List<LandingDoorEntity> doors = LANDING_DOORS.get(level);
        return doors != null ? doors : List.of();
    }

    /** かごまたは乗場の扉かどうか。これらは衝突判定や運搬の対象にしない。 */
    public static boolean isElevatorPart(Entity entity) {
        return entity instanceof ElevatorCarEntity || entity instanceof LandingDoorEntity;
    }

    @Nullable
    public static ElevatorCarEntity findCar(Level level, UUID uuid) {
        for (ElevatorCarEntity car : cars(level)) {
            if (car.getUUID().equals(uuid) && !car.isRemoved()) {
                return car;
            }
        }
        return null;
    }

    static void add(ElevatorCarEntity car) {
        CARS.computeIfAbsent(car.level(), level -> new CopyOnWriteArrayList<>()).add(car);
    }

    static void remove(ElevatorCarEntity car) {
        List<ElevatorCarEntity> cars = CARS.get(car.level());
        if (cars != null) {
            cars.remove(car);
        }
    }

    static void add(LandingDoorEntity door) {
        LANDING_DOORS.computeIfAbsent(door.level(), level -> new CopyOnWriteArrayList<>()).add(door);
    }

    static void remove(LandingDoorEntity door) {
        List<LandingDoorEntity> doors = LANDING_DOORS.get(door.level());
        if (doors != null) {
            doors.remove(door);
        }
    }
}

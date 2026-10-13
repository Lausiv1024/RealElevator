package net.lausiv1024.realelevator.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import net.minecraft.world.level.Level;

/**
 * ワールドごとに読み込まれているかごの一覧。当たり判定と運搬で、近くのかごを毎回エンティティ検索せずに引くために使う。
 */
public final class CarTracker {
    private static final Map<Level, List<ElevatorCarEntity>> CARS = Collections.synchronizedMap(new WeakHashMap<>());

    private CarTracker() {
    }

    public static List<ElevatorCarEntity> cars(Level level) {
        List<ElevatorCarEntity> cars = CARS.get(level);
        return cars != null ? cars : List.of();
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
}

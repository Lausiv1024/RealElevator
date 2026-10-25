package net.lausiv1024.realelevator.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * かごの床・壁・天井・戸、釣合い重り、乗場の戸を、エンティティの衝突判定に加える。Mixinから呼ぶ。
 */
public final class CarCollisions {
    /** 段差の乗り越え判定は検索範囲より少し広い範囲を見るので、その分を含めて集める。 */
    private static final double SEARCH_MARGIN = 1.0;

    private CarCollisions() {
    }

    /**
     * @param entity 移動するエンティティ
     * @param area   バニラがエンティティの衝突を集める範囲
     * @param shapes バニラが集めたエンティティの衝突形状
     */
    public static List<VoxelShape> withCarShapes(Entity entity, AABB area, List<VoxelShape> shapes) {
        if (ElevatorTracker.isElevatorPart(entity)) {
            return shapes;
        }
        List<ElevatorCarEntity> cars = ElevatorTracker.cars(entity.level());
        List<LandingDoorEntity> landingDoors = ElevatorTracker.landingDoors(entity.level());
        if (cars.isEmpty() && landingDoors.isEmpty()) {
            return shapes;
        }

        AABB search = area.inflate(SEARCH_MARGIN);
        // かごとこのエンティティのどちらが先にtickしても同じ位置になるよう、時刻から求める
        double now = ElevatorClock.now(entity.level());
        List<VoxelShape> result = null;
        for (ElevatorCarEntity car : cars) {
            if (car.isRemoved()) {
                continue;
            }
            if (car.exteriorAt(car.yAt(now)).intersects(search)) {
                result = add(result, shapes, car.collisionBoxesAt(now));
            }
            AABB counterweight = car.counterweightAt(now);
            if (counterweight != null && counterweight.intersects(search)) {
                result = add(result, shapes, List.of(counterweight));
            }
        }
        for (LandingDoorEntity door : landingDoors) {
            if (!door.isRemoved() && door.getBoundingBox().intersects(search)) {
                result = add(result, shapes, door.collisionBoxesAt(now));
            }
        }
        return result != null ? result : shapes;
    }

    private static List<VoxelShape> add(List<VoxelShape> result, List<VoxelShape> original, List<AABB> boxes) {
        List<VoxelShape> list = result != null ? result : new ArrayList<>(original);
        for (AABB box : boxes) {
            list.add(Shapes.create(box));
        }
        return list;
    }
}

package net.lausiv1024.realelevator.entity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * かごの床・壁・天井を、エンティティの衝突判定に加える。Mixinから呼ぶ。
 */
public final class CarCollisions {
    /** 段差の乗り越え判定は検索範囲より少し広い範囲を見るので、その分を含めて集める。 */
    private static final double SEARCH_MARGIN = 1.0;

    private CarCollisions() {
    }

    /**
     * @param entity  移動するエンティティ
     * @param area    バニラがエンティティの衝突を集める範囲
     * @param shapes  バニラが集めたエンティティの衝突形状
     */
    public static List<VoxelShape> withCarShapes(Entity entity, AABB area, List<VoxelShape> shapes) {
        if (entity instanceof ElevatorCarEntity) {
            return shapes;
        }
        List<ElevatorCarEntity> cars = CarTracker.cars(entity.level());
        if (cars.isEmpty()) {
            return shapes;
        }

        AABB search = area.inflate(SEARCH_MARGIN);
        long now = entity.level().getGameTime();
        List<VoxelShape> result = null;
        for (ElevatorCarEntity car : cars) {
            if (car.isRemoved()) {
                continue;
            }
            // かごとこのエンティティのどちらが先にtickしても同じ位置になるよう、ゲーム時刻から求める
            double carY = car.yAt(now);
            if (!car.exteriorAt(carY).intersects(search)) {
                continue;
            }
            if (result == null) {
                result = new ArrayList<>(shapes);
            }
            for (AABB box : car.partBoxesAt(carY)) {
                result.add(Shapes.create(box));
            }
        }
        return result != null ? result : shapes;
    }
}

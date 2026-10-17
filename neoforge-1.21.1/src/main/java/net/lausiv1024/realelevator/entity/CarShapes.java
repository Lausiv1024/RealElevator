package net.lausiv1024.realelevator.entity;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * coreのかごと戸の形状を、向きに合わせて回す。動かない部分はキャッシュする。
 */
public final class CarShapes {
    private static final Map<CarModel, List<List<Part>>> PARTS = new EnumMap<>(CarModel.class);
    private static final Map<CarModel, List<Box>> EXTERIORS = new EnumMap<>(CarModel.class);

    static {
        for (CarModel model : CarModel.values()) {
            List<Part> parts = CarGeometry.parts(model);
            Box exterior = CarGeometry.exterior(model);
            PARTS.put(model, List.of(rotate(parts, 0), rotate(parts, 1), rotate(parts, 2), rotate(parts, 3)));
            EXTERIORS.put(model, List.of(exterior.rotateQuarterTurns(0), exterior.rotateQuarterTurns(1),
                    exterior.rotateQuarterTurns(2), exterior.rotateQuarterTurns(3)));
        }
    }

    private CarShapes() {
    }

    /** 正面を facing に向けたときの部位（ワールドの軸に平行、かごの原点からの相対座標）。 */
    public static List<Part> parts(CarModel model, Direction facing) {
        return PARTS.get(model).get(facing.get2DDataValue());
    }

    public static Box exterior(CarModel model, Direction facing) {
        return EXTERIORS.get(model).get(facing.get2DDataValue());
    }

    /** 開き具合 fraction での戸板（かごの原点からの相対座標）。 */
    public static List<Box> doorPanels(CarModel model, Direction facing, DoorGeometry.Side side, double fraction) {
        return DoorGeometry.panels(model, side, fraction).stream()
                .map(box -> box.rotateQuarterTurns(facing.get2DDataValue()))
                .toList();
    }

    /** 三方枠（かごの原点からの相対座標）。 */
    public static List<Box> landingFrame(CarModel model, Direction facing) {
        return DoorGeometry.landingFrame(model).stream()
                .map(box -> box.rotateQuarterTurns(facing.get2DDataValue()))
                .toList();
    }

    public static Box doorTravelBounds(CarModel model, Direction facing, DoorGeometry.Side side) {
        return DoorGeometry.travelBounds(model, side).rotateQuarterTurns(facing.get2DDataValue());
    }

    public static AABB toAabb(Box box, double x, double y, double z) {
        return new AABB(box.minX() + x, box.minY() + y, box.minZ() + z, box.maxX() + x, box.maxY() + y, box.maxZ() + z);
    }

    private static List<Part> rotate(List<Part> parts, int quarterTurns) {
        return parts.stream().map(p -> new Part(p.type(), p.box().rotateQuarterTurns(quarterTurns))).toList();
    }
}

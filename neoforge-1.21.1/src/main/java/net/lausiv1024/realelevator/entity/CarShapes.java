package net.lausiv1024.realelevator.entity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

/**
 * coreのかごと戸の形状を、向きに合わせて回す。動かない部分はキャッシュする。
 */
public final class CarShapes {
    private static final Map<CarSpec, List<List<Part>>> PARTS = new ConcurrentHashMap<>();
    private static final Map<CarModel, List<Box>> EXTERIORS = new ConcurrentHashMap<>();
    /** 同期データの1バイトのうち、機種の番号に使う下位のビット。上位のビットは扉の方式の番号。 */
    private static final int MODEL_BITS = 4;

    private CarShapes() {
    }

    /** かごの仕様を、同期データの1バイトにする。 */
    public static byte encode(CarSpec spec) {
        return (byte) (spec.model().ordinal() | spec.door().ordinal() << MODEL_BITS);
    }

    /** {@link #encode} の逆。範囲外の番号は、端の値にする。 */
    public static CarSpec decode(byte value) {
        CarModel[] models = CarModel.values();
        DoorType[] doors = DoorType.values();
        int bits = value & 0xFF;
        CarModel model = models[Math.min(bits & ((1 << MODEL_BITS) - 1), models.length - 1)];
        DoorType door = doors[Math.min(bits >> MODEL_BITS, doors.length - 1)];
        return new CarSpec(model, door);
    }

    /** 正面を facing に向けたときの部位（ワールドの軸に平行、かごの原点からの相対座標）。 */
    public static List<Part> parts(CarSpec spec, Direction facing) {
        return PARTS.computeIfAbsent(spec, key -> {
            List<Part> parts = CarGeometry.parts(key);
            return List.of(rotate(parts, 0), rotate(parts, 1), rotate(parts, 2), rotate(parts, 3));
        }).get(facing.get2DDataValue());
    }

    public static Box exterior(CarSpec spec, Direction facing) {
        return EXTERIORS.computeIfAbsent(spec.model(), key -> {
            Box exterior = CarGeometry.exterior(key);
            return List.of(exterior.rotateQuarterTurns(0), exterior.rotateQuarterTurns(1),
                    exterior.rotateQuarterTurns(2), exterior.rotateQuarterTurns(3));
        }).get(facing.get2DDataValue());
    }

    /** 開き具合 fraction での戸板（かごの原点からの相対座標）。 */
    public static List<Box> doorPanels(CarSpec spec, Direction facing, DoorGeometry.Side side, double fraction) {
        return DoorGeometry.panels(spec, side, fraction).stream()
                .map(box -> box.rotateQuarterTurns(facing.get2DDataValue()))
                .toList();
    }

    /** 三方枠（かごの原点からの相対座標）。 */
    public static List<Box> landingFrame(CarSpec spec, Direction facing) {
        return DoorGeometry.landingFrame(spec).stream()
                .map(box -> box.rotateQuarterTurns(facing.get2DDataValue()))
                .toList();
    }

    public static Box doorTravelBounds(CarSpec spec, Direction facing, DoorGeometry.Side side) {
        return DoorGeometry.travelBounds(spec, side).rotateQuarterTurns(facing.get2DDataValue());
    }

    public static AABB toAabb(Box box, double x, double y, double z) {
        return new AABB(box.minX() + x, box.minY() + y, box.minZ() + z, box.maxX() + x, box.maxY() + y, box.maxZ() + z);
    }

    private static List<Part> rotate(List<Part> parts, int quarterTurns) {
        return parts.stream().map(p -> new Part(p.type(), p.box().rotateQuarterTurns(quarterTurns))).toList();
    }
}

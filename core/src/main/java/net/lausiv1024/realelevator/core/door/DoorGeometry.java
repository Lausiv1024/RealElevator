package net.lausiv1024.realelevator.core.door;

import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;

/**
 * 中央開き（CO）の戸の形状。座標はかごのローカル座標（{@link CarGeometry} と同じ）で、乗場の戸も
 * かごがその階に着床したときの位置で表す。
 *
 * <p>かごの戸は正面の袖壁のすぐ外側、乗場の戸はさらにその外側に、敷居のすき間を空けて並ぶ。
 * 開くと、戸は出入口の両脇の袖壁（乗場では三方枠）の裏へ滑り込む。
 */
public final class DoorGeometry {
    /** 戸の厚み（実機 約30〜40 mm）。 */
    public static final int PANEL_THICKNESS_PX = 1;
    /** かごの敷居と乗場の敷居のすき間（実機 約30 mm）。 */
    public static final int SILL_GAP_PX = 1;

    public enum Side {
        CAR,
        LANDING
    }

    private DoorGeometry() {
    }

    /**
     * 開き具合 fraction（0 が全閉、1 が全開）での戸板。左右の2枚を返す。
     */
    public static List<Box> panels(CarModel model, Side side, double fraction) {
        double px = 1.0 / CarModel.PIXELS_PER_BLOCK;
        Box entrance = CarGeometry.entrance(model);
        double panelWidth = entrance.sizeX() / 2;
        double slide = Math.clamp(fraction, 0.0, 1.0) * panelWidth;

        // 袖壁の外面から外側へ並べる
        double minZ = entrance.maxZ() + (side == Side.LANDING ? (PANEL_THICKNESS_PX + SILL_GAP_PX) * px : 0);
        double maxZ = minZ + PANEL_THICKNESS_PX * px;

        return List.of(
                new Box(entrance.minX() - slide, 0, minZ, entrance.minX() + panelWidth - slide, entrance.maxY(), maxZ),
                new Box(entrance.maxX() - panelWidth + slide, 0, minZ, entrance.maxX() + slide, entrance.maxY(), maxZ));
    }

    /** 戸が全閉から全開まで動く範囲全体。 */
    public static Box travelBounds(CarModel model, Side side) {
        List<Box> closed = panels(model, side, 0);
        List<Box> open = panels(model, side, 1);
        return new Box(open.get(0).minX(), 0, closed.get(0).minZ(), open.get(1).maxX(), closed.get(0).maxY(), closed.get(0).maxZ());
    }
}

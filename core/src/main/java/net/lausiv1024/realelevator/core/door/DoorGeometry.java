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
 *
 * <p>乗場の壁は1ブロックの厚みの列で、乗場の戸はその列の昇降路側の面に付く。壁の列のうち、出入口のまわり
 * （幅3ブロック、高さ{@value #LANDING_FRAME_HEIGHT}ブロック）は三方枠で、出入口だけが開いている。
 * 乗場のボタン・階床表示器・ホールランタンは、三方枠の乗場側の面（{@link #landingFace}）に付ける。
 */
public final class DoorGeometry {
    /** 戸の厚み（実機 約30〜40 mm）。 */
    public static final int PANEL_THICKNESS_PX = 1;
    /** かごの敷居と乗場の敷居のすき間（実機 約30 mm）。 */
    public static final int SILL_GAP_PX = 1;
    /** 三方枠の幅の半分 [ブロック]。かごの中心の列と、その左右の列で3ブロック。 */
    public static final double LANDING_FRAME_HALF_WIDTH = 1.5;
    /** 三方枠の高さ [ブロック]。 */
    public static final int LANDING_FRAME_HEIGHT = 4;

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

    /**
     * 三方枠の脇（出入口の横の部分）の、幅の中央の X を2倍した値 [px]。right は乗場から見て右（+X）側の脇か。
     * 戸袋は三方枠の裏にあるので、乗場の器具は脇の面のどこにでも付けられる。2倍しているのは、幅が奇数でも整数で表すため。
     */
    public static int frameJambCenter2Px(CarModel model, boolean right) {
        int entranceEdge = (int) Math.round(CarGeometry.entrance(model).maxX() * CarModel.PIXELS_PER_BLOCK);
        int frameEdge = (int) Math.round(LANDING_FRAME_HALF_WIDTH * CarModel.PIXELS_PER_BLOCK);
        int center2 = entranceEdge + frameEdge;
        return right ? center2 : -center2;
    }

    /** 乗場の壁の列の、乗場側の面。乗場のボタン・表示器・ランタンを付ける面。 */
    public static double landingFace(CarModel model) {
        return travelBounds(model, Side.LANDING).minZ() + 1;
    }

    /**
     * 三方枠。乗場の壁の列のうち、乗場の戸の前から乗場側の面までを、出入口を残して埋める。
     * 戸は三方枠の裏（昇降路側）を滑るので、戸と三方枠は重ならない。
     */
    public static List<Box> landingFrame(CarModel model) {
        Box entrance = CarGeometry.entrance(model);
        double back = travelBounds(model, Side.LANDING).maxZ();
        double face = landingFace(model);
        double half = LANDING_FRAME_HALF_WIDTH;
        double top = LANDING_FRAME_HEIGHT;
        return List.of(
                new Box(-half, 0, back, entrance.minX(), top, face),
                new Box(entrance.maxX(), 0, back, half, top, face),
                new Box(entrance.minX(), entrance.maxY(), back, entrance.maxX(), top, face));
    }

    /** 戸が全閉から全開まで動く範囲全体。 */
    public static Box travelBounds(CarModel model, Side side) {
        List<Box> closed = panels(model, side, 0);
        List<Box> open = panels(model, side, 1);
        return new Box(open.get(0).minX(), 0, closed.get(0).minZ(), open.get(1).maxX(), closed.get(0).maxY(), closed.get(0).maxZ());
    }
}

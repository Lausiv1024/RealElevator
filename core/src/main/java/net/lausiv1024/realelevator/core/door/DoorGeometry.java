package net.lausiv1024.realelevator.core.door;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;

/**
 * 戸の形状。座標はかごのローカル座標（{@link CarGeometry} と同じ）で、乗場の戸も
 * かごがその階に着床したときの位置で表す。
 *
 * <p>かごの戸は正面の袖壁のすぐ外側、乗場の戸はさらにその外側に、敷居のすき間を空けて並ぶ。
 * 開くと、戸は出入口の脇の袖壁（乗場では三方枠）の裏へ滑り込む。
 *
 * <ul>
 * <li>CO：左右に1枚ずつ。それぞれ出入口の半分の幅で、両脇へ開く。</li>
 * <li>2S：2枚とも戸袋の側へ開く。戸は前後2本のレールに分かれ、出入口の奥の端を閉じる戸（速い戸）は出入口の幅だけ、
 * 戸袋の側の戸（遅い戸）は戸1枚の幅だけ動く。閉じたとき2枚は {@value CarSpec#SIDE_OVERLAP_PX}px 重なる。</li>
 * <li>4CO：左右に2枚ずつ。片側は2Sと同じで、中央の戸（速い戸）は出入口の半分の幅、脇の戸（遅い戸）は戸1枚の幅だけ動く。</li>
 * </ul>
 *
 * <p>レールが2本のとき、かごでは遅い戸が袖壁の側、速い戸がその外側、乗場では速い戸がかごの側、遅い戸が三方枠の側を通る
 * （遅い戸がどちらも戸袋のある壁に近い）。
 *
 * <p>乗場の壁は1ブロックの厚みの列で、乗場の戸はその列の昇降路側の面に付く。壁の列のうち、出入口のまわり
 * （昇降路と同じ幅3または4ブロック、高さ{@value #LANDING_FRAME_HEIGHT}ブロック）は三方枠で、出入口だけが開いている。
 * 乗場のボタン・階床表示器・ホールランタンは、三方枠の乗場側の面（{@link #landingFace}）に付ける。
 */
public final class DoorGeometry {
    /** 戸の厚み（実機 約30〜40 mm）。 */
    public static final int PANEL_THICKNESS_PX = 1;
    /** かごの敷居と乗場の敷居のすき間（実機 約30 mm）。 */
    public static final int SILL_GAP_PX = 1;
    /** 三方枠の高さ [ブロック]。 */
    public static final int LANDING_FRAME_HEIGHT = 4;

    public enum Side {
        CAR,
        LANDING
    }

    private DoorGeometry() {
    }

    /** 三方枠の幅の半分 [ブロック]。三方枠は昇降路と同じ幅で、かごの中心がその中央に来る。 */
    public static double landingFrameHalfWidth(CarModel model) {
        return model.shaftWidth() / 2.0;
    }

    public static double landingFrameHalfWidth(CarSpec spec) {
        return landingFrameHalfWidth(spec.model());
    }

    /** 中央開き（CO）の戸板。 */
    public static List<Box> panels(CarModel model, Side side, double fraction) {
        return panels(CarSpec.of(model), side, fraction);
    }

    /**
     * 開き具合 fraction（0 が全閉、1 が全開）での戸板。CO は左、右の順、2S は遅い戸、速い戸の順、
     * 4CO は左の遅い戸、左の速い戸、右の速い戸、右の遅い戸の順。
     */
    public static List<Box> panels(CarSpec spec, Side side, double fraction) {
        double px = 1.0 / CarModel.PIXELS_PER_BLOCK;
        Box entrance = CarGeometry.entrance(spec);
        double f = Math.clamp(fraction, 0.0, 1.0);
        double panel = spec.panelWidthPx() * px;
        double top = entrance.maxY();
        double thick = PANEL_THICKNESS_PX * px;

        // 袖壁の外面から外側へ、レールを並べる。first がかごの袖壁に近い側
        int tracks = spec.door().tracks();
        double first = entrance.maxZ() + (side == Side.LANDING ? (tracks * PANEL_THICKNESS_PX + SILL_GAP_PX) * px : 0);
        double last = first + (tracks - 1) * thick;
        // 遅い戸は、戸袋のある壁（かごでは袖壁、乗場では三方枠）に近いレールを通る
        double slowZ = side == Side.CAR ? first : last;
        double fastZ = side == Side.CAR ? last : first;

        List<Box> boxes = new ArrayList<>(4);
        switch (spec.door()) {
            case CENTER -> {
                double slide = f * panel;
                boxes.add(new Box(entrance.minX() - slide, 0, slowZ, entrance.minX() + panel - slide, top, slowZ + thick));
                boxes.add(new Box(entrance.maxX() - panel + slide, 0, slowZ, entrance.maxX() + slide, top, slowZ + thick));
            }
            case SIDE_LEFT -> {
                // 戸袋は −X 側。遅い戸は出入口の左端を、速い戸は右端を閉じる
                double slowSlide = f * panel;
                double fastSlide = f * entrance.sizeX();
                boxes.add(new Box(entrance.minX() - slowSlide, 0, slowZ, entrance.minX() + panel - slowSlide, top, slowZ + thick));
                boxes.add(new Box(entrance.maxX() - panel - fastSlide, 0, fastZ, entrance.maxX() - fastSlide, top, fastZ + thick));
            }
            case SIDE_RIGHT -> {
                double slowSlide = f * panel;
                double fastSlide = f * entrance.sizeX();
                boxes.add(new Box(entrance.maxX() - panel + slowSlide, 0, slowZ, entrance.maxX() + slowSlide, top, slowZ + thick));
                boxes.add(new Box(entrance.minX() + fastSlide, 0, fastZ, entrance.minX() + panel + fastSlide, top, fastZ + thick));
            }
            case FOUR_CENTER -> {
                double centre = (entrance.minX() + entrance.maxX()) / 2;
                double slowSlide = f * panel;
                double fastSlide = f * entrance.sizeX() / 2;
                boxes.add(new Box(entrance.minX() - slowSlide, 0, slowZ, entrance.minX() + panel - slowSlide, top, slowZ + thick));
                boxes.add(new Box(centre - panel - fastSlide, 0, fastZ, centre - fastSlide, top, fastZ + thick));
                boxes.add(new Box(centre + fastSlide, 0, fastZ, centre + panel + fastSlide, top, fastZ + thick));
                boxes.add(new Box(entrance.maxX() - panel + slowSlide, 0, slowZ, entrance.maxX() + slowSlide, top, slowZ + thick));
            }
        }
        return boxes;
    }

    /**
     * 三方枠の脇（出入口の横の部分）の、幅の中央の X を2倍した値 [px]。right は乗場から見て右（+X）側の脇か。
     * 戸袋は三方枠の裏にあるので、乗場の器具は脇の面のどこにでも付けられる。2倍しているのは、幅が奇数でも整数で表すため。
     */
    public static int frameJambCenter2Px(CarModel model, boolean right) {
        return frameJambCenter2Px(CarSpec.of(model), right);
    }

    public static int frameJambCenter2Px(CarSpec spec, boolean right) {
        int frameEdge = (int) Math.round(landingFrameHalfWidth(spec) * CarModel.PIXELS_PER_BLOCK);
        return right ? spec.entranceMaxXPx() + frameEdge : spec.entranceMinXPx() - frameEdge;
    }

    /** 乗場の壁の列の、乗場側の面。乗場のボタン・表示器・ランタンを付ける面。 */
    public static double landingFace(CarModel model) {
        return landingFace(CarSpec.of(model));
    }

    public static double landingFace(CarSpec spec) {
        return travelBounds(spec, Side.LANDING).minZ() + 1;
    }

    /**
     * 乗場の面を faceOffset だけ乗場側へずらしたもの。二方向出入口の奥の乗場では、乗場の戸がブロックの格子にそろわないので、
     * 三方枠を厚くして次のブロックの面まで埋める。
     */
    public static double landingFace(CarSpec spec, double faceOffset) {
        return landingFace(spec) + faceOffset;
    }

    /** 中央開き（CO）の三方枠。 */
    public static List<Box> landingFrame(CarModel model) {
        return landingFrame(CarSpec.of(model));
    }

    /**
     * 三方枠。乗場の壁の列のうち、乗場の戸の前から乗場側の面までを、出入口を残して埋める。
     * 戸は三方枠の裏（昇降路側）を滑るので、戸と三方枠は重ならない。
     */
    public static List<Box> landingFrame(CarSpec spec) {
        return landingFrame(spec, 0);
    }

    /** 乗場の面を faceOffset だけずらした三方枠（{@link #landingFace(CarSpec, double)}）。 */
    public static List<Box> landingFrame(CarSpec spec, double faceOffset) {
        Box entrance = CarGeometry.entrance(spec);
        double back = travelBounds(spec, Side.LANDING).maxZ();
        double face = landingFace(spec, faceOffset);
        double half = landingFrameHalfWidth(spec);
        double top = LANDING_FRAME_HEIGHT;
        return List.of(
                new Box(-half, 0, back, entrance.minX(), top, face),
                new Box(entrance.maxX(), 0, back, half, top, face),
                new Box(entrance.minX(), entrance.maxY(), back, entrance.maxX(), top, face));
    }

    /**
     * 多光軸ビームセンサーの検知領域。かごの戸（いちばん外側のレール）の乗場側の面から乗場の戸の面までの、敷居のすき間の薄い板で、
     * 出入口の幅と全高にわたる。閉動作中にここへ生き物が入ったら、戸に触れる前でも開き直す。
     */
    public static Box beamZone(CarSpec spec) {
        double px = 1.0 / CarModel.PIXELS_PER_BLOCK;
        Box entrance = CarGeometry.entrance(spec);
        double carDoorFace = entrance.maxZ() + spec.door().tracks() * PANEL_THICKNESS_PX * px;
        return new Box(entrance.minX(), 0, carDoorFace, entrance.maxX(), entrance.maxY(), carDoorFace + SILL_GAP_PX * px);
    }

    /** 中央開き（CO）の戸が動く範囲。 */
    public static Box travelBounds(CarModel model, Side side) {
        return travelBounds(CarSpec.of(model), side);
    }

    /** 戸が全閉から全開まで動く範囲全体（すべての戸とレールを含む）。 */
    public static Box travelBounds(CarSpec spec, Side side) {
        Box bounds = null;
        for (double fraction : new double[] {0, 1}) {
            for (Box panel : panels(spec, side, fraction)) {
                bounds = bounds == null ? panel : new Box(
                        Math.min(bounds.minX(), panel.minX()), Math.min(bounds.minY(), panel.minY()), Math.min(bounds.minZ(), panel.minZ()),
                        Math.max(bounds.maxX(), panel.maxX()), Math.max(bounds.maxY(), panel.maxY()), Math.max(bounds.maxZ(), panel.maxZ()));
            }
        }
        return bounds;
    }
}

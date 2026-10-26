package net.lausiv1024.realelevator.core.hoist;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Axis;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Point;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Sheave;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;

/**
 * かごの外側の機器：かご枠（たて枠・上はり・下はり）、ローラーガイド、非常止め、ドアオペレーター、エプロン、かご上の安全柵。
 * 座標はかごのローカル座標で、Y はかごの床面からの高さ（描画では、かごの位置だけ上下に動かす）。
 *
 * <ul>
 * <li>たて枠・上はり・下はりは、ガイドレールと同じ奥行きの位置に幅3pxで通す。かご下のシーブの金具とロープは、レールから奥行きの
 * 向きに離れているので、ぶつからない。たて枠は、かごの外壁とレールの先端のすき間が1px以上あればその中に1pxの柱として、
 * なければ（P-13・P-15）外壁の面に埋めて描く。</li>
 * <li>ローラーガイド：上はりの上と下はりの下の、レールの端に、台の板と3つのローラー（半径{@value #ROLLER_RADIUS_PX}px）。
 * 左右の2つはレールの刃の両面に、残り1つは刃の先端に当て、かごの位置から回転角を求める。</li>
 * <li>非常止め：下のローラーガイドのさらに下に、レールの刃を両側から挟む形で付ける。</li>
 * <li>ドアオペレーター：屋根の上の出入口の側に、戸の動く範囲いっぱいの箱と、端のモーター。箱の前の面のベルトに、戸ごとの吊り金具が
 * 戸と一緒に動く（ベルトの端で止まる）。</li>
 * <li>エプロン：出入口の下に、かごの戸の外側の面に沿って垂れる板。長さは{@value #APRON_PX}pxで、ピットに収まらなければ短くする。</li>
 * <li>安全柵：屋根の縁の、出入口の側を除く辺（二方向出入口では左右だけ）。高さは{@value #FENCE_PX}pxで、頂部に収まらなければ低くする。
 * 左右の辺は、ローラーガイドと上はりのところで切る。当たり判定を持つ。</li>
 * </ul>
 */
public final class CarFrame {
    public static final int BEAM_WIDTH_PX = 3;
    public static final int BEAM_HEIGHT_PX = 3;
    public static final double ROLLER_RADIUS_PX = 1;
    public static final int APRON_PX = 18;
    public static final int FENCE_PX = 11;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;
    private static final double LIFT = 0.002;
    /** 屋根の上面の高さ [px]。 */
    private static final int ROOF_PX = CarModel.CEILING_HEIGHT_PX + CarGeometry.CEILING_PX;
    /** ローラーガイドの台の板の厚み [px]。 */
    private static final int PLATE_PX = 1;
    /** 安全柵を、ローラーガイドの前後で切る幅の半分 [px]。 */
    private static final double FENCE_CUT_PX = 3;
    /** 安全柵の柱の間隔の上限 [px]。 */
    private static final int FENCE_POST_SPACING_PX = 8;
    /** ドアオペレーターの箱の奥行きと高さ [px]。 */
    private static final int OPERATOR_DEPTH_PX = 4;
    private static final int OPERATOR_HEIGHT_PX = 4;
    private static final int MOTOR_PX = 3;

    public enum Type {
        /** たて枠・上はり・下はり（暗い塗装色）。 */
        FRAME,
        /** ローラーガイドの台の板。 */
        GUIDE_PLATE,
        /** 非常止め。 */
        SAFETY_GEAR,
        /** ドアオペレーターの箱。 */
        OPERATOR,
        /** ドアオペレーターのモーター。 */
        MOTOR,
        /** ドアオペレーターのベルト。 */
        BELT,
        /** エプロン（亜鉛メッキ調）。 */
        APRON,
        /** 安全柵（黄色の塗装）。 */
        FENCE
    }

    public record Part(Type type, Box box) {
    }

    private final CarSpec spec;
    private final boolean through;
    /** 昇降路の幅の半分 [px]。 */
    private final double half;
    /** かごの外壁の X の半分 [px]。 */
    private final double outerX;
    /** かごの外壁の Z の半分 [px]（正面の外面）。 */
    private final double outerZ;
    /** ガイドレールの中心の Z [px]。 */
    private final double railZ;
    private final double apronPx;
    private final double fencePx;
    private final List<Part> parts;

    /**
     * @param railZ  ガイドレールの中心の Z [ブロック]（かごのローカル座標）
     * @param half   昇降路の幅の半分 [ブロック]
     */
    CarFrame(CarSpec spec, PitMode pitMode, boolean through, double railZ, double half) {
        this.spec = spec;
        this.through = through;
        this.half = half / PX;
        Box exterior = CarGeometry.exterior(spec);
        this.outerX = exterior.maxX() / PX;
        this.outerZ = exterior.maxZ() / PX;
        this.railZ = railZ / PX;
        // ピットの底・頂部の天井に、1pxの余裕を残して収める
        this.apronPx = Math.min(APRON_PX, pitMode.pit * CarModel.PIXELS_PER_BLOCK - CarGeometry.FLOOR_PX - 1);
        this.fencePx = Math.min(FENCE_PX, pitMode.overhead * CarModel.PIXELS_PER_BLOCK - ROOF_PX - 1);
        this.parts = List.copyOf(buildParts());
    }

    /** 動かない部分（ドアオペレーターの吊り金具とローラーは別）。 */
    public List<Part> parts() {
        return parts;
    }

    /** 安全柵の箱。当たり判定に使う。 */
    public List<Box> fence() {
        return parts.stream().filter(p -> p.type() == Type.FENCE).map(Part::box).toList();
    }

    private List<Part> buildParts() {
        List<Part> list = new ArrayList<>();
        double z0 = railZ - BEAM_WIDTH_PX / 2.0;
        double z1 = railZ + BEAM_WIDTH_PX / 2.0;
        double beamBottom = -CarGeometry.FLOOR_PX - BEAM_HEIGHT_PX;
        double crossheadTop = ROOF_PX + BEAM_HEIGHT_PX;
        list.add(new Part(Type.FRAME, box(-outerX, ROOF_PX, z0, outerX, crossheadTop, z1)));
        list.add(new Part(Type.FRAME, box(-outerX, beamBottom, z0, outerX, -CarGeometry.FLOOR_PX, z1)));
        for (int side = -1; side <= 1; side += 2) {
            // たて枠：すき間がなければ外壁の面に埋める
            boolean outside = half - ShaftLayout.RAIL_WIDTH_PX - outerX >= 1;
            // 埋めるときは外壁の外側の半分だけにする（内側の面と重ねると、かごの中からちらついて見える）
            double inner = outside ? outerX : outerX - 0.5;
            double outer = outside ? outerX + 1 : outerX + LIFT / PX;
            list.add(new Part(Type.FRAME, mirrored(side, inner, beamBottom, outer, crossheadTop, z0, z1)));
            // ローラーガイドの台の板：レールの刃が通るところは切り欠く（刃の先端までの本体と、刃の両側に伸びる2本の腕）
            double bladeTip = half - ShaftLayout.RAIL_WIDTH_PX;
            for (double y : new double[] {crossheadTop, beamBottom - PLATE_PX}) {
                list.add(new Part(Type.GUIDE_PLATE, mirrored(side, inner, y, bladeTip, y + PLATE_PX, z0, z1)));
                list.add(new Part(Type.GUIDE_PLATE, mirrored(side, bladeTip, y, half - 1, y + PLATE_PX, railZ + 0.5, z1)));
                list.add(new Part(Type.GUIDE_PLATE, mirrored(side, bladeTip, y, half - 1, y + PLATE_PX, z0, railZ - 0.5)));
            }
            // 非常止め：刃の両側の顎と、それをつなぐ背
            double gearTop = beamBottom - PLATE_PX - 2 * ROLLER_RADIUS_PX - 1;
            double gearBottom = gearTop - 3;
            list.add(new Part(Type.SAFETY_GEAR, mirrored(side, bladeTip, gearBottom, half - 1, gearTop, railZ + 0.5, z1)));
            list.add(new Part(Type.SAFETY_GEAR, mirrored(side, bladeTip, gearBottom, half - 1, gearTop, z0, railZ - 0.5)));
            list.add(new Part(Type.SAFETY_GEAR, mirrored(side, bladeTip - 2, gearBottom, bladeTip, gearTop, z0, z1)));
        }
        addOperator(list, Entrance.FRONT);
        addApron(list, Entrance.FRONT);
        if (through) {
            addOperator(list, Entrance.REAR);
            addApron(list, Entrance.REAR);
        }
        addFence(list);
        return list;
    }

    /**
     * ドアオペレーターの左右の範囲 [px]：戸の動く範囲いっぱい（ただし安全柵にかからないよう、外壁の1px内側まで）。
     * 右の端の{@value #MOTOR_PX}pxにモーターを置き、残りが箱とベルト。
     */
    private double operatorLeft() {
        return Math.max(DoorGeometry.travelBounds(spec, DoorGeometry.Side.CAR).minX() / PX, -(outerX - 1));
    }

    private double operatorRight() {
        return Math.min(DoorGeometry.travelBounds(spec, DoorGeometry.Side.CAR).maxX() / PX, outerX - 1);
    }

    private void addOperator(List<Part> list, Entrance entrance) {
        double front = outerZ - 1;
        // 上はりが出入口の近くを通る機種（P-6 など）では、箱を上はりの手前で止める
        double crossheadFront = entrance == Entrance.FRONT ? railZ + BEAM_WIDTH_PX / 2.0 : -(railZ - BEAM_WIDTH_PX / 2.0);
        double back = Math.max(front - OPERATOR_DEPTH_PX, crossheadFront);
        double top = ROOF_PX + OPERATOR_HEIGHT_PX;
        double left = operatorLeft();
        double motor = operatorRight() - MOTOR_PX;
        list.add(new Part(Type.OPERATOR, rotate(box(left, ROOF_PX, back, motor, top, front), entrance)));
        list.add(new Part(Type.MOTOR, rotate(box(motor, ROOF_PX, Math.max(front - MOTOR_PX, back), operatorRight(), ROOF_PX + MOTOR_PX, front),
                entrance)));
        list.add(new Part(Type.BELT, rotate(box(left + 1, ROOF_PX + 1.5, front, motor - 1, ROOF_PX + 2.5, front + 0.25), entrance)));
    }

    private void addApron(List<Part> list, Entrance entrance) {
        double doorOuter = DoorGeometry.travelBounds(spec, DoorGeometry.Side.CAR).maxZ() / PX;
        double top = -CarGeometry.FLOOR_PX;
        list.add(new Part(Type.APRON, rotate(box(spec.entranceMinXPx(), top - apronPx, doorOuter - 1, spec.entranceMaxXPx(), top,
                doorOuter), entrance)));
    }

    private void addFence(List<Part> list) {
        if (fencePx < 2) {
            return;
        }
        double bottom = ROOF_PX;
        double top = ROOF_PX + fencePx;
        double middle = ROOF_PX + Math.floor(fencePx / 2);
        double back = -outerZ;
        double front = outerZ;
        // 左右の辺：ローラーガイドと上はりの前後で切った2つの区間
        for (int side = -1; side <= 1; side += 2) {
            double x0 = side < 0 ? -outerX : outerX - 1;
            double cutBack = railZ - FENCE_CUT_PX;
            double cutFront = railZ + FENCE_CUT_PX;
            addRun(list, x0, x0 + 1, cutFront, front, bottom, middle, top, true);
            addRun(list, x0, x0 + 1, back + (through ? 0 : 1), cutBack, bottom, middle, top, true);
        }
        if (!through) {
            addRun(list, -outerX, outerX, back, back + 1, bottom, middle, top, false);
        }
    }

    /**
     * 安全柵の1区間。alongZ なら Z の a〜b（a &lt; b。逆なら区間がないので置かない）、そうでなければ X の x0〜x1 に沿い、Z は a〜b。
     * 両端と、{@value #FENCE_POST_SPACING_PX}px 以内ごとに柱、上と中ほどに横棒。
     */
    private void addRun(List<Part> list, double x0, double x1, double a, double b, double bottom, double middle, double top,
                        boolean alongZ) {
        double from = alongZ ? a : x0;
        double to = alongZ ? b : x1;
        if (to - from < 1) {
            return;
        }
        double zFrom = alongZ ? from : a;
        double zTo = alongZ ? to : b;
        int posts = Math.max(1, (int) Math.ceil((to - from - 1) / FENCE_POST_SPACING_PX));
        for (int i = 0; i <= posts; i++) {
            double at = from + (to - from - 1) * i / posts;
            Box post = alongZ ? box(x0, bottom, at, x1, top - 1, at + 1) : box(at, bottom, zFrom, at + 1, top - 1, zTo);
            list.add(new Part(Type.FENCE, post));
        }
        // 横棒は柱の間（柱と重ならない）
        for (int i = 0; i < posts; i++) {
            double s = from + (to - from - 1) * i / posts + 1;
            double e = from + (to - from - 1) * (i + 1) / posts;
            if (e - s <= 0) {
                continue;
            }
            double y = middle - 0.5;
            list.add(new Part(Type.FENCE, alongZ ? box(x0, y, s, x1, y + 1, e) : box(s, y, zFrom, e, y + 1, zTo)));
        }
        list.add(new Part(Type.FENCE, alongZ ? box(x0, top - 1, from, x1, top, to) : box(from, top - 1, zFrom, to, top, zTo)));
    }

    /**
     * かごが carY にあるときのローラー（左右・上下のローラーガイドに3つずつ）。回転角は、かごの位置をローラーの半径で割ったもの。
     */
    public List<Sheave> rollers(double carY) {
        List<Sheave> rollers = new ArrayList<>();
        double r = ROLLER_RADIUS_PX * PX;
        double width = PX;
        double angle = carY / r;
        double crossheadTop = ROOF_PX + BEAM_HEIGHT_PX;
        double beamBottom = -CarGeometry.FLOOR_PX - BEAM_HEIGHT_PX;
        for (double y : new double[] {crossheadTop + PLATE_PX + ROLLER_RADIUS_PX, beamBottom - PLATE_PX - ROLLER_RADIUS_PX}) {
            for (int side = -1; side <= 1; side += 2) {
                double sideX = side * (half - 2.5) * PX;
                double tipX = side * (half - ShaftLayout.RAIL_WIDTH_PX - ROLLER_RADIUS_PX) * PX;
                double cy = carY + y * PX;
                double offset = (0.5 + ROLLER_RADIUS_PX) * PX;
                rollers.add(new Sheave(new Point(sideX, cy, railZ * PX - offset), r, width, Axis.X, angle));
                rollers.add(new Sheave(new Point(sideX, cy, railZ * PX + offset), r, width, Axis.X, -angle));
                rollers.add(new Sheave(new Point(tipX, cy, railZ * PX), r, width, Axis.Z, side * angle));
            }
        }
        return rollers;
    }

    /** 出入口 entrance のドアオペレーターの吊り金具。戸の開き具合 fraction の戸板ごとに、戸板の幅の中央に付く。 */
    public List<Box> hangers(double fraction, Entrance entrance) {
        double front = outerZ - 1;
        List<Box> boxes = new ArrayList<>();
        for (Box panel : DoorGeometry.panels(spec, DoorGeometry.Side.CAR, fraction)) {
            // 吊り金具はベルトの上を動き、ベルトの端で止まる
            double centre = Math.clamp((panel.minX() + panel.maxX()) / 2 / PX, operatorLeft() + 2, operatorRight() - MOTOR_PX - 2);
            boxes.add(rotate(box(centre - 1, ROOF_PX + 1, front, centre + 1, ROOF_PX + 3, front + 0.5), entrance));
        }
        return boxes;
    }

    /** px の座標の箱（右側 side=1 の形を、side=−1 なら左右に映す）。 */
    private static Box mirrored(int side, double x0, double y0, double x1, double y1, double z0, double z1) {
        return side > 0 ? box(x0, y0, z0, x1, y1, z1) : box(-x1, y0, z0, -x0, y1, z1);
    }

    private static Box rotate(Box box, Entrance entrance) {
        return box.rotateQuarterTurns(entrance.quarterTurns());
    }

    private static Box box(double x0, double y0, double z0, double x1, double y1, double z1) {
        return new Box(Math.min(x0, x1) * PX, Math.min(y0, y1) * PX, Math.min(z0, z1) * PX,
                Math.max(x0, x1) * PX, Math.max(y0, y1) * PX, Math.max(z0, z1) * PX);
    }
}

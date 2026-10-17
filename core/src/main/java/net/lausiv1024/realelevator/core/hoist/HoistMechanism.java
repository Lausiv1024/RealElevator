package net.lausiv1024.realelevator.core.hoist;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;

/**
 * 昇降路の機構（機械室レス、2:1ローピング、釣合い重りはかごの奥）。かごの床面の高さから、釣合い重りの位置、
 * 各シーブの回転角、ロープの経路を求める。
 *
 * <p>座標は、水平方向はかごのローカル座標（原点はかごの床面の中心、+Z が出入口の向き）、高さはワールドのY座標。単位はブロック。
 *
 * <p>ロープの経路（乗場から見て）：左上の固定端 → かごの左側のすき間を下りる → かご下の2つのシーブで左から右へくぐる →
 * かごの右側のすき間を上る → 頂部のそらせ車で奥へ向きを変える → 頂部の奥右にある巻上機の綱車 → 奥の釣合い重りの上のシーブ →
 * 頂部の固定端。かご側のロープとシーブは、かごが傾かないよう奥行きの中央に置く。ただし、ガイドレールが中央の近くにある機種では、
 * レールに当たらないよう、中央からレールと反対の向きへ少しずらす。
 * 巻上機の周速はかごの速度の2倍、釣合い重りはかごと逆向きに同じ速さで動く。
 */
public final class HoistMechanism {
    /** ロープの本数。 */
    public static final int ROPE_COUNT = 3;
    /** ロープの間隔の最大値 [px]。シーブの軸の方向に並べる。かごの横のすき間が狭い機種では詰める。 */
    private static final double MAX_ROPE_SPACING_PX = 2;
    /** ロープの太さ [px]。 */
    public static final double ROPE_WIDTH_PX = 1;

    /** かご下のシーブの、ロープが通る半径 [px]。 */
    private static final double CAR_SHEAVE_RADIUS_PX = 4;
    /** 頂部のそらせ車の、ロープが通る半径 [px]。巻上機の綱車と同じ大きさ。かごの横のすき間の上にあるので、かごには当たらない。 */
    private static final double DEFLECTOR_RADIUS_PX = 5;
    /** 巻上機の綱車の、ロープが通る半径 [px]。 */
    private static final double MACHINE_SHEAVE_RADIUS_PX = 5;
    /** 釣合い重りのシーブの、ロープが通る半径 [px]。 */
    private static final double COUNTERWEIGHT_SHEAVE_RADIUS_PX = 6;
    /** シーブの幅（軸の方向）の最大値 [px]。かごの横のすき間が狭い機種では、すき間の幅にする。 */
    private static final double MAX_SHEAVE_WIDTH_PX = 6;
    /** かご下のシーブの中心の、かごの床面からの高さ [px]。床の下面（−2px）から1px空ける。 */
    private static final double CAR_SHEAVE_Y_PX = -CarGeometry.FLOOR_PX - 1 - CAR_SHEAVE_RADIUS_PX;
    /** 頂部の水平なロープの、昇降路の天井からの深さ [px]。 */
    private static final double TOP_ROPE_DEPTH_PX = 1;
    /** 巻上機のモーターの、綱車の内側の長さ [px]と、軸からの半径 [px]。 */
    private static final double MOTOR_LENGTH_PX = 6;
    private static final double MOTOR_RADIUS_PX = 4;

    /** 釣合い重りの幅・厚み・高さ [px]。 */
    private static final double COUNTERWEIGHT_WIDTH_PX = 14;
    private static final double COUNTERWEIGHT_THICKNESS_PX = 6;
    private static final double COUNTERWEIGHT_HEIGHT_PX = 40;
    /** 釣合い重りのシーブの中心の、重りの上面からの高さ [px]。 */
    private static final double COUNTERWEIGHT_SHEAVE_RISE_PX = 7;
    /** かごが最上階にいるときの、釣合い重りの下面とピットの底のすき間（ランバイ）[px]。 */
    private static final double COUNTERWEIGHT_RUNBY_PX = 8;
    /** 釣合い重りのレールの断面 [px]。重りの奥に、両端に1本ずつ置く。 */
    private static final double COUNTERWEIGHT_RAIL_WIDTH_PX = 2;
    private static final double COUNTERWEIGHT_RAIL_DEPTH_PX = 2;
    /** ロープの固定端の金具の、天井からの深さ [px]。 */
    private static final double HITCH_DEPTH_PX = 3;

    /** 円弧を折れ線にするときの、90°あたりの分割数。 */
    private static final int ARC_STEPS_PER_QUARTER = 4;

    public enum Axis {
        X,
        Z
    }

    public record Point(double x, double y, double z) {
    }

    /**
     * シーブ。radius は描く半径（ロープの中心が通る半径から、ロープの太さの半分を引いたもの）。
     * angle は axis の正の向きを親指にした右ねじの向きの回転角 [rad]。
     */
    public record Sheave(Point centre, double radius, double width, Axis axis, double angle) {
    }

    private final CarModel model;
    private final PitMode pitMode;
    private final int bottomFloor;
    private final int topFloor;

    /** 昇降路の幅の半分 [px]。 */
    private final double shaftHalfWidthPx;
    /** かごの左右のすき間の中央の X の絶対値。かご側のロープ、そらせ車、巻上機の綱車がここに並ぶ。 */
    private final double ropeX;
    /** かご側のロープ・シーブ・固定端の Z。かごの奥行きの中央か、ガイドレールを避けてずらした位置。 */
    private final double carRopeZ;
    /** ロープの間隔とシーブの幅。 */
    private final double ropeSpacing;
    private final double sheaveWidth;
    /** 巻上機の綱車の中心の Z。綱車の手前の端がかごの奥の面より1px奥になるよう置く。 */
    private final double machineZ;
    private final double counterweightZ;
    private final double counterweightSheaveX;
    private final double backWallZ;

    /**
     * @param floors 停止階の床面のY座標（ワールド座標）
     */
    public HoistMechanism(CarModel model, PitMode pitMode, int[] floors) {
        this(CarSpec.of(model), pitMode, floors);
    }

    /** 扉の方式で、乗場の戸の奥行き（かごの位置）とガイドレールの位置が変わる。 */
    public HoistMechanism(CarSpec spec, PitMode pitMode, int[] floors) {
        CarModel model = spec.model();
        if (floors.length < 2) {
            throw new IllegalArgumentException("at least two floors are needed");
        }
        this.model = model;
        this.pitMode = pitMode;
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (int floor : floors) {
            min = Math.min(min, floor);
            max = Math.max(max, floor);
        }
        this.bottomFloor = min;
        this.topFloor = max;

        Box exterior = CarGeometry.exterior(model);
        double halfWidthPx = exterior.maxX() * CarModel.PIXELS_PER_BLOCK;
        double carBackPx = exterior.minZ() * CarModel.PIXELS_PER_BLOCK;
        this.shaftHalfWidthPx = model.shaftWidth() * CarModel.PIXELS_PER_BLOCK / 2.0;
        double gapPx = shaftHalfWidthPx - halfWidthPx;
        // ロープはすき間の中央（1pxの格子にそろえる）
        this.ropeX = px(halfWidthPx + Math.floor(gapPx / 2));
        // 軸が X のシーブ（そらせ車・綱車）はかごの横のすき間の上にあるので、ロープもシーブもすき間に収める
        this.ropeSpacing = px(Math.min(MAX_ROPE_SPACING_PX, (gapPx - ROPE_WIDTH_PX) / (ROPE_COUNT - 1)));
        this.sheaveWidth = px(Math.min(MAX_SHEAVE_WIDTH_PX, gapPx));
        this.machineZ = px(carBackPx - 1 - MACHINE_SHEAVE_RADIUS_PX);
        this.counterweightZ = machineZ - px(MACHINE_SHEAVE_RADIUS_PX);
        this.counterweightSheaveX = ropeX - px(COUNTERWEIGHT_SHEAVE_RADIUS_PX);
        ShaftLayout layout = new ShaftLayout(spec, pitMode, new int[] {0, ShaftLayout.MIN_FLOOR_SPACING});
        this.backWallZ = layout.backRow() - 0.5 - layout.carZ();
        this.carRopeZ = carRopeZ(layout.railRowZ() - layout.carZ());
    }

    /** ガイドレールの奥行きの半分と、かご側のロープ・シーブ・金具のうち一番太いものの半分 [px]。この和だけレールから離す。 */
    private double carRopeZ(double railZ) {
        double band = ropeSpacing * (ROPE_COUNT - 1) / 2 + px(ROPE_WIDTH_PX) / 2;
        double clearance = px(ShaftLayout.RAIL_DEPTH_PX) / 2
                + Math.max(band, Math.max(sheaveWidth / 2 + px(1), ropeSpacing + px(1)));
        if (Math.abs(railZ) >= clearance - 1e-9) {
            return 0;
        }
        // 中央に近いほうへ、レールの前か後ろに置く
        return railZ >= 0 ? railZ - clearance : railZ + clearance;
    }

    private static double px(double pixels) {
        return pixels / CarModel.PIXELS_PER_BLOCK;
    }

    public CarModel model() {
        return model;
    }

    public PitMode pitMode() {
        return pitMode;
    }

    /** ピットの底のY座標。 */
    public double bottom() {
        return bottomFloor - pitMode.pit;
    }

    /** 昇降路の天井のY座標。 */
    public double top() {
        return topFloor + pitMode.overhead;
    }

    /** 昇降路の内側の全体（描画の範囲を決めるのに使う）。 */
    public Box shaftBounds() {
        double half = px(shaftHalfWidthPx);
        double front = CarGeometry.exterior(model).maxZ();
        return new Box(-half, bottom(), backWallZ, half, top(), front);
    }

    /** かごが最下階にいるときを0とした、かごの移動量。 */
    private double travel(double carY) {
        return carY - bottomFloor;
    }

    /** 頂部の水平なロープの高さ（そらせ車と綱車の上端）。 */
    private double topRopeY() {
        return top() - px(TOP_ROPE_DEPTH_PX);
    }

    // ---- 釣合い重り ----

    /** 釣合い重りの下面のY座標。かごが最上階のとき、ピットの底からランバイの分だけ上。 */
    public double counterweightBottom(double carY) {
        return bottom() + px(COUNTERWEIGHT_RUNBY_PX) + (topFloor - carY);
    }

    /** 釣合い重りの外形。当たり判定にも使う。 */
    public Box counterweight(double carY) {
        double bottom = counterweightBottom(carY);
        double halfWidth = px(COUNTERWEIGHT_WIDTH_PX) / 2;
        double halfThickness = px(COUNTERWEIGHT_THICKNESS_PX) / 2;
        double centreX = counterweightSheaveX;
        return new Box(centreX - halfWidth, bottom, counterweightZ - halfThickness,
                centreX + halfWidth, bottom + px(COUNTERWEIGHT_HEIGHT_PX), counterweightZ + halfThickness);
    }

    /** 釣合い重りのシーブを支える金具（シーブの両側の板）。 */
    public List<Box> counterweightSheaveBrackets(double carY) {
        double top = counterweightBottom(carY) + px(COUNTERWEIGHT_HEIGHT_PX);
        return sheaveBrackets(counterweightSheaveX, top, top + px(COUNTERWEIGHT_SHEAVE_RISE_PX + 1), counterweightZ);
    }

    // ---- かご ----

    /** かご下のシーブを支える金具。 */
    public List<Box> carSheaveBrackets(double carY) {
        List<Box> boxes = new ArrayList<>();
        double bottom = carY + px(CAR_SHEAVE_Y_PX - 1);
        double top = carY - px(CarGeometry.FLOOR_PX);
        for (double x : new double[] {-carSheaveX(), carSheaveX()}) {
            boxes.addAll(sheaveBrackets(x, bottom, top, carRopeZ));
        }
        return boxes;
    }

    private List<Box> sheaveBrackets(double x, double bottom, double top, double z) {
        double halfWidth = px(2);
        double inner = sheaveWidth / 2;
        double outer = inner + px(1);
        return List.of(
                new Box(x - halfWidth, bottom, z - outer, x + halfWidth, top, z - inner),
                new Box(x - halfWidth, bottom, z + inner, x + halfWidth, top, z + outer));
    }

    /** かご下のシーブの中心の X の絶対値。 */
    private double carSheaveX() {
        return ropeX - px(CAR_SHEAVE_RADIUS_PX);
    }

    // ---- 動かない部分 ----

    /** 頂部のそらせ車の中心。かごの右側を上ってきたロープを、上端から奥へ水平に送る。 */
    private Point deflectorCentre() {
        double r = px(DEFLECTOR_RADIUS_PX);
        return new Point(ropeX, topRopeY() - r, carRopeZ - r);
    }

    /** 巻上機の綱車の中心。 */
    private Point machineCentre() {
        return new Point(ropeX, topRopeY() - px(MACHINE_SHEAVE_RADIUS_PX), machineZ);
    }

    /** 巻上機のモーター。綱車と同じ軸で、綱車の内側（−X 側）にある。 */
    public Box motor() {
        Point c = machineCentre();
        double inner = c.x() - sheaveWidth / 2;
        double r = px(MOTOR_RADIUS_PX);
        return new Box(inner - px(MOTOR_LENGTH_PX), c.y() - r, c.z() - r, inner, c.y() + r, c.z() + r);
    }

    /** 釣合い重りのレール。重りの奥に、両端に1本ずつ、ピットの底から天井まで。 */
    public List<Box> counterweightRails() {
        double halfWidth = px(COUNTERWEIGHT_WIDTH_PX) / 2;
        double back = counterweightZ - px(COUNTERWEIGHT_THICKNESS_PX) / 2;
        double width = px(COUNTERWEIGHT_RAIL_WIDTH_PX);
        double depth = px(COUNTERWEIGHT_RAIL_DEPTH_PX);
        double left = counterweightSheaveX - halfWidth;
        double right = counterweightSheaveX + halfWidth;
        return List.of(
                new Box(left, bottom(), back - depth, left + width, top(), back),
                new Box(right - width, bottom(), back - depth, right, top(), back));
    }

    /** ロープの固定端の金具（かご側と釣合い重り側）。 */
    public List<Box> hitches() {
        double y0 = top() - px(HITCH_DEPTH_PX);
        double halfX = px(1.5);
        double halfZ = ropeSpacing + px(1);
        return List.of(
                new Box(-ropeX - halfX, y0, carRopeZ - halfZ, -ropeX + halfX, top(), carRopeZ + halfZ),
                new Box(counterweightDeadEndX() - halfX, y0, counterweightZ - halfZ,
                        counterweightDeadEndX() + halfX, top(), counterweightZ + halfZ));
    }

    private double counterweightDeadEndX() {
        return counterweightSheaveX - px(COUNTERWEIGHT_SHEAVE_RADIUS_PX);
    }

    // ---- シーブ ----

    /**
     * 各シーブ。順に、かご下の左、かご下の右、頂部のそらせ車、巻上機の綱車、釣合い重りのシーブ。
     *
     * <p>回転角は、かごが最下階にいるときを0とし、ロープがシーブの上を滑らずに進んだ長さを半径で割って求める。
     * かごが d 上がると、かご下のシーブ（かごに対して）と釣合い重りのシーブには d、そらせ車と綱車には 2d のロープが通る。
     */
    public List<Sheave> sheaves(double carY) {
        double d = travel(carY);
        double ropeHalf = px(ROPE_WIDTH_PX) / 2;
        double carR = px(CAR_SHEAVE_RADIUS_PX);
        double deflectorR = px(DEFLECTOR_RADIUS_PX);
        double machineR = px(MACHINE_SHEAVE_RADIUS_PX);
        double cwR = px(COUNTERWEIGHT_SHEAVE_RADIUS_PX);
        double carSheaveY = carY + px(CAR_SHEAVE_Y_PX);
        return List.of(
                // ロープはかご下を左から右へ進む（下端が +X へ動く）
                new Sheave(new Point(-carSheaveX(), carSheaveY, carRopeZ), carR - ropeHalf, sheaveWidth, Axis.Z, d / carR),
                new Sheave(new Point(carSheaveX(), carSheaveY, carRopeZ), carR - ropeHalf, sheaveWidth, Axis.Z, d / carR),
                // かご側から上ってきたロープが、上端を奥（−Z）へ進む
                new Sheave(deflectorCentre(), deflectorR - ropeHalf, sheaveWidth, Axis.X, -2 * d / deflectorR),
                new Sheave(machineCentre(), machineR - ropeHalf, sheaveWidth, Axis.X, -2 * d / machineR),
                // ロープは右から下りて、下端を左（−X）へ進む
                new Sheave(counterweightSheaveCentre(carY), cwR - ropeHalf, sheaveWidth, Axis.Z, -d / cwR));
    }

    private Point counterweightSheaveCentre(double carY) {
        double top = counterweightBottom(carY) + px(COUNTERWEIGHT_HEIGHT_PX);
        return new Point(counterweightSheaveX, top + px(COUNTERWEIGHT_SHEAVE_RISE_PX), counterweightZ);
    }

    // ---- ロープ ----

    /**
     * ロープの経路。本数分の折れ線で、かご側の固定端から釣合い重り側の固定端まで。シーブに巻き付く部分は円弧を折れ線にする。
     * 各ロープは、そのとき巻き付いているシーブの軸の方向にずらして並べる。
     */
    public List<List<Point>> ropes(double carY) {
        double hitchY = top() - px(HITCH_DEPTH_PX);
        double carR = px(CAR_SHEAVE_RADIUS_PX);
        double deflectorR = px(DEFLECTOR_RADIUS_PX);
        double machineR = px(MACHINE_SHEAVE_RADIUS_PX);
        double cwR = px(COUNTERWEIGHT_SHEAVE_RADIUS_PX);
        double carSheaveY = carY + px(CAR_SHEAVE_Y_PX);
        Point deflector = deflectorCentre();
        Point machine = machineCentre();
        Point cw = counterweightSheaveCentre(carY);

        List<List<Point>> ropes = new ArrayList<>();
        for (int i = 0; i < ROPE_COUNT; i++) {
            double o = (i - (ROPE_COUNT - 1) / 2.0) * ropeSpacing;
            List<Point> rope = new ArrayList<>();
            rope.add(new Point(-ropeX, hitchY, carRopeZ + o));
            // かご下の左のシーブ：左端（180°）から下端（270°）へ
            arcXY(rope, -carSheaveX(), carSheaveY, carRopeZ + o, carR, 180, 270);
            // かご下の右のシーブ：下端（270°）から右端（360°）へ
            arcXY(rope, carSheaveX(), carSheaveY, carRopeZ + o, carR, 270, 360);
            // そらせ車：手前（+Z）から上端へ。ここからロープは軸（X）の方向に並ぶ
            arcZY(rope, deflector.x() + o, deflector.y(), deflector.z(), deflectorR, 0, 90);
            // 巻上機の綱車：上端から奥へ
            arcZY(rope, machine.x() + o, machine.y(), machine.z(), machineR, 90, 180);
            // 釣合い重りのシーブ：右端から下を通って左端へ
            arcXY(rope, cw.x(), cw.y(), cw.z() + o, cwR, 0, -180);
            rope.add(new Point(counterweightDeadEndX(), hitchY, counterweightZ + o));
            ropes.add(rope);
        }
        return ropes;
    }

    /** XY平面（軸が Z）の円弧。角度は +X から +Y へ測る [deg]。 */
    private static void arcXY(List<Point> out, double cx, double cy, double z, double r, double fromDeg, double toDeg) {
        int steps = arcSteps(fromDeg, toDeg);
        for (int s = 0; s <= steps; s++) {
            double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * s / steps);
            out.add(new Point(cx + r * Math.cos(a), cy + r * Math.sin(a), z));
        }
    }

    /** ZY平面（軸が X）の円弧。角度は +Z から +Y へ測る [deg]。 */
    private static void arcZY(List<Point> out, double x, double cy, double cz, double r, double fromDeg, double toDeg) {
        int steps = arcSteps(fromDeg, toDeg);
        for (int s = 0; s <= steps; s++) {
            double a = Math.toRadians(fromDeg + (toDeg - fromDeg) * s / steps);
            out.add(new Point(x, cy + r * Math.sin(a), cz + r * Math.cos(a)));
        }
    }

    private static int arcSteps(double fromDeg, double toDeg) {
        return Math.max(1, (int) Math.round(Math.abs(toDeg - fromDeg) / 90 * ARC_STEPS_PER_QUARTER));
    }
}

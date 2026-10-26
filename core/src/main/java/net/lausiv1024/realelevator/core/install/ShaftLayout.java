package net.lausiv1024.realelevator.core.install;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.car.FloorEntrances;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;

/**
 * 制御盤を基準にした昇降路の配置。
 *
 * <p>座標は制御盤のブロックの中心を原点とし、+Z を制御盤の正面（乗場側）、+X を乗場から見て右とする。
 * ブロックは中心の座標（整数）で表す。高さは制御盤（最下階の床面）からのブロック数。
 *
 * <ul>
 * <li>制御盤は乗場の壁の列（Z=0）にある。昇降路と三方枠は、制御盤の左隣から左へ、機種で決まる幅（3または4ブロック）だけ並ぶ
 * （幅3ブロックなら X=−3〜−1、4ブロックなら X=−4〜−1）。出入口はその中央で、三方枠の高さは{@value DoorGeometry#LANDING_FRAME_HEIGHT}ブロック。</li>
 * <li>乗場の戸は壁の列の昇降路側の面（Z=−0.5）に付き、かごはその奥（−Z 側）にある。昇降路の奥行きは、
 * かごの奥行きに釣合い重りの1列を足した分（横落としでは、重りの列を右に足し、奥行きはかごの分だけ）。</li>
 * <li>ガイドレールは、かごの中心の列で、昇降路の左右の端のブロックに置く。</li>
 * </ul>
 */
public final class ShaftLayout {
    /** 停止階の間隔の最小値。かごの外形の高さ（約3.7ブロック）より広くする。 */
    public static final int MIN_FLOOR_SPACING = 4;
    /**
     * 乗場の器具を出入口の上に付けられる停止階の間隔の最小値。間隔が4ブロックだと、上の階の床が三方枠の一番上の段の前に来て、
     * 出入口の上の表示器やランタンを隠してしまう。
     */
    public static final int MIN_SPACING_FOR_OVER_DOOR = 5;

    public enum PitMode {
        /** 実機の寸法（ピット約1.3 m、頂部すき間約4 m）を1.5倍したもの。 */
        REAL(2, 6),
        /** かごが収まる最小限。 */
        COMPACT(1, 4);

        /** 最下階の床より下の深さ [ブロック]。 */
        public final int pit;
        /** 最上階の床から昇降路の天井までの高さ [ブロック]。 */
        public final int overhead;

        PitMode(int pit, int overhead) {
            this.pit = pit;
            this.overhead = overhead;
        }
    }

    /** ブロックの位置（制御盤からの相対）。 */
    public record Cell(int x, int y, int z) {
    }

    public enum FloorProblem {
        /** 問題なし。 */
        NONE,
        /** 停止階が2つ未満。 */
        TOO_FEW,
        /** 間隔が {@value #MIN_FLOOR_SPACING} ブロック未満の停止階がある。 */
        TOO_CLOSE,
        /** 制御盤（最下階）より下に停止階がある。 */
        BELOW_PANEL,
        /** 停止階が {@value #MAX_FLOORS} を超える。 */
        TOO_MANY
    }

    /**
     * 停止階の数の上限。主操作盤に行先ボタンが収まる数（2px角のボタンで非常呼びを省いて18階床）。
     * これを超える建物は、将来は乗場で行先を選ぶ方式（行先予約）に限って対応し、それまでは低層・高層などに分けて運用する。
     */
    public static final int MAX_FLOORS = 18;

    /** 釣合い重りの位置。 */
    public enum CounterweightPosition {
        /** かごの奥（後ろ落とし）。昇降路の奥行きに重りの1列を足す。 */
        REAR,
        /**
         * かごの右（横落とし）。設置用ブロックの真後ろの列（X=0、乗場の壁の列より奥）を重りの列にする。
         * 展望仕様と二方向出入口で使う。
         */
        SIDE
    }

    private final CarSpec spec;
    private final CarModel model;
    private final PitMode pitMode;
    private final CounterweightPosition counterweight;
    private final int[] floors;
    /** 二方向出入口（前後）のときの、階ごとの出入口（floors と同じ並び）。一方向出入口なら null。 */
    private final FloorEntrances[] entrances;

    /**
     * @param floors 停止階の床面の高さ（制御盤からの相対、0 が最下階）
     */
    public ShaftLayout(CarModel model, PitMode pitMode, int[] floors) {
        this(CarSpec.of(model), pitMode, floors);
    }

    public ShaftLayout(CarSpec spec, PitMode pitMode, int[] floors) {
        this(spec, pitMode, CounterweightPosition.REAR, floors);
    }

    public ShaftLayout(CarSpec spec, PitMode pitMode, CounterweightPosition counterweight, int[] floors) {
        this(spec, pitMode, counterweight, floors, null);
    }

    /**
     * @param entrances 二方向出入口（前後）なら、階ごとの出入口（floors を下から並べた順）。一方向出入口なら null。
     *                  二方向出入口では、釣合い重りは横落としに決まる
     */
    public ShaftLayout(CarSpec spec, PitMode pitMode, CounterweightPosition counterweight, int[] floors, FloorEntrances[] entrances) {
        this.spec = spec;
        this.model = spec.model();
        this.pitMode = pitMode;
        this.counterweight = entrances != null ? CounterweightPosition.SIDE : counterweight;
        this.floors = floors.clone();
        Arrays.sort(this.floors);
        this.entrances = entrances != null ? entrances.clone() : null;
    }

    /** 二方向出入口（前後）か。 */
    public boolean isThrough() {
        return entrances != null;
    }

    /** floor 番目（下から）の階で使う出入口。 */
    public FloorEntrances entrances(int floor) {
        return entrances == null || floor >= entrances.length ? FloorEntrances.FRONT : entrances[floor];
    }

    /**
     * 二方向出入口の奥の乗場の壁の列（Z）。正面と同じく、乗場の戸のかご側の面より乗場側で、昇降路側の面がいちばん近い列。
     * 奥の乗場の戸はこの列の中を動き、奥の三方枠は乗場の戸からこの列の乗場側の面までを埋める（正面より最大で1ブロック厚い）。
     */
    public int rearWallRow() {
        double doorCarSide = carZ() - DoorGeometry.travelBounds(spec, DoorGeometry.Side.LANDING).minZ();
        return (int) Math.floor(doorCarSide - 0.5 + 1e-9);
    }

    /**
     * 二方向出入口の奥の乗場の面を、正面の乗場の面（{@link DoorGeometry#landingFace}）から乗場側へずらす量 [ブロック]。
     * 奥の三方枠と乗場の器具を、奥の形（正面の形を180°回したもの）で求めるときに使う。
     */
    public double rearFaceOffset() {
        double rearFace = carZ() - (rearWallRow() - 0.5);
        return rearFace - DoorGeometry.landingFace(spec);
    }

    /** 停止階の並びに問題がないか。 */
    public static FloorProblem check(int[] floors) {
        if (floors.length < 2) {
            return FloorProblem.TOO_FEW;
        }
        if (floors.length > MAX_FLOORS) {
            return FloorProblem.TOO_MANY;
        }
        int[] sorted = floors.clone();
        Arrays.sort(sorted);
        if (sorted[0] < 0) {
            return FloorProblem.BELOW_PANEL;
        }
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] - sorted[i - 1] < MIN_FLOOR_SPACING) {
                return FloorProblem.TOO_CLOSE;
            }
        }
        return FloorProblem.NONE;
    }

    /** 停止階の並び floors で、乗場の器具を出入口の上に付けられるか。 */
    public static boolean allowsOverDoorHallFixtures(int[] floors) {
        int[] sorted = floors.clone();
        Arrays.sort(sorted);
        for (int i = 1; i < sorted.length; i++) {
            if (sorted[i] - sorted[i - 1] < MIN_SPACING_FOR_OVER_DOOR) {
                return false;
            }
        }
        return true;
    }

    /** 停止階の並び floors で付けられる配置にする。出入口の上に付けられなければ、横へ移す。 */
    public static FixtureConfig fitFixtures(FixtureConfig config, int[] floors) {
        return allowsOverDoorHallFixtures(floors) ? config : config.withoutOverDoorHallFixtures();
    }

    /** 昇降路の左端の列の X。 */
    public int leftColumn() {
        return -model.shaftWidth();
    }

    /** 昇降路の右端の列の X（制御盤の左隣）。 */
    public int rightColumn() {
        return -1;
    }

    /** かごの原点（かご内の床面の中心）の X。昇降路の幅の中央（幅4ブロックならブロックの境目）。 */
    public double carX() {
        return (leftColumn() + rightColumn()) / 2.0;
    }

    /** かごの原点の Z。乗場の戸が壁の列の昇降路側の面（Z=−0.5）に来るように置く。 */
    public double carZ() {
        double landingDoorFromCar = DoorGeometry.travelBounds(spec, DoorGeometry.Side.LANDING).minZ();
        return -0.5 - landingDoorFromCar;
    }

    /**
     * 昇降路の奥行きの列（Z）。かごの奥の面を含む列から、壁の列の手前（Z=−1）まで。後ろ落としでは、さらに釣合い重りの1列を足す。
     */
    public int backRow() {
        if (isThrough()) {
            // 奥の乗場の壁の列の手前まで
            return rearWallRow() + 1;
        }
        double carBack = carZ() + CarGeometry.exterior(model).minZ();
        int carBackRow = (int) Math.floor(carBack + 0.5);
        return counterweight == CounterweightPosition.REAR ? carBackRow - 1 : carBackRow;
    }

    public CounterweightPosition counterweight() {
        return counterweight;
    }

    /** 釣合い重りの列の X（横落としのときだけ。設置用ブロックの真後ろ）。 */
    public int counterweightColumn() {
        return 0;
    }

    /** 昇降路の右端の X（横落としでは重りの列を含む）。 */
    private int shaftRightColumn() {
        return counterweight == CounterweightPosition.SIDE ? counterweightColumn() : rightColumn();
    }

    /** 昇降路の一番下（ピットの底）の高さ。 */
    public int bottom() {
        return -pitMode.pit;
    }

    /** 昇降路の天井の高さ（このブロックは含まない）。 */
    public int top() {
        return floors[floors.length - 1] + pitMode.overhead;
    }

    /**
     * 設置する前に空いている（空気である）必要があるブロック。昇降路の全体と、各階の三方枠の場所。
     */
    public List<Cell> requiredClearCells() {
        List<Cell> cells = new ArrayList<>();
        for (int y = bottom(); y < top(); y++) {
            for (int x = leftColumn(); x <= shaftRightColumn(); x++) {
                for (int z = backRow(); z <= -1; z++) {
                    cells.add(new Cell(x, y, z));
                }
            }
        }
        for (int i = 0; i < floors.length; i++) {
            int floor = floors[i];
            if (!entrances(i).has(Entrance.REAR)) {
                continue;
            }
            // 奥の三方枠
            for (int y = floor; y < floor + DoorGeometry.LANDING_FRAME_HEIGHT; y++) {
                for (int x = leftColumn(); x <= rightColumn(); x++) {
                    cells.add(new Cell(x, y, rearWallRow()));
                }
            }
        }
        for (int i = 0; i < floors.length; i++) {
            int floor = floors[i];
            if (!entrances(i).has(Entrance.FRONT)) {
                continue;
            }
            for (int y = floor; y < floor + DoorGeometry.LANDING_FRAME_HEIGHT; y++) {
                for (int x = leftColumn(); x <= rightColumn(); x++) {
                    cells.add(new Cell(x, y, 0));
                }
            }
        }
        return cells;
    }

    /** ガイドレールの断面 [px]。ブロックのかごと反対側の端にあり、奥行きはブロックの中央。 */
    public static final int RAIL_WIDTH_PX = 4;
    public static final int RAIL_DEPTH_PX = 4;

    /** ガイドレールのブロック。left は乗場から見て左（−X）側のレールか。 */
    public record Rail(Cell cell, boolean left) {
        /** レールの形（制御盤からの相対座標）。 */
        public Box box() {
            double p = CarModel.PIXELS_PER_BLOCK;
            double outer = left ? cell.x() - 0.5 : cell.x() + 0.5;
            double inner = left ? outer + RAIL_WIDTH_PX / p : outer - RAIL_WIDTH_PX / p;
            double halfDepth = RAIL_DEPTH_PX / p / 2;
            return new Box(Math.min(outer, inner), cell.y(), cell.z() - halfDepth,
                    Math.max(outer, inner), cell.y() + 1, cell.z() + halfDepth);
        }
    }

    /**
     * ガイドレール。左右とも、ピットの底から天井まで。ただし右のレールは、頂部のそらせ車（昇降路の機構）と重ならないよう、
     * 天井の1ブロック下で止める。
     */
    /** ガイドレールの列（かごの中心の列）の Z。 */
    public int railRowZ() {
        return (int) Math.floor(carZ() + 0.5);
    }

    public List<Rail> rails() {
        int row = railRowZ();
        List<Rail> rails = new ArrayList<>();
        for (int y = bottom(); y < top(); y++) {
            rails.add(new Rail(new Cell(leftColumn(), y, row), true));
            if (y < top() - 1) {
                rails.add(new Rail(new Cell(rightColumn(), y, row), false));
            }
        }
        return rails;
    }

    /** 頂部の制御盤の高さ [ブロック]。 */
    public static final int CONTROLLER_HEIGHT = 2;
    /** 頂部の制御盤の幅（壁に沿った向き）と厚み [px]。横落としでかごの横のすき間（4px〜）に収まる厚み。 */
    public static final int CONTROLLER_WIDTH_PX = 12;
    public static final int CONTROLLER_DEPTH_PX = 4;

    /**
     * 頂部の制御盤のブロック（下から順）。昇降路の左奥の隅の、天井から下 {@value #CONTROLLER_HEIGHT} ブロック。
     * この列は、かご・釣合い重り・巻上機・ロープ・ガイドレールのどれとも重ならない。
     */
    public List<Cell> controllerCells() {
        List<Cell> cells = new ArrayList<>();
        for (int y = top() - CONTROLLER_HEIGHT; y < top(); y++) {
            cells.add(controllerMount() == ControllerMount.FRONT_WALL
                    ? new Cell(counterweightColumn(), y, -1)
                    : new Cell(leftColumn(), y, controllerRow()));
        }
        return cells;
    }

    /** 頂部の制御盤を付ける壁。 */
    public enum ControllerMount {
        /** 奥の壁（後ろ落とし。昇降路の左奥の隅）。 */
        BACK_WALL,
        /** 左の壁の奥寄り（横落とし）。 */
        LEFT_WALL,
        /** 乗場の壁の裏（二方向出入口の前後。重りの列の、設置用ブロックの真後ろ）。 */
        FRONT_WALL
    }

    public ControllerMount controllerMount() {
        if (isThrough()) {
            return ControllerMount.FRONT_WALL;
        }
        return counterweight == CounterweightPosition.SIDE ? ControllerMount.LEFT_WALL : ControllerMount.BACK_WALL;
    }

    /**
     * 頂部の制御盤の形（制御盤からの相対座標）。ブロックごとに、壁に付いた幅 {@value #CONTROLLER_WIDTH_PX}px・厚み {@value #CONTROLLER_DEPTH_PX}px の箱。
     * 後ろ落としでは奥の壁の中央、横落としでは左の壁の奥寄り、二方向出入口では乗場の壁の裏の中央に付く。
     */
    public List<Box> controllerBoxes() {
        double p = CarModel.PIXELS_PER_BLOCK;
        double halfWidth = CONTROLLER_WIDTH_PX / p / 2;
        double depth = CONTROLLER_DEPTH_PX / p;
        List<Box> boxes = new ArrayList<>();
        for (Cell cell : controllerCells()) {
            // 横落としでは、左の壁の奥寄り（かご側のロープの固定端を避ける）
            boxes.add(switch (controllerMount()) {
                case LEFT_WALL -> new Box(cell.x() - 0.5, cell.y(), cell.z() - 0.5, cell.x() - 0.5 + depth, cell.y() + 1, cell.z() - 0.5 + halfWidth * 2);
                case BACK_WALL -> new Box(cell.x() - halfWidth, cell.y(), cell.z() - 0.5, cell.x() + halfWidth, cell.y() + 1, cell.z() - 0.5 + depth);
                case FRONT_WALL -> new Box(cell.x() - halfWidth, cell.y(), cell.z() + 0.5 - depth, cell.x() + halfWidth, cell.y() + 1, cell.z() + 0.5);
            });
        }
        return boxes;
    }

    /**
     * 頂部の制御盤の列（Z）。後ろ落としでは奥の重りの列（左奥の隅）。横落としでは左の壁に付けるので、
     * かご側のロープとガイドレールを避けた、いちばん奥の列にする。
     */
    private int controllerRow() {
        if (counterweight == CounterweightPosition.REAR) {
            return backRow();
        }
        // 左の壁のすき間には、かご側のロープ（奥行きの中央から6pxまでずれる）と固定端、ガイドレールが通る。それを避けた奥の列
        double p = CarModel.PIXELS_PER_BLOCK;
        double ropeBand = (6 + 3) / p;
        double railHalf = RAIL_DEPTH_PX / p / 2;
        double railZ = railRowZ() - carZ();
        // 二方向出入口では、奥の乗場の戸が動く範囲にもかからないようにする
        double rearDoors = isThrough() ? -DoorGeometry.travelBounds(spec, DoorGeometry.Side.LANDING).minZ() : Double.NEGATIVE_INFINITY;
        for (int row = backRow(); row <= -1; row++) {
            double from = row - 0.5 - carZ();
            double to = from + CONTROLLER_WIDTH_PX / p;
            boolean clearOfRopes = to <= -ropeBand || from >= ropeBand;
            boolean clearOfRail = to <= railZ - railHalf || from >= railZ + railHalf;
            if (clearOfRopes && clearOfRail && from >= rearDoors - 1e-9) {
                return row;
            }
        }
        return backRow();
    }



    public CarSpec spec() {
        return spec;
    }

    public CarModel model() {
        return model;
    }

    public PitMode pitMode() {
        return pitMode;
    }

    public int[] floors() {
        return floors.clone();
    }
}

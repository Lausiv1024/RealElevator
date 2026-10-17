package net.lausiv1024.realelevator.core.install;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;

/**
 * 制御盤を基準にした昇降路の配置。
 *
 * <p>座標は制御盤のブロックの中心を原点とし、+Z を制御盤の正面（乗場側）、+X を乗場から見て右とする。
 * ブロックは中心の座標（整数）で表す。高さは制御盤（最下階の床面）からのブロック数。
 *
 * <ul>
 * <li>制御盤は乗場の壁の列（Z=0）にある。出入口は乗場から見て制御盤の2ブロック左（X=−2）が中心で、
 * 三方枠は X=−3〜−1 の3ブロック、高さ{@value DoorGeometry#LANDING_FRAME_HEIGHT}ブロック。</li>
 * <li>乗場の戸は壁の列の昇降路側の面（Z=−0.5）に付き、かごはその奥（−Z 側）にある。昇降路は X=−3〜−1 の幅3ブロックで、
 * かごの奥行きに釣合い重りの1列を足した奥行き。</li>
 * <li>ガイドレールは、かごの中心の列で、昇降路の左右の端のブロックに置く。</li>
 * </ul>
 */
public final class ShaftLayout {
    /** 出入口の中心（かごの中心）の、制御盤からの X。 */
    public static final int CAR_COLUMN = -2;
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
        BELOW_PANEL
    }

    private final CarModel model;
    private final PitMode pitMode;
    private final int[] floors;

    /**
     * @param floors 停止階の床面の高さ（制御盤からの相対、0 が最下階）
     */
    public ShaftLayout(CarModel model, PitMode pitMode, int[] floors) {
        this.model = model;
        this.pitMode = pitMode;
        this.floors = floors.clone();
        Arrays.sort(this.floors);
    }

    /** 停止階の並びに問題がないか。 */
    public static FloorProblem check(int[] floors) {
        if (floors.length < 2) {
            return FloorProblem.TOO_FEW;
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

    /** かごの原点（かご内の床面の中心）の X。 */
    public double carX() {
        return CAR_COLUMN;
    }

    /** かごの原点の Z。乗場の戸が壁の列の昇降路側の面（Z=−0.5）に来るように置く。 */
    public double carZ() {
        double landingDoorFromCar = DoorGeometry.travelBounds(model, DoorGeometry.Side.LANDING).minZ();
        return -0.5 - landingDoorFromCar;
    }

    /** 昇降路の奥行きの列（Z）。かごの奥の面を含む列から、釣合い重りの1列を足して、壁の列の手前（Z=−1）まで。 */
    public int backRow() {
        double carBack = carZ() + CarGeometry.exterior(model).minZ();
        return (int) Math.floor(carBack + 0.5) - 1;
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
            for (int x = CAR_COLUMN - 1; x <= CAR_COLUMN + 1; x++) {
                for (int z = backRow(); z <= -1; z++) {
                    cells.add(new Cell(x, y, z));
                }
            }
        }
        for (int floor : floors) {
            for (int y = floor; y < floor + DoorGeometry.LANDING_FRAME_HEIGHT; y++) {
                for (int x = CAR_COLUMN - 1; x <= CAR_COLUMN + 1; x++) {
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
    public List<Rail> rails() {
        int row = (int) Math.floor(carZ() + 0.5);
        List<Rail> rails = new ArrayList<>();
        for (int y = bottom(); y < top(); y++) {
            rails.add(new Rail(new Cell(CAR_COLUMN - 1, y, row), true));
            if (y < top() - 1) {
                rails.add(new Rail(new Cell(CAR_COLUMN + 1, y, row), false));
            }
        }
        return rails;
    }

    /** 頂部の制御盤の高さ [ブロック]。 */
    public static final int CONTROLLER_HEIGHT = 2;

    /**
     * 頂部の制御盤のブロック（下から順）。昇降路の左奥の隅の、天井から下 {@value #CONTROLLER_HEIGHT} ブロック。
     * この列は、かご・釣合い重り・巻上機・ロープ・ガイドレールのどれとも重ならない。
     */
    public List<Cell> controllerCells() {
        List<Cell> cells = new ArrayList<>();
        for (int y = top() - CONTROLLER_HEIGHT; y < top(); y++) {
            cells.add(new Cell(CAR_COLUMN - 1, y, backRow()));
        }
        return cells;
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

package net.lausiv1024.realelevator.core.design;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Normal;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.design.InteriorConfig.FrameStyle;
import net.lausiv1024.realelevator.core.design.InteriorConfig.Landing;
import net.lausiv1024.realelevator.core.design.InteriorConfig.PanelStyle;
import net.lausiv1024.realelevator.core.display.FaceGrid;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.display.HallLantern;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Kind;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.core.panel.FixtureConfig.HallIndicator;
import net.lausiv1024.realelevator.core.panel.FixtureConfig.HallLanternPlacement;
import net.lausiv1024.realelevator.core.panel.WheelchairPanelLayout;

/**
 * 乗場の形と器具の配置。三方枠の範囲（昇降路と同じ幅、高さ{@value DoorGeometry#LANDING_FRAME_HEIGHT}ブロック）に、
 * 出入口のまわりの枠、枠の外の壁（カメレオン）、敷居、操作盤、階床表示器、ホールランタンを置く。
 * 座標はかごのローカル座標で、かごがその階に着床したときの位置で表す（乗場は +Z 側）。
 *
 * <ul>
 * <li>枠：出入口のまわりに幅{@value #FRAME_PX}px、壁の面から{@value #FRAME_PROTRUSION_PX}px出っ張る枠。幕板付きでは、上の部分を
 * 三方枠の範囲の上端まで伸ばす。埋め込みの操作盤では、右の枠を{@value #EMBEDDED_FRAME_PX}pxにする。</li>
 * <li>壁：三方枠の範囲のうち枠の外。ブロックの格子に合わせて1ブロックずつに分け、まわりの壁と同じ素材の見た目で描く。</li>
 * <li>操作盤：壁付けでは、枠のすぐ右（1px空ける。脇が狭ければ接して）に幅{@value #PANEL_WIDTH_PX}pxの板を貼る（3文字の階名があれば、板と表示窓を{@value #LONG_LABEL_EXTRA_PX}px広げる）。上から、表示窓、▲▼、車椅子マーク、
 * 車椅子用の▲▼、鍵スイッチ（飾り）。埋め込みでは、同じ並びを右の枠に埋め込む。表示窓がないとき（群管理向けや、表示器が扉の上のとき）は、
 * ボタンを少し下げる。</li>
 * <li>ホールランタン：扉の横は、枠のすぐ左（1px空ける）に縦長。扉の右（群管理向けだけ）は操作盤の上に縦長。扉の上は、細枠なら枠の上の壁に、
 * 幕板付きなら幕板に横長。背面プレート（ランプのまわり{@value #LANTERN_PLATE_MARGIN_PX}px）は壁に付けるときだけ。</li>
 * </ul>
 */
public final class LandingLayout {
    public static final int FRAME_PX = 4;
    public static final int FRAME_PROTRUSION_PX = 1;
    public static final int EMBEDDED_FRAME_PX = 7;
    public static final int PANEL_WIDTH_PX = 5;
    /** 枠と、壁付けの操作盤やランタンの間のすき間 [px]。 */
    public static final int PANEL_GAP_PX = 1;
    public static final int WINDOW_WIDTH_PX = 4;
    /** 3文字の階名のために、壁付けの操作盤の板と表示窓を広げる幅 [px]（0.5px のドットで 3×5 の文字3つ）。 */
    public static final int LONG_LABEL_EXTRA_PX = 2;
    public static final int WINDOW_HEIGHT_PX = 7;
    public static final int MARK_PX = 4;
    public static final int KEY_PX = 2;
    public static final int LANTERN_PLATE_MARGIN_PX = 2;
    /** 表示窓があるときの、▼の下端の高さ [px]。 */
    public static final int DOWN_WITH_WINDOW_PX = 29;
    /** 表示窓がないときの、▼の下端の高さ [px]（実機 約1000 mm）。 */
    public static final int DOWN_WITHOUT_WINDOW_PX = 25;
    /** ▲▼などの並びのすき間 [px]。 */
    private static final int GAP_PX = 2;
    private static final int LANTERN_WIDTH_PX = 2;
    private static final int LANTERN_SECTION_PX = 7;
    /** 縦長のランタンの下端の高さ [px]。上端が枠の上端の少し下に来る。 */
    private static final int LANTERN_BOTTOM_PX = 37;
    /** 横長のランタンの1段の高さ [px]。 */
    private static final int OVER_SECTION_PX = 2;
    private static final int TOP_PX = DoorGeometry.LANDING_FRAME_HEIGHT * CarModel.PIXELS_PER_BLOCK;
    private static final int BLOCK_PX = CarModel.PIXELS_PER_BLOCK;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;
    /** 面に重ねるものを浮かせる量。 */
    private static final double LIFT = 0.002;
    /** 表示窓や車椅子マークの厚み。 */
    private static final double SURFACE = PX / 8;
    /** 鍵スイッチの出っ張り。 */
    private static final double KEY_DEPTH = PX / 4;

    public enum Type {
        /** 枠（三方枠・幕板）。枠の素材で描く。 */
        FRAME,
        /** 敷居（アルミ色）。 */
        SILL,
        /** 敷居の戸の溝（暗い線）。 */
        SILL_GROOVE,
        /** 壁付けの操作盤の板（ステンレス）。 */
        PLATE,
        /** 表示窓の黒い地。 */
        WINDOW,
        /** 車椅子マークの青い地。 */
        MARK,
        /** 車椅子マークの白い図柄。 */
        MARK_SYMBOL,
        /** 鍵スイッチ（飾り）。 */
        KEY,
        /** 鍵スイッチの鍵穴。 */
        KEY_SLOT,
        /** ホールランタンの背面プレート（ステンレス）。 */
        LANTERN_PLATE
    }

    public record Part(Type type, Box box) {
    }

    /**
     * 枠の外の壁の、1ブロックの中に収まる一片。u・v は、この一片の左下が、そのブロックの面のテクスチャのどこにあたるか [ブロック]
     * （まわりのブロックとテクスチャの継ぎ目をそろえるため）。乗場の側の面（+Z）だけを描く。
     */
    public record WallCell(Box box, double u, double v) {
    }

    private final CarSpec spec;
    private final FixtureConfig fixtures;
    private final Landing landing;
    private final double face;
    private final double back;
    private final int half;
    private final int entranceMin;
    private final int entranceMax;
    private final int entranceTop;
    private final int leftOuter;
    private final int rightOuter;
    private final int headTop;
    private final boolean embedded;
    private final boolean window;
    /** 3文字の階名があるか（表示器を広げる）。 */
    private final boolean longLabels;
    /** 壁付けの操作盤の板と、表示窓の幅 [px]。 */
    private final int panelWidth;
    private final int windowWidth;
    private final int down;
    /** 操作盤の列の中心の X [px]。 */
    private final double column;
    /** 操作盤の部品を付ける面の Z [ブロック]。 */
    private final double surface;

    private LandingLayout(CarSpec spec, FixtureConfig fixtures, Landing landing, double faceOffset, boolean longLabels) {
        this.spec = spec;
        this.longLabels = longLabels;
        this.panelWidth = PANEL_WIDTH_PX + (longLabels ? LONG_LABEL_EXTRA_PX : 0);
        this.windowWidth = WINDOW_WIDTH_PX + (longLabels ? LONG_LABEL_EXTRA_PX : 0);
        this.fixtures = fit(fixtures, landing);
        this.landing = landing;
        this.face = DoorGeometry.landingFace(spec, faceOffset);
        this.back = DoorGeometry.travelBounds(spec, DoorGeometry.Side.LANDING).maxZ();
        this.half = spec.model().shaftWidth() * BLOCK_PX / 2;
        this.entranceMin = spec.entranceMinXPx();
        this.entranceMax = spec.entranceMaxXPx();
        this.entranceTop = CarModel.ENTRANCE_HEIGHT_PX;
        this.embedded = landing.panel() == PanelStyle.EMBEDDED;
        this.leftOuter = entranceMin - FRAME_PX;
        this.rightOuter = entranceMax + (embedded ? EMBEDDED_FRAME_PX : FRAME_PX);
        this.headTop = landing.frame() == FrameStyle.FASCIA ? TOP_PX : entranceTop + FRAME_PX;
        this.window = this.fixtures.hasHallIndicator() && this.fixtures.hallIndicator() == HallIndicator.OVER_BUTTONS;
        this.down = window ? DOWN_WITH_WINDOW_PX : DOWN_WITHOUT_WINDOW_PX;
        // 脇が狭くて（片開きの P-13 など）すき間を空けると収まらないときは、枠に接して貼る
        int gap = Math.clamp(half - rightOuter - panelWidth, 0, PANEL_GAP_PX);
        this.column = embedded ? entranceMax + EMBEDDED_FRAME_PX / 2.0 : rightOuter + gap + panelWidth / 2.0;
        this.surface = embedded ? frameFront() : face + PX;
    }

    /** 出入口 entrance の乗場（乗場の面を faceOffset だけずらしたもの）の配置。fixtures は {@link #fit} で選べる配置に直す。 */
    public static LandingLayout of(CarSpec spec, FixtureConfig fixtures, Landing landing, double faceOffset) {
        return of(spec, fixtures, landing, faceOffset, false);
    }

    /** {@link #of(CarSpec, FixtureConfig, Landing, double)} の、longLabels なら3文字の階名の分だけ表示器を広げたもの。 */
    public static LandingLayout of(CarSpec spec, FixtureConfig fixtures, Landing landing, double faceOffset, boolean longLabels) {
        return new LandingLayout(spec, fixtures, landing, faceOffset, longLabels);
    }

    /**
     * 乗場の意匠で選べる配置に直す。群管理向けでなければ、ランタンを扉の右にできない（扉の横にする）。
     * 細枠では、扉の上に表示器とランタンの両方は収まらないので、ランタンを扉の横にする。
     */
    public static FixtureConfig fit(FixtureConfig fixtures, Landing landing) {
        FixtureConfig result = fixtures;
        if (!result.groupControl() && result.hallLantern() == HallLanternPlacement.RIGHT_OF_DOOR) {
            result = result.withHallLantern(HallLanternPlacement.BESIDE_DOOR);
        }
        if (landing.frame() == FrameStyle.THIN && result.hasHallIndicator() && result.hallIndicator() == HallIndicator.OVER_DOOR
                && result.hallLantern() == HallLanternPlacement.OVER_DOOR) {
            result = result.withHallLantern(HallLanternPlacement.BESIDE_DOOR);
        }
        return result;
    }

    /** 設定画面で選べるホールランタンの位置。overDoorAllowed は、停止階の間隔から扉の上に付けられるか。 */
    public static List<HallLanternPlacement> lanternChoices(FixtureConfig fixtures, Landing landing, boolean overDoorAllowed) {
        List<HallLanternPlacement> choices = new ArrayList<>();
        for (HallLanternPlacement placement : HallLanternPlacement.values()) {
            if (placement == HallLanternPlacement.OVER_DOOR && !overDoorAllowed) {
                continue;
            }
            if (fit(fixtures.withHallLantern(placement), landing).hallLantern() == placement) {
                choices.add(placement);
            }
        }
        return choices;
    }

    /** 選べる配置に直した設定。 */
    public FixtureConfig fixtures() {
        return fixtures;
    }

    /** 三方枠の範囲全体（描画のカリング用）。器具の出っ張りを含む。 */
    public static Box bounds(CarSpec spec, double faceOffset) {
        double half = spec.model().shaftWidth() / 2.0;
        double back = DoorGeometry.travelBounds(spec, DoorGeometry.Side.LANDING).maxZ();
        return new Box(-half, -PX, back, half, DoorGeometry.LANDING_FRAME_HEIGHT, DoorGeometry.landingFace(spec, faceOffset) + 3 * PX);
    }

    /** 枠の、乗場の側の面の Z。 */
    private double frameFront() {
        return face + FRAME_PROTRUSION_PX * PX;
    }

    /** 枠・敷居・操作盤の板と部品・ランタンの背面プレート。 */
    public List<Part> parts() {
        List<Part> parts = new ArrayList<>();
        double front = frameFront();
        parts.add(new Part(Type.FRAME, box(leftOuter, 0, entranceMin, entranceTop, back, front)));
        parts.add(new Part(Type.FRAME, box(entranceMax, 0, rightOuter, entranceTop, back, front)));
        parts.add(new Part(Type.FRAME, box(leftOuter, entranceTop, rightOuter, headTop, back, front)));
        parts.addAll(sill(spec, DoorGeometry.Side.LANDING, front));
        if (!embedded) {
            parts.add(new Part(Type.PLATE, box(column - panelWidth / 2.0, plateBottom(), column + panelWidth / 2.0, plateTop(),
                    face, face + PX)));
        }
        if (window) {
            parts.add(new Part(Type.WINDOW, windowBox()));
        }
        int keyBottom = fixtures.wheelchairPanel() ? down - 19 : down - 3;
        parts.add(new Part(Type.KEY, box(column - KEY_PX / 2.0, keyBottom, column + KEY_PX / 2.0, keyBottom + KEY_PX,
                surface, surface + KEY_DEPTH)));
        parts.add(new Part(Type.KEY_SLOT, box(column - 0.125, keyBottom + 0.5, column + 0.125, keyBottom + 1.5,
                surface + KEY_DEPTH, surface + KEY_DEPTH + LIFT)));
        if (fixtures.wheelchairPanel()) {
            double markTop = down - GAP_PX;
            parts.add(new Part(Type.MARK, box(column - MARK_PX / 2.0, markTop - MARK_PX, column + MARK_PX / 2.0, markTop,
                    surface, surface + SURFACE)));
            FaceGrid grid = new FaceGrid(Normal.POS_Z, surface + SURFACE, (column - MARK_PX / 2.0 + 0.5) * PX, (markTop - 0.5) * PX, PX / 4);
            for (Box dot : WheelchairPanelLayout.markSymbol(grid)) {
                parts.add(new Part(Type.MARK_SYMBOL, dot));
            }
        }
        Lamp lamp = lamp();
        if (lamp != null && lamp.plated()) {
            int m = LANTERN_PLATE_MARGIN_PX;
            // 脇が狭いときは、三方枠の範囲と枠からはみ出さないよう削る
            double minX = Math.max(-half, lamp.minX() - m);
            double maxX = Math.min(half, lamp.maxX() + m);
            if (lamp.minX() >= rightOuter) {
                minX = Math.max(minX, rightOuter);
            }
            if (lamp.maxX() <= leftOuter) {
                maxX = Math.min(maxX, leftOuter);
            }
            parts.add(new Part(Type.LANTERN_PLATE, box(minX, lamp.minY() - m, maxX, lamp.maxY() + m, face, face + PX)));
        }
        return parts;
    }

    /**
     * 敷居。出入口の幅で、戸の下の床と同じ高さに置き、戸のレールごとに溝の線を引く。
     * かごの敷居は袖壁の内側の面からかごの戸の外側まで、乗場の敷居は乗場の戸から front まで。
     */
    public static List<Part> sill(CarSpec spec, DoorGeometry.Side side, double front) {
        Box entrance = CarGeometry.entrance(spec);
        Box travel = DoorGeometry.travelBounds(spec, side);
        double near = side == DoorGeometry.Side.CAR ? CarGeometry.interior(spec).maxZ() : travel.minZ();
        double far = side == DoorGeometry.Side.CAR ? travel.maxZ() : front;
        List<Part> parts = new ArrayList<>();
        parts.add(new Part(Type.SILL, new Box(entrance.minX(), -PX, near, entrance.maxX(), LIFT, far)));
        TreeSet<Double> tracks = new TreeSet<>();
        for (Box panel : DoorGeometry.panels(spec, side, 0)) {
            tracks.add((panel.minZ() + panel.maxZ()) / 2);
        }
        for (double z : tracks) {
            parts.add(new Part(Type.SILL_GROOVE, new Box(entrance.minX(), LIFT, z - PX / 8, entrance.maxX(), 2 * LIFT, z + PX / 8)));
        }
        return parts;
    }

    /** 枠の外の壁を、ブロックの格子で分けたもの。 */
    public List<WallCell> wall() {
        List<WallCell> cells = new ArrayList<>();
        addWall(cells, -half, 0, leftOuter, TOP_PX);
        addWall(cells, rightOuter, 0, half, TOP_PX);
        addWall(cells, leftOuter, headTop, rightOuter, TOP_PX);
        return cells;
    }

    /** px で表した長方形 [x0, x1]×[y0, y1] の壁を、ブロックの格子（三方枠の左端と床が格子の線）で分けて加える。 */
    private void addWall(List<WallCell> cells, int x0, int y0, int x1, int y1) {
        for (int gx = -half; gx < x1; gx += BLOCK_PX) {
            int cx0 = Math.max(x0, gx);
            int cx1 = Math.min(x1, gx + BLOCK_PX);
            if (cx0 >= cx1) {
                continue;
            }
            for (int gy = 0; gy < y1; gy += BLOCK_PX) {
                int cy0 = Math.max(y0, gy);
                int cy1 = Math.min(y1, gy + BLOCK_PX);
                if (cy0 >= cy1) {
                    continue;
                }
                cells.add(new WallCell(box(cx0, cy0, cx1, cy1, back, face), (cx0 - gx) * PX, (cy0 - gy) * PX));
            }
        }
    }

    /** 階 floor（0始まり、全 floorCount 階）の乗場のボタン。上り、下り、車椅子用の上り、下りの順で、ない方は含めない。 */
    public List<Button> buttons(int floor, int floorCount) {
        List<Button> buttons = new ArrayList<>(4);
        addButtons(buttons, floor, floorCount, down, false);
        if (fixtures.wheelchairPanel()) {
            addButtons(buttons, floor, floorCount, down - 16, true);
        }
        return buttons;
    }

    private void addButtons(List<Button> buttons, int floor, int floorCount, int bottom, boolean wheelchair) {
        double left = column - ButtonLayout.BUTTON_PX / 2.0;
        double right = left + ButtonLayout.BUTTON_PX;
        if (floor < floorCount - 1) {
            int upBottom = bottom + ButtonLayout.BUTTON_PX + GAP_PX;
            buttons.add(new Button(Kind.HALL_UP, floor, box(left, upBottom, right, upBottom + ButtonLayout.BUTTON_PX,
                    surface, surface + ButtonLayout.PROTRUSION), Normal.POS_Z, wheelchair));
        }
        if (floor > 0) {
            buttons.add(new Button(Kind.HALL_DOWN, floor, box(left, bottom, right, bottom + ButtonLayout.BUTTON_PX,
                    surface, surface + ButtonLayout.PROTRUSION), Normal.POS_Z, wheelchair));
        }
    }

    /** 乗場の階床表示器。群管理向けでは付けないので null。 */
    public FloorIndicator.Frame indicator(String label, TravelDirection direction) {
        if (!fixtures.hasHallIndicator()) {
            return null;
        }
        if (fixtures.hallIndicator() == HallIndicator.OVER_BUTTONS) {
            return FloorIndicator.hallWindow(windowBox(), label, direction);
        }
        boolean fascia = landing.frame() == FrameStyle.FASCIA;
        int facePx = (int) Math.round((fascia ? frameFront() : face) * BLOCK_PX);
        return FloorIndicator.hallOverDoor(spec, overDoorIndicatorBottom(), facePx, label, direction, longLabels);
    }

    /** 扉の上の表示器の下端 [px]。幕板付きなら出入口の1px上、細枠なら枠の1px上。 */
    private int overDoorIndicatorBottom() {
        return landing.frame() == FrameStyle.FASCIA ? entranceTop + 1 : entranceTop + FRAME_PX + 1;
    }

    /**
     * ホールランタンの段。上の段（上り）、下の段（下り）の順。縦長で最上階・最下階なら、上下をつなげた1段だけ。付けないときは空。
     *
     * @param terminal 最上階または最下階か
     */
    public List<HallLantern.Section> lantern(boolean terminal) {
        Lamp lamp = lamp();
        if (lamp == null) {
            return List.of();
        }
        double z0 = lamp.z();
        double z1 = z0 + PX;
        if (lamp.vertical()) {
            if (terminal) {
                return List.of(new HallLantern.Section(HallLantern.BOTH, box(lamp.minX(), lamp.minY(), lamp.maxX(), lamp.maxY(), z0, z1)));
            }
            double middle = lamp.minY() + LANTERN_SECTION_PX;
            return List.of(
                    new HallLantern.Section(HallLantern.UP_ONLY, box(lamp.minX(), middle + 1, lamp.maxX(), lamp.maxY(), z0, z1)),
                    new HallLantern.Section(HallLantern.DOWN_ONLY, box(lamp.minX(), lamp.minY(), lamp.maxX(), middle, z0, z1)));
        }
        double middle = lamp.minY() + OVER_SECTION_PX;
        return List.of(
                new HallLantern.Section(HallLantern.UP_ONLY, box(lamp.minX(), middle, lamp.maxX(), lamp.maxY(), z0, z1)),
                new HallLantern.Section(HallLantern.DOWN_ONLY, box(lamp.minX(), lamp.minY(), lamp.maxX(), middle, z0, z1)));
    }

    /** ランプ全体の長方形 [px] と、ランプの裏の面の Z、背面プレートを付けるか。 */
    private record Lamp(double minX, double minY, double maxX, double maxY, double z, boolean plated, boolean vertical) {
    }

    private Lamp lamp() {
        int height = LANTERN_SECTION_PX * 2 + 1;
        boolean plate = landing.lanternPlate();
        double onWall = plate ? face + PX : face;
        return switch (fixtures.hallLantern()) {
            case NONE -> null;
            case BESIDE_DOOR -> {
                double maxX = leftOuter - PANEL_GAP_PX - (plate ? LANTERN_PLATE_MARGIN_PX : 0);
                yield new Lamp(maxX - LANTERN_WIDTH_PX, LANTERN_BOTTOM_PX, maxX, LANTERN_BOTTOM_PX + height, onWall, plate, true);
            }
            case RIGHT_OF_DOOR -> new Lamp(column - LANTERN_WIDTH_PX / 2.0, LANTERN_BOTTOM_PX, column + LANTERN_WIDTH_PX / 2.0,
                    LANTERN_BOTTOM_PX + height, embedded ? surface : onWall, plate && !embedded, true);
            case OVER_DOOR -> {
                if (landing.frame() == FrameStyle.FASCIA) {
                    boolean indicatorBelow = fixtures.hasHallIndicator() && fixtures.hallIndicator() == HallIndicator.OVER_DOOR;
                    int bottom = indicatorBelow ? overDoorIndicatorBottom() + FloorIndicator.OVER_DOOR_HEIGHT_PX + 1 : entranceTop + 1;
                    yield new Lamp(entranceMin, bottom, entranceMax, bottom + OVER_SECTION_PX * 2, frameFront(), false, false);
                }
                int bottom = entranceTop + FRAME_PX + 1 + LANTERN_PLATE_MARGIN_PX;
                yield new Lamp(entranceMin, bottom, entranceMax, bottom + OVER_SECTION_PX * 2, onWall, plate, false);
            }
        };
    }

    private Box windowBox() {
        int bottom = down + ButtonLayout.BUTTON_PX * 2 + GAP_PX * 2;
        return box(column - windowWidth / 2.0, bottom, column + windowWidth / 2.0, bottom + WINDOW_HEIGHT_PX, surface, surface + SURFACE);
    }

    private int plateBottom() {
        return fixtures.wheelchairPanel() ? down - 20 : down - 4;
    }

    private int plateTop() {
        int buttonsTop = down + ButtonLayout.BUTTON_PX * 2 + GAP_PX;
        return window ? buttonsTop + GAP_PX + WINDOW_HEIGHT_PX + 1 : buttonsTop + 1;
    }

    /** X・Y は px、Z はブロックで表した箱。 */
    private static Box box(double x0, double y0, double x1, double y1, double z0, double z1) {
        return new Box(x0 * PX, y0 * PX, z0, x1 * PX, y1 * PX, z1);
    }
}

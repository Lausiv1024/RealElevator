package net.lausiv1024.realelevator.core.display;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.IntFunction;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Normal;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.design.LandingLayout;
import net.lausiv1024.realelevator.core.display.FloorIndicator.Frame;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.CarPanelLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;

class FloorIndicatorTest {
    private static final double EPS = 1e-9;
    private static final double PX = 1.0 / 16;
    private static final FixtureConfig OVER_DOOR = FixtureConfig.DEFAULT.withHallIndicator(FixtureConfig.HallIndicator.OVER_DOOR);

    @Test
    void displayedFloorSwitchesAtTheMidpoint() {
        int[] floors = {64, 69, 74};
        assertEquals(0, FloorIndicator.displayedFloor(64, floors));
        assertEquals(0, FloorIndicator.displayedFloor(66.4, floors));
        assertEquals(1, FloorIndicator.displayedFloor(66.6, floors));
        assertEquals(2, FloorIndicator.displayedFloor(80, floors));
        assertEquals(0, FloorIndicator.displayedFloor(-100, floors));
    }

    @Test
    void defaultLabelsCountFromOne() {
        assertEquals("1", FloorIndicator.defaultLabel(0));
        assertEquals("12", FloorIndicator.defaultLabel(11));
    }

    @Test
    void fontCoversDigitsAndCommonFloorLetters() {
        for (char c : "0123456789BFGLMPR-".toCharArray()) {
            assertTrue(DotFont.supports(c), String.valueOf(c));
            boolean any = false;
            for (int y = 0; y < DotFont.HEIGHT; y++) {
                for (int x = 0; x < DotFont.WIDTH; x++) {
                    any |= DotFont.isLit(c, x, y);
                }
            }
            assertTrue(any, "blank glyph: " + c);
        }
        assertFalse(DotFont.supports('X'));
        assertFalse(DotFont.isLit('X', 0, 0));
        assertTrue(DotFont.supports('b'));
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void carIndicatorSitsOnTheJambAboveTheButtons(CarModel model) {
        Frame frame = FloorIndicator.car(model, 4, FixtureConfig.DEFAULT, "12", TravelDirection.UP).get(0);
        Box face = frame.face();
        Box interior = CarGeometry.interior(model);
        assertTrue(face.minX() >= interior.minX() - EPS && face.maxX() <= CarGeometry.entrance(model).minX() + EPS);
        // 主操作盤の板（袖壁から1px）の面の表示窓
        assertEquals(interior.maxZ() - PX, face.maxZ(), EPS);
        double buttonsTop = ButtonLayout.carPanel(model, 4).stream().mapToDouble(b -> b.box().maxY()).max().orElseThrow();
        assertTrue(face.minY() > buttonsTop);
        assertTrue(face.maxY() <= interior.maxY());
        // 0.5px のドット
        assertDotsInside(frame, PX / 2);
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void hallIndicatorIsCenteredAboveTheEntrance(CarModel model) {
        Frame frame = hall(model, OVER_DOOR, "B1", TravelDirection.DOWN);
        Box face = frame.face();
        assertEquals(-face.maxX(), face.minX(), EPS);
        assertTrue(face.minY() > CarGeometry.entrance(model).maxY());
        assertEquals(DoorGeometry.landingFace(model), face.minZ(), EPS);
        assertDotsInside(frame);
    }

    @Test
    void carIndicatorReadsLeftToRightFromInsideTheCar() {
        // かごの中から見ると左が +X なので、1文字目の方が +X 側にある
        List<Box> twelve = FloorIndicator.car(CarModel.P11, 2, FixtureConfig.DEFAULT, "12", TravelDirection.NONE).get(0).dots();
        List<Box> one = FloorIndicator.car(CarModel.P11, 2, FixtureConfig.DEFAULT, "1", TravelDirection.NONE).get(0).dots();
        double maxXOfOne = one.stream().mapToDouble(Box::maxX).max().orElseThrow();
        double twelveMax = twelve.stream().mapToDouble(Box::maxX).max().orElseThrow();
        double twelveMin = twelve.stream().mapToDouble(Box::minX).min().orElseThrow();
        assertTrue(twelveMax > maxXOfOne, "the first character should be on the +X side");
        assertTrue(twelveMin < one.stream().mapToDouble(Box::minX).min().orElseThrow());
    }

    @Test
    void hallIndicatorReadsLeftToRightFromTheLanding() {
        // 乗場から見ると左が −X なので、矢印が −X 側、階名が +X 側にある
        Frame withArrow = hall(CarModel.P11, OVER_DOOR, "", TravelDirection.UP);
        Frame withLabel = hall(CarModel.P11, OVER_DOOR, "8", TravelDirection.NONE);
        double arrowMax = withArrow.dots().stream().mapToDouble(Box::maxX).max().orElseThrow();
        double labelMin = withLabel.dots().stream().mapToDouble(Box::minX).min().orElseThrow();
        assertTrue(arrowMax < labelMin);
    }

    @Test
    void arrowAppearsOnlyWithADirection() {
        int none = hall(CarModel.P11, OVER_DOOR, "1", TravelDirection.NONE).dots().size();
        int up = hall(CarModel.P11, OVER_DOOR, "1", TravelDirection.UP).dots().size();
        int down = hall(CarModel.P11, OVER_DOOR, "1", TravelDirection.DOWN).dots().size();
        assertTrue(up > none);
        assertEquals(up, down);
        // 上向きの矢印は先端が上
        List<Box> arrow = hall(CarModel.P11, OVER_DOOR, "", TravelDirection.UP).dots();
        double top = arrow.stream().mapToDouble(Box::maxY).max().orElseThrow();
        assertEquals(1, arrow.stream().filter(b -> Math.abs(b.maxY() - top) < EPS).count());
    }

    @Test
    void longLabelsAreCutToTwoCharactersUnlessTheIndicatorIsWidened() {
        assertEquals(hall(CarModel.P11, OVER_DOOR, "12", TravelDirection.NONE).dots(),
                hall(CarModel.P11, OVER_DOOR, "123", TravelDirection.NONE).dots());
        assertTrue(FloorIndicator.hasLongLabel("1", "MB1"));
        assertFalse(FloorIndicator.hasLongLabel("1", "B1"));
    }

    /** 3文字の階名があるエレベーターでは、どの表示器も3文字を表示器の中に収める。 */
    @ParameterizedTest
    @EnumSource(CarModel.class)
    void threeCharacterLabelsFitInEveryIndicator(CarModel model) {
        CarSpec spec = CarSpec.of(model);
        FixtureConfig overButtons = FixtureConfig.DEFAULT.withHallIndicator(FixtureConfig.HallIndicator.OVER_BUTTONS);
        Frame two = LandingLayout.of(spec, OVER_DOOR, InteriorConfig.Landing.DEFAULT, 0, true).indicator("12", TravelDirection.UP);
        Frame three = LandingLayout.of(spec, OVER_DOOR, InteriorConfig.Landing.DEFAULT, 0, true).indicator("MB1", TravelDirection.UP);
        assertEquals(FloorIndicator.overDoorWidthPx(true) * PX, three.face().sizeX(), EPS);
        assertTrue(three.dots().size() > two.dots().size());
        assertDotsInside(three);
        assertDotsInside(LandingLayout.of(spec, overButtons, InteriorConfig.Landing.DEFAULT, 0, true).indicator("MB1", TravelDirection.UP), PX / 2);
        assertDotsInside(FloorIndicator.car(spec, 4, FixtureConfig.DEFAULT, "MB1", TravelDirection.UP).get(0), PX / 2);
        for (Frame wheelchair : FloorIndicator.wheelchair(spec, 4, "MB1", TravelDirection.UP)) {
            assertDotsInside(wheelchair, PX / 2);
        }
    }

    /** 表示窓の中の階名は、窓の幅の中央にちょうど置く（ドットの並びの端数で左に寄らない）。 */
    @ParameterizedTest
    @EnumSource(CarModel.class)
    void labelsAreCenteredInTheWindows(CarModel model) {
        FixtureConfig overButtons = FixtureConfig.DEFAULT.withHallIndicator(FixtureConfig.HallIndicator.OVER_BUTTONS);
        // 字形の両端が埋まっている文字で確かめる（「1」は左の列が空いている）
        for (String label : new String[] {"8", "M8", "88"}) {
            assertCentered(hall(model, overButtons, label, TravelDirection.NONE));
            assertCentered(FloorIndicator.car(model, 4, FixtureConfig.DEFAULT, label, TravelDirection.NONE).get(0));
        }
        assertCentered(LandingLayout.of(CarSpec.of(model), overButtons, InteriorConfig.Landing.DEFAULT, 0, true).indicator("MB1", TravelDirection.NONE));
    }

    /** 主操作盤の表示窓は、どの機種も同じ幅で、袖壁の幅の中央にある。 */
    @Test
    void carPanelWindowsHaveTheSameWidthForEveryModel() {
        for (CarModel model : CarModel.values()) {
            Box window = FloorIndicator.car(model, 4, FixtureConfig.DEFAULT, "1", TravelDirection.NONE).get(0).face();
            assertEquals(CarPanelLayout.windowWidthPx() * PX, window.sizeX(), EPS);
            CarSpec spec = CarSpec.of(model);
            double jambCenter = (ButtonLayout.jambMinPx(spec, true) + ButtonLayout.jambWidthPx(spec, true) / 2.0) * PX;
            assertEquals(jambCenter, (window.minX() + window.maxX()) / 2, EPS);
        }
        // 5×7 の2文字（0.5px のドットで11ドット）が入る
        assertTrue(CarPanelLayout.windowWidthPx() >= 6);
    }

    /** ドットで出すのは、ドットにある文字だけを先頭から3文字まで。 */
    @Test
    void dotsShowOnlySupportedCharactersUpToThree() {
        assertEquals("1", FloorIndicator.shown("改札1"));
        assertEquals("MB1", FloorIndicator.shown("MB12"));
        assertTrue(FloorIndicator.fitsDots("MB1"));
        assertFalse(FloorIndicator.fitsDots("MB12"));
        assertFalse(FloorIndicator.fitsDots("改札"));
    }

    /** フォントで階名を描く場所は、どの表示器でも表示面の中にあり、表示面と同じ向き。 */
    @ParameterizedTest
    @EnumSource(CarModel.class)
    void fontLabelAreasSitOnTheFaces(CarModel model) {
        CarSpec spec = CarSpec.of(model);
        FixtureConfig overButtons = FixtureConfig.DEFAULT.withHallIndicator(FixtureConfig.HallIndicator.OVER_BUTTONS);
        List<Frame> frames = new java.util.ArrayList<>();
        frames.add(hall(model, OVER_DOOR, "", TravelDirection.UP));
        frames.add(hall(model, overButtons, "", TravelDirection.UP));
        frames.addAll(FloorIndicator.car(spec, 4, FixtureConfig.DEFAULT, "", TravelDirection.UP));
        frames.addAll(FloorIndicator.wheelchair(spec, 4, "", TravelDirection.UP));
        for (Frame frame : frames) {
            Box face = frame.face();
            Box label = frame.label();
            assertTrue(label.minY() >= face.minY() - EPS && label.maxY() <= face.maxY() + EPS, face + " / " + label);
            assertTrue(label.sizeY() > 0);
            if (frame.normal().x() == 0) {
                assertTrue(label.minX() >= face.minX() - EPS && label.maxX() <= face.maxX() + EPS, face + " / " + label);
            } else {
                assertTrue(label.minZ() >= face.minZ() - EPS && label.maxZ() <= face.maxZ() + EPS, face + " / " + label);
            }
        }
        assertEquals(Normal.POS_Z, hall(model, OVER_DOOR, "", TravelDirection.UP).normal());
        assertEquals(Normal.NEG_Z, FloorIndicator.car(spec, 4, FixtureConfig.DEFAULT, "", TravelDirection.UP).get(0).normal());
    }

    private static void assertCentered(Frame frame) {
        double minX = frame.dots().stream().mapToDouble(Box::minX).min().orElseThrow();
        double maxX = frame.dots().stream().mapToDouble(Box::maxX).max().orElseThrow();
        assertEquals((frame.face().minX() + frame.face().maxX()) / 2, (minX + maxX) / 2, EPS, frame.face().toString());
    }

    @Test
    void leftPanelIndicatorNeedsBothPanels() {
        FixtureConfig rightOnly = FixtureConfig.DEFAULT.withLeftPanelIndicator(true);
        assertEquals(1, FloorIndicator.car(CarModel.P11, 4, rightOnly, "1", TravelDirection.NONE).size());

        FixtureConfig both = rightOnly.withPanelSides(FixtureConfig.PanelSides.BOTH);
        List<Frame> frames = FloorIndicator.car(CarModel.P11, 4, both, "1", TravelDirection.NONE);
        assertEquals(2, frames.size());
        // 左右対称の位置
        assertEquals(-frames.get(0).face().maxX(), frames.get(1).face().minX(), EPS);
        assertEquals(frames.get(0).face().minY(), frames.get(1).face().minY(), EPS);
        assertTrue(frames.get(1).face().minX() >= CarGeometry.entrance(CarModel.P11).maxX());

        FixtureConfig withoutLeft = both.withLeftPanelIndicator(false);
        assertEquals(1, FloorIndicator.car(CarModel.P11, 4, withoutLeft, "1", TravelDirection.NONE).size());
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void hallIndicatorOverTheButtonsSitsAboveThem(CarModel model) {
        FixtureConfig config = FixtureConfig.DEFAULT.withHallIndicator(FixtureConfig.HallIndicator.OVER_BUTTONS);
        Frame frame = hall(model, config, "2", TravelDirection.DOWN);
        Box face = frame.face();
        List<ButtonLayout.Button> buttons = LandingLayout.of(CarSpec.of(model), config, InteriorConfig.Landing.DEFAULT, 0).buttons(1, 3);
        double buttonsTop = buttons.stream().mapToDouble(b -> b.box().maxY()).max().orElseThrow();
        double buttonCenter = (buttons.get(0).box().minX() + buttons.get(0).box().maxX()) / 2;
        assertTrue(face.minY() > buttonsTop);
        assertEquals(buttonCenter, (face.minX() + face.maxX()) / 2, EPS);
        // 0.5px のドット
        assertDotsInside(frame, PX / 2);
    }

    private static Frame hall(CarModel model, FixtureConfig config, String label, TravelDirection direction) {
        return LandingLayout.of(CarSpec.of(model), config, InteriorConfig.Landing.DEFAULT, 0).indicator(label, direction);
    }

    private static void assertDotsInside(Frame frame) {
        assertDotsInside(frame, PX);
    }

    private static void assertDotsInside(Frame frame, double dotSize) {
        Box face = frame.face();
        assertFalse(frame.dots().isEmpty());
        for (Box dot : frame.dots()) {
            assertEquals(dotSize, Math.max(dot.sizeX(), dot.sizeZ()), EPS);
            assertEquals(dotSize, dot.sizeY(), EPS);
            assertTrue(dot.minY() >= face.minY() - EPS && dot.maxY() <= face.maxY() + EPS, dot.toString());
            boolean onX = dot.sizeX() < dot.sizeZ();
            if (onX) {
                // 側壁の表示器：ドットは X の向きに浮く
                assertTrue(dot.minZ() >= face.minZ() - EPS && dot.maxZ() <= face.maxZ() + EPS, dot.toString());
                assertTrue(dot.maxX() <= face.minX() + EPS || dot.minX() >= face.maxX() - EPS, dot.toString());
            } else {
                assertTrue(dot.minX() >= face.minX() - EPS && dot.maxX() <= face.maxX() + EPS, dot.toString());
                // ドットは表示面の手前（見る人の側）に浮いている
                assertTrue(dot.maxZ() <= face.minZ() + EPS || dot.minZ() >= face.maxZ() - EPS, dot.toString());
            }
        }
    }

    @Test
    void wheelchairDisplaysSitInTheBandOfBothPanels() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        List<Frame> frames = FloorIndicator.wheelchair(spec, 6, "12", TravelDirection.UP);
        assertEquals(2, frames.size());
        for (Frame frame : frames) {
            assertDotsInside(frame, PX / 2);
        }
        // 右の側壁（−X）のドットは +X の側、左の側壁（+X）のドットは −X の側に浮く
        assertTrue(frames.get(0).dots().get(0).minX() >= frames.get(0).face().maxX() - EPS);
        assertTrue(frames.get(1).dots().get(0).maxX() <= frames.get(1).face().minX() + EPS);
    }

    @Test
    void longGapsShowHyphensInTheMiddle() {
        int[] floors = {0, 5, 25, 30};
        IntFunction<String> labels = i -> String.valueOf(i + 1);
        // 普通の階間では、近いほうの階
        assertEquals("1", FloorIndicator.displayedLabel(2.4, floors, labels));
        assertEquals("2", FloorIndicator.displayedLabel(2.6, floors, labels));
        // 長い階間（20ブロック）は、どちらの階からも4ブロック以上離れたらハイフン
        assertEquals("2", FloorIndicator.displayedLabel(8.9, floors, labels));
        assertEquals("--", FloorIndicator.displayedLabel(9.0, floors, labels));
        assertEquals("--", FloorIndicator.displayedLabel(15, floors, labels));
        assertEquals("--", FloorIndicator.displayedLabel(21.0, floors, labels));
        assertEquals("3", FloorIndicator.displayedLabel(21.1, floors, labels));
        // 9ブロックの階間は長い階間ではない
        assertEquals("1", FloorIndicator.displayedLabel(4.4, new int[] {0, 9}, labels));
        assertEquals("2", FloorIndicator.displayedLabel(4.6, new int[] {0, 9}, labels));
    }
}

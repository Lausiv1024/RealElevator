package net.lausiv1024.realelevator.core.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Kind;

/** 車椅子用操作盤と乗場の車椅子用ボタンの配置。 */
class WheelchairLayoutTest {
    private static final double EPS = 1e-9;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;
    private static final int[] FLOOR_COUNTS = {2, 5, 10, 20, 30, 40};

    static Stream<CarSpec> specs() {
        List<CarSpec> specs = new ArrayList<>();
        for (CarModel model : CarModel.values()) {
            for (DoorType door : DoorType.values()) {
                if (door.isAvailable(model)) {
                    specs.add(new CarSpec(model, door));
                }
            }
        }
        return specs.stream();
    }

    @ParameterizedTest
    @MethodSource("specs")
    void thePanelSitsOnBothSideWallsInsideTheCar(CarSpec spec) {
        Box interior = CarGeometry.interior(spec);
        for (int floors : FLOOR_COUNTS) {
            List<Button> buttons = ButtonLayout.wheelchairPanel(spec, floors);
            assertEquals((floors + 2) * 2, buttons.size());
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                Box b = button.box();
                assertTrue(button.wheelchair());
                boolean right = i < buttons.size() / 2;
                // 側壁の内面から室内側へ1px
                assertEquals(right ? interior.minX() : interior.maxX() - PX, b.minX(), EPS, spec + " " + button);
                assertEquals(PX, b.sizeX(), EPS);
                // 奥行きの両端にすき間を空けて収まり、天井より下
                double margin = ButtonLayout.WHEELCHAIR_PANEL_MARGIN_PX * PX;
                assertTrue(b.minZ() >= interior.minZ() + margin - EPS && b.maxZ() <= interior.maxZ() - margin + EPS,
                        spec + " " + floors + " floors: " + button);
                assertTrue(b.maxY() <= interior.maxY(), spec + " " + floors + " floors");
                for (int j = 0; j < i; j++) {
                    assertFalse(b.intersects(buttons.get(j).box()), spec + " " + button + " / " + buttons.get(j));
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("specs")
    void thePanelIsCenteredAlongTheSideWall(CarSpec spec) {
        Box interior = CarGeometry.interior(spec);
        List<Button> buttons = ButtonLayout.wheelchairPanel(spec, 6);
        double minZ = buttons.stream().mapToDouble(b -> b.box().minZ()).min().orElseThrow();
        double maxZ = buttons.stream().mapToDouble(b -> b.box().maxZ()).max().orElseThrow();
        double center = (interior.minZ() + interior.maxZ()) / 2;
        assertEquals(center, (minZ + maxZ) / 2, PX);
    }

    @Test
    void openAndCloseAreAtTheDoorEndAndFloorsRunTowardsTheBack() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        List<Button> buttons = ButtonLayout.wheelchairPanel(spec, 5);
        Button open = buttons.get(0);
        Button close = buttons.get(1);
        assertEquals(Kind.OPEN, open.kind());
        assertEquals(Kind.CLOSE, close.kind());
        assertEquals(open.box().maxZ(), close.box().maxZ(), EPS);
        assertTrue(close.box().minY() > open.box().minY());
        for (Button button : buttons.subList(2, 7)) {
            assertTrue(button.box().maxZ() < open.box().minZ(), "floors are behind the open button: " + button);
        }
        // 2段：1階と2階が同じ列、3階はその奥
        assertEquals(buttons.get(2).box().minZ(), buttons.get(3).box().minZ(), EPS);
        assertTrue(buttons.get(3).box().minY() > buttons.get(2).box().minY());
        assertTrue(buttons.get(4).box().maxZ() < buttons.get(2).box().minZ());
        assertEquals(ButtonLayout.WHEELCHAIR_PANEL_BOTTOM_PX * PX, open.box().minY(), EPS);
    }

    @Test
    void manyFloorsAddRowsInsteadOfOverflowing() {
        CarSpec small = CarSpec.of(CarModel.P6);
        assertEquals(2, ButtonLayout.wheelchairPanelRows(small, 6));
        assertTrue(ButtonLayout.wheelchairPanelRows(small, 30) > 2);
    }

    @ParameterizedTest
    @MethodSource("specs")
    void wheelchairHallButtonsSitBelowTheUsualOnes(CarSpec spec) {
        List<Button> buttons = ButtonLayout.hallButtons(spec, 1, 3, 0, true);
        assertEquals(List.of(Kind.HALL_UP, Kind.HALL_DOWN, Kind.HALL_UP, Kind.HALL_DOWN), buttons.stream().map(Button::kind).toList());
        List<Button> usual = buttons.subList(0, 2);
        List<Button> wheelchair = buttons.subList(2, 4);
        assertEquals(ButtonLayout.hallButtons(spec, 1, 3, 0), usual);
        double usualBottom = usual.stream().mapToDouble(b -> b.box().minY()).min().orElseThrow();
        for (Button button : wheelchair) {
            assertTrue(button.wheelchair());
            assertTrue(button.box().maxY() < usualBottom, button.toString());
            assertEquals(usual.get(0).box().minX(), button.box().minX(), EPS);
            assertEquals(usual.get(0).box().minZ(), button.box().minZ(), EPS);
        }
        // 最上階・最下階は片方だけ
        assertEquals(List.of(Kind.HALL_UP, Kind.HALL_UP),
                ButtonLayout.hallButtons(spec, 0, 3, 0, true).stream().map(Button::kind).toList());
    }
}

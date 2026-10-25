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
import net.lausiv1024.realelevator.core.car.Normal;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Kind;

/** 車椅子用操作盤と乗場の車椅子用ボタンの配置。 */
class WheelchairLayoutTest {
    private static final double EPS = 1e-9;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;
    private static final int[] FLOOR_COUNTS = {2, 5, 10, 12, 20, 30, 40};

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

    private static Box plate(List<PanelPart> parts) {
        return parts.stream().filter(p -> p.type() == PanelPart.Type.PLATE).findFirst().orElseThrow().box();
    }

    @ParameterizedTest
    @MethodSource("specs")
    void thePanelSitsOnBothSideWallsInsideTheCar(CarSpec spec) {
        Box interior = CarGeometry.interior(spec);
        for (int floors : FLOOR_COUNTS) {
            WheelchairPanelLayout layout = WheelchairPanelLayout.of(spec, floors);
            List<Button> buttons = ButtonLayout.wheelchairPanel(spec, floors);
            assertEquals((floors + 3) * 2, buttons.size());
            for (int i = 0; i < buttons.size(); i++) {
                Button button = buttons.get(i);
                Box b = button.box();
                boolean right = i < buttons.size() / 2;
                Box plate = plate(layout.parts(spec, right));
                assertTrue(button.wheelchair());
                assertEquals(right ? Normal.POS_X : Normal.NEG_X, button.normal());
                // 側壁から1px浮かせた板の面から0.5px出っ張る
                assertEquals(right ? interior.minX() + PX : interior.maxX() - 1.5 * PX, b.minX(), EPS, spec + " " + button);
                // 板の中に収まる
                assertTrue(b.minZ() >= plate.minZ() - EPS && b.maxZ() <= plate.maxZ() + EPS
                        && b.minY() >= plate.minY() - EPS && b.maxY() <= plate.maxY() + EPS, spec + " " + floors + ": " + button);
                for (int j = 0; j < i; j++) {
                    assertFalse(b.intersects(buttons.get(j).box()), spec + " " + button + " / " + buttons.get(j));
                }
            }
            for (boolean right : new boolean[] {true, false}) {
                Box plate = plate(layout.parts(spec, right));
                double margin = WheelchairPanelLayout.MARGIN_PX * PX;
                assertTrue(plate.minZ() >= interior.minZ() + margin - EPS && plate.maxZ() <= interior.maxZ() - margin + EPS,
                        spec + " " + floors + " floors: " + plate);
                assertTrue(plate.maxY() <= WheelchairPanelLayout.MAX_TOP_PX * PX + EPS, spec + " " + floors + " floors");
                Box window = layout.window(spec, right);
                assertTrue(window.minZ() >= plate.minZ() - EPS && window.maxZ() <= plate.maxZ() + EPS && window.maxY() <= plate.maxY() + EPS);
            }
        }
    }

    /** 見る人（側壁を向いた人）から見て左から右への位置。 */
    private static double across(Button button, boolean right) {
        return right ? -button.box().minZ() : button.box().minZ();
    }

    @Test
    void floorsRunLeftToRightFromTheLowerRowAndTheBottomRowHasSosOpenAndClose() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        for (boolean right : new boolean[] {true, false}) {
            List<Button> all = ButtonLayout.wheelchairPanel(spec, 12);
            List<Button> buttons = right ? all.subList(0, all.size() / 2) : all.subList(all.size() / 2, all.size());
            Button open = buttons.get(0);
            Button close = buttons.get(1);
            Button sos = buttons.get(buttons.size() - 1);
            assertEquals(Kind.OPEN, open.kind());
            assertEquals(Kind.CLOSE, close.kind());
            assertEquals(Kind.EMERGENCY, sos.kind());
            // いちばん下の段：左に非常呼び、右に開・閉
            assertEquals(open.box().minY(), sos.box().minY(), EPS);
            assertTrue(across(sos, right) < across(open, right) && across(open, right) < across(close, right));
            // 2段で6列。1〜6が下の段で左から右、7〜12がその上
            Button first = buttons.get(2);
            Button sixth = buttons.get(7);
            Button seventh = buttons.get(8);
            assertEquals(first.box().minY(), sixth.box().minY(), EPS);
            assertTrue(across(first, right) < across(sixth, right));
            assertTrue(seventh.box().minY() > first.box().maxY());
            assertEquals(across(first, right), across(seventh, right), EPS);
            assertTrue(first.box().minY() > open.box().maxY());
        }
        assertEquals(6, WheelchairPanelLayout.of(spec, 12).columns());
        assertEquals(2, WheelchairPanelLayout.of(spec, 12).floorRows());
    }

    @Test
    void theMarkIsAtTheRightEndOfTheBand() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        WheelchairPanelLayout layout = WheelchairPanelLayout.of(spec, 6);
        for (boolean right : new boolean[] {true, false}) {
            List<PanelPart> parts = layout.parts(spec, right);
            Box mark = parts.stream().filter(p -> p.type() == PanelPart.Type.MARK).findFirst().orElseThrow().box();
            Box window = layout.window(spec, right);
            Box band = parts.stream().filter(p -> p.type() == PanelPart.Type.BAND).findFirst().orElseThrow().box();
            double markAcross = right ? -mark.minZ() : mark.minZ();
            double windowAcross = right ? -window.minZ() : window.minZ();
            assertTrue(markAcross > windowAcross, "the mark should be right of the display");
            assertTrue(mark.minY() >= band.minY() && mark.maxY() <= band.maxY());
            assertTrue(parts.stream().anyMatch(p -> p.type() == PanelPart.Type.MARK_SYMBOL));
        }
    }

    @Test
    void manyFloorsAddRowsAndShrinkTheButtonsWhenTooTall() {
        CarSpec small = CarSpec.of(CarModel.P6);
        assertEquals(3, WheelchairPanelLayout.of(small, 6).buttonPx());
        assertTrue(WheelchairPanelLayout.of(small, 12).floorRows() > 2);
        assertEquals(2, WheelchairPanelLayout.of(small, 40).buttonPx());
    }
}

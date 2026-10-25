package net.lausiv1024.realelevator.core.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

/** 主操作盤（袖壁の縦長の板）の配置。 */
class CarPanelLayoutTest {
    private static final double EPS = 1e-9;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;

    static Stream<CarSpec> specs() {
        return WheelchairLayoutTest.specs();
    }

    @Test
    void buttonsShrinkOnlyWhenLoweringThePanelIsNotEnough() {
        CarSpec wide = CarSpec.of(CarModel.P17);
        assertEquals(3, CarPanelLayout.of(wide, 2).buttonPx());
        assertEquals(CarPanelLayout.DEFAULT_BOTTOM_PX, CarPanelLayout.of(wide, 2).bottomPx());
        // 3px のまま、下端を下げて収める
        CarPanelLayout ten = CarPanelLayout.of(wide, 10);
        assertEquals(3, ten.buttonPx());
        assertTrue(ten.bottomPx() < CarPanelLayout.DEFAULT_BOTTOM_PX && ten.bottomPx() >= CarPanelLayout.MIN_BOTTOM_PX);
        // 下げても収まらなければ 2px
        assertEquals(2, CarPanelLayout.of(wide, 11).buttonPx());
        assertTrue(CarPanelLayout.of(wide, 16).hasEmergency());
        // さらに多ければ非常呼びを省く
        assertEquals(2, CarPanelLayout.of(wide, 18).buttonPx());
        assertFalse(CarPanelLayout.of(wide, 18).hasEmergency());
    }

    @Test
    void narrowJambsUseSmallButtons() {
        // 袖壁が 7〜8px の機種では、3px角を2列に並べると余白が残らないので、はじめから 2px角
        assertEquals(2, CarPanelLayout.of(CarSpec.of(CarModel.P11), 2).buttonPx());
        assertEquals(2, CarPanelLayout.of(CarSpec.of(CarModel.P13), 2).buttonPx());
        assertEquals(3, CarPanelLayout.of(new CarSpec(CarModel.P11, DoorType.SIDE_LEFT), 2).buttonPx());
    }

    @ParameterizedTest
    @MethodSource("specs")
    void buttonsFitOnThePlateBelowTheWindow(CarSpec spec) {
        for (int floors = 2; floors <= 18; floors++) {
            CarPanelLayout layout = CarPanelLayout.of(spec, floors);
            for (boolean right : ButtonLayout.panelJambs(spec, FixtureConfig.PanelSides.BOTH)) {
                List<Button> buttons = layout.buttons(spec, floors, right);
                assertEquals(floors + (layout.hasEmergency() ? 3 : 2), buttons.size());
                Box plate = layout.parts(spec, right, false).get(0).box();
                Box window = CarPanelLayout.window(spec, right);
                assertTrue(plate.maxY() <= CarModel.CEILING_HEIGHT_PX * PX);
                assertEquals(CarGeometry.interior(spec).maxZ(), plate.maxZ(), EPS);
                // 表示窓は板の中で、0.5px のドットで 5×7 の2文字（11ドット）が入る幅
                assertTrue(window.minX() >= plate.minX() && window.maxX() <= plate.maxX());
                assertTrue(window.sizeX() >= 5.5 * PX - EPS, spec + " window " + window.sizeX() * 16);
                for (int i = 0; i < buttons.size(); i++) {
                    Box b = buttons.get(i).box();
                    // 板の両脇に1px以上の余白を残す
                    assertTrue(b.minX() >= plate.minX() + PX - EPS && b.maxX() <= plate.maxX() - PX + EPS, spec + " " + floors + " " + buttons.get(i));
                    assertTrue(b.minY() >= CarPanelLayout.MIN_BOTTOM_PX * PX - EPS);
                    assertTrue(b.maxY() <= window.minY() - PX + EPS, spec + " " + floors + " floors: " + buttons.get(i));
                    assertEquals(plate.minZ(), b.maxZ(), EPS);
                    for (int j = 0; j < i; j++) {
                        assertFalse(b.intersects(buttons.get(j).box()), spec + " " + buttons.get(i) + " / " + buttons.get(j));
                    }
                }
            }
        }
    }

    @Test
    void theEmergencyButtonIsAboveTheFloorButtons() {
        List<Button> buttons = ButtonLayout.carPanel(CarModel.P11, 6);
        Button sos = buttons.get(buttons.size() - 1);
        assertEquals(Kind.EMERGENCY, sos.kind());
        for (Button button : buttons.subList(0, buttons.size() - 1)) {
            assertTrue(sos.box().minY() > button.box().maxY());
        }
    }
}

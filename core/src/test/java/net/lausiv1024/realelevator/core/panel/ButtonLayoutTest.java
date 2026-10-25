package net.lausiv1024.realelevator.core.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Kind;

class ButtonLayoutTest {
    private static final double EPS = 1e-12;
    private static final double PX = 1.0 / 16;

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void carPanelSitsOnTheRightJambInsideTheCar(CarModel model) {
        Box interior = CarGeometry.interior(model);
        Box entrance = CarGeometry.entrance(model);
        for (Button button : ButtonLayout.carPanel(model, 10)) {
            Box b = button.box();
            // 袖壁が狭い機種は 2px角
            double size = ButtonLayout.jambWidthPx(model) >= CarPanelLayout.MIN_JAMB_FOR_LARGE_PX ? 3 * PX : 2 * PX;
            assertEquals(size, b.sizeX(), EPS);
            assertEquals(size, b.sizeY(), EPS);
            // 袖壁から1px浮かせた板の面から、さらに0.5px出っ張る
            assertEquals(interior.maxZ() - PX, b.maxZ(), EPS);
            assertEquals(PX / 2, b.sizeZ(), EPS);
            // かごの中から見て右側（−X）の袖壁の範囲
            assertTrue(b.minX() >= interior.minX() && b.maxX() <= entrance.minX(), button.toString());
            assertTrue(b.maxY() <= interior.maxY());
        }
    }

    @Test
    void carPanelOrderAndColumns() {
        List<Button> buttons = ButtonLayout.carPanel(CarModel.P11, 4);
        assertEquals(List.of(Kind.OPEN, Kind.CLOSE, Kind.CAR_CALL, Kind.CAR_CALL, Kind.CAR_CALL, Kind.CAR_CALL, Kind.EMERGENCY),
                buttons.stream().map(Button::kind).toList());
        Button open = buttons.get(0);
        Button close = buttons.get(1);
        Button first = buttons.get(2);
        Button second = buttons.get(3);
        Button third = buttons.get(4);
        // かごの中から見て左（+X）に開と奇数階、右に閉と偶数階
        assertTrue(open.box().minX() > close.box().minX());
        assertEquals(open.box().minX(), first.box().minX(), EPS);
        assertEquals(close.box().minX(), second.box().minX(), EPS);
        // 開・閉が一番下で、1・2階が同じ段、3階はその上
        assertTrue(open.box().maxY() < first.box().minY());
        assertEquals(first.box().minY(), second.box().minY(), EPS);
        assertTrue(third.box().minY() > first.box().maxY());
        assertEquals(0, first.floor());
        assertEquals(1, second.floor());
    }

    @Test
    void carButtonsDoNotOverlap() {
        List<Button> buttons = ButtonLayout.carPanel(CarModel.P9, 12);
        for (int i = 0; i < buttons.size(); i++) {
            for (int j = i + 1; j < buttons.size(); j++) {
                assertFalse(buttons.get(i).box().intersects(buttons.get(j).box()));
            }
        }
    }

    @Test
    void bothPanelsMirrorOntoTheLeftJamb() {
        List<Button> right = ButtonLayout.carPanel(CarModel.P11, 3);
        List<Button> both = ButtonLayout.carPanel(CarModel.P11, 3, FixtureConfig.PanelSides.BOTH);
        assertEquals(right.size() * 2, both.size());
        assertEquals(right, both.subList(0, right.size()));
        for (int i = 0; i < right.size(); i++) {
            Button r = right.get(i);
            Button l = both.get(right.size() + i);
            assertEquals(r.kind(), l.kind());
            assertEquals(r.floor(), l.floor());
            assertEquals(r.box().minY(), l.box().minY(), EPS);
            assertTrue(l.box().minX() >= CarGeometry.entrance(CarModel.P11).maxX());
        }
        assertEquals(right.stream().mapToDouble(b -> b.box().maxY()).max().orElseThrow(),
                both.stream().mapToDouble(b -> b.box().maxY()).max().orElseThrow(), EPS);
    }
}

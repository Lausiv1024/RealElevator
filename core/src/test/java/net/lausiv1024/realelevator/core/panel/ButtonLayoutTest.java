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
            assertEquals(2 * PX, b.sizeX(), EPS);
            assertEquals(2 * PX, b.sizeY(), EPS);
            // 袖壁の内面から室内側へ1px
            assertEquals(interior.maxZ(), b.maxZ(), EPS);
            assertEquals(PX, b.sizeZ(), EPS);
            // かごの中から見て右側（−X）の袖壁の範囲
            assertTrue(b.minX() >= interior.minX() && b.maxX() <= entrance.minX(), button.toString());
            assertTrue(b.maxY() <= interior.maxY());
        }
    }

    @Test
    void carPanelOrderAndColumns() {
        List<Button> buttons = ButtonLayout.carPanel(CarModel.P11, 4);
        assertEquals(List.of(Kind.OPEN, Kind.CLOSE, Kind.CAR_CALL, Kind.CAR_CALL, Kind.CAR_CALL, Kind.CAR_CALL),
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

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void hallButtonsSitBesideTheLandingDoorOnTheLandingSide(CarModel model) {
        Box travel = DoorGeometry.travelBounds(model, DoorGeometry.Side.LANDING);
        List<Button> middle = ButtonLayout.hallButtons(model, 1, 3);
        assertEquals(List.of(Kind.HALL_UP, Kind.HALL_DOWN), middle.stream().map(Button::kind).toList());
        assertTrue(middle.get(0).box().minY() > middle.get(1).box().maxY());
        for (Button button : middle) {
            assertTrue(button.box().minX() > travel.maxX(), "overlaps the door pocket");
            assertEquals(travel.maxZ(), button.box().minZ(), EPS);
            assertEquals(1, button.floor());
        }
    }

    @Test
    void terminalFloorsHaveOnlyOneHallButton() {
        assertEquals(List.of(Kind.HALL_UP), ButtonLayout.hallButtons(CarModel.P11, 0, 2).stream().map(Button::kind).toList());
        assertEquals(List.of(Kind.HALL_DOWN), ButtonLayout.hallButtons(CarModel.P11, 1, 2).stream().map(Button::kind).toList());
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
        assertEquals(ButtonLayout.carPanelTopPx(3) / 16.0,
                both.stream().mapToDouble(b -> b.box().maxY()).max().orElseThrow(), EPS);
    }
}

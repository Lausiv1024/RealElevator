package net.lausiv1024.realelevator.core.door;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.design.InteriorConfig.SecurityWindow;

class DoorWindowsTest {
    private static final double EPS = 1e-9;
    private static final double PX = 1.0 / CarModel.PIXELS_PER_BLOCK;

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
    void windowIsAHoleInTheMiddleOfEveryPanel(CarSpec spec) {
        for (SecurityWindow window : SecurityWindow.values()) {
            if (window == SecurityWindow.NONE) {
                continue;
            }
            for (int turns = 0; turns < 4; turns++) {
                for (Box panel : DoorGeometry.panels(spec, DoorGeometry.Side.CAR, 0)) {
                    Box rotated = panel.rotateQuarterTurns(turns);
                    DoorWindows.Pieces pieces = DoorWindows.split(rotated, window);
                    Box glass = pieces.glass();
                    // 枠とガラスで戸板をちょうど埋める
                    double volume = glass.sizeX() * glass.sizeY() * glass.sizeZ();
                    for (Box piece : pieces.frame()) {
                        volume += piece.sizeX() * piece.sizeY() * piece.sizeZ();
                        assertFalse(piece.intersects(glass));
                        assertTrue(piece.sizeX() > 0 && piece.sizeY() > 0 && piece.sizeZ() > 0, piece.toString());
                    }
                    assertEquals(rotated.sizeX() * rotated.sizeY() * rotated.sizeZ(), volume, 1e-9);
                    // 左右の枠は同じ幅（窓は戸板の中央）
                    Box left = pieces.frame().get(0);
                    Box right = pieces.frame().get(1);
                    assertEquals(left.sizeX() * left.sizeZ(), right.sizeX() * right.sizeZ(), EPS);
                    assertEquals(rotated.maxY() - DoorWindows.TOP_PX * PX, glass.maxY(), EPS);
                    assertEquals(DoorWindows.heightPx(window) * PX, glass.sizeY(), EPS);
                }
            }
        }
    }

    @Test
    void sizesFollowTheRatios() {
        // 戸板10px（P-11の中央開き）：標準は約1/3で偶奇をそろえて4px、大型は約2/3で6px
        assertEquals(4, DoorWindows.widthPx(10, SecurityWindow.STANDARD));
        assertEquals(6, DoorWindows.widthPx(10, SecurityWindow.LARGE));
        assertEquals(6, DoorWindows.widthPx(10, SecurityWindow.EXTRA_LARGE));
        assertEquals(17, DoorWindows.heightPx(SecurityWindow.STANDARD));
        assertEquals(30, DoorWindows.heightPx(SecurityWindow.LARGE));
        assertEquals(40, DoorWindows.heightPx(SecurityWindow.EXTRA_LARGE));
        // 超大型でも戸の下端より上で終わる
        assertTrue(CarModel.ENTRANCE_HEIGHT_PX - DoorWindows.TOP_PX - DoorWindows.heightPx(SecurityWindow.EXTRA_LARGE) > 0);
    }

    @Test
    void noWindowKeepsThePanel() {
        Box panel = DoorGeometry.panels(CarSpec.of(CarModel.P11), DoorGeometry.Side.LANDING, 0.3).get(0);
        DoorWindows.Pieces pieces = DoorWindows.split(panel, SecurityWindow.NONE);
        assertEquals(List.of(panel), pieces.frame());
        assertNull(pieces.glass());
    }
}

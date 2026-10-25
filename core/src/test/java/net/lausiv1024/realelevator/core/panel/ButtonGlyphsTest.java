package net.lausiv1024.realelevator.core.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;

/** ボタンの面の図柄。 */
class ButtonGlyphsTest {
    private static final double EPS = 1e-9;

    private static List<Button> allButtons() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        List<Button> buttons = new ArrayList<>();
        buttons.addAll(ButtonLayout.carPanel(spec, 6, FixtureConfig.PanelSides.BOTH));
        buttons.addAll(ButtonLayout.carPanel(spec, 14, FixtureConfig.PanelSides.RIGHT));
        buttons.addAll(ButtonLayout.wheelchairPanel(spec, 8));
        buttons.addAll(ButtonLayout.hallButtons(spec, 1, 3, 0, true));
        for (Button button : ButtonLayout.carPanel(spec, 6, FixtureConfig.PanelSides.RIGHT)) {
            buttons.add(button.rotateQuarterTurns(2));
        }
        return buttons;
    }

    @Test
    void glyphsSitOnTheFrontFaceOfEachButton() {
        for (Button button : allButtons()) {
            ButtonGlyphs.Glyph glyph = ButtonGlyphs.of(button, "12");
            assertFalse(glyph.symbol().isEmpty(), button.toString());
            List<Box> dots = glyph.symbol();
            Box b = button.box();
            for (Box dot : dots) {
                assertTrue(dot.minY() >= b.minY() - EPS && dot.maxY() <= b.maxY() + EPS, button + " " + dot);
                switch (button.normal()) {
                    case POS_X -> assertTrue(dot.minX() >= b.maxX() - EPS && dot.minZ() >= b.minZ() - EPS && dot.maxZ() <= b.maxZ() + EPS);
                    case NEG_X -> assertTrue(dot.maxX() <= b.minX() + EPS && dot.minZ() >= b.minZ() - EPS && dot.maxZ() <= b.maxZ() + EPS);
                    case POS_Z -> assertTrue(dot.minZ() >= b.maxZ() - EPS && dot.minX() >= b.minX() - EPS && dot.maxX() <= b.maxX() + EPS);
                    case NEG_Z -> assertTrue(dot.maxZ() <= b.minZ() + EPS && dot.minX() >= b.minX() - EPS && dot.maxX() <= b.maxX() + EPS);
                }
            }
        }
    }

    @Test
    void openAndCloseAreMirroredAndTheTextReadsLeftToRight() {
        List<Button> panel = ButtonLayout.carPanel(CarModel.P11, 2);
        ButtonGlyphs.Glyph open = ButtonGlyphs.of(panel.get(0), "");
        ButtonGlyphs.Glyph close = ButtonGlyphs.of(panel.get(1), "");
        assertNotEquals(open.symbol().size(), 0);
        assertEquals(open.symbol().size(), close.symbol().size());
        // かごの中から見ると左が +X。「1」の縦棒は文字の中央より右（−X 寄り）にあるので、「12」の左の文字は +X 側に来る
        Button first = panel.get(2);
        List<Box> twelve = ButtonGlyphs.of(first, "12").symbol();
        double maxX = twelve.stream().mapToDouble(Box::maxX).max().orElseThrow();
        double minX = twelve.stream().mapToDouble(Box::minX).min().orElseThrow();
        long leftHalf = twelve.stream().filter(d -> d.minX() > (minX + maxX) / 2).count();
        long rightHalf = twelve.size() - leftHalf;
        // 「1」（7ドット）より「2」（11ドット）の方が多いので、見る人の右（−X）の半分の方が多い
        assertTrue(rightHalf > leftHalf, "left " + leftHalf + ", right " + rightHalf);
    }
}

package net.lausiv1024.realelevator.core.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.lausiv1024.realelevator.core.design.InteriorConfig.FrameStyle;
import net.lausiv1024.realelevator.core.design.InteriorConfig.Landing;
import net.lausiv1024.realelevator.core.design.InteriorConfig.LandingVariant;
import net.lausiv1024.realelevator.core.design.InteriorConfig.LandingWindow;
import net.lausiv1024.realelevator.core.design.InteriorConfig.Part;
import net.lausiv1024.realelevator.core.design.InteriorConfig.SecurityWindow;

/** 乗場の意匠の、階ごとの意匠と、かご・乗場で分けた防犯窓とドアの素材。 */
class InteriorConfigTest {
    private static final Landing FASCIA = Landing.DEFAULT.withFrame(FrameStyle.FASCIA).withSecurityWindow(LandingWindow.LARGE);

    @Test
    void floorsWithoutAVariantUseTheCommonLanding() {
        InteriorConfig config = InteriorConfig.DEFAULT.withLandingVariants(List.of(new LandingVariant(List.of(2, 0), FASCIA)));
        assertEquals(FASCIA, config.landingAt(0));
        assertEquals(Landing.DEFAULT, config.landingAt(1));
        assertEquals(FASCIA, config.landingAt(2));
        // 階は小さい順に並べ、重なりを除く
        assertEquals(List.of(0, 2), new LandingVariant(List.of(2, 0, 2), FASCIA).floors());
    }

    @Test
    void aFloorBelongsToOnlyOneVariant() {
        InteriorConfig config = InteriorConfig.DEFAULT.withLandingVariants(List.of(
                new LandingVariant(List.of(1), FASCIA), new LandingVariant(List.of(), Landing.DEFAULT)));
        // 1階目を意匠2へ移すと、意匠1からは外れる
        config = config.assignLandingFloor(1, 1);
        assertEquals(List.of(), config.landingVariants().get(0).floors());
        assertEquals(List.of(1), config.landingVariants().get(1).floors());
        // −1 で共通に戻す
        config = config.assignLandingFloor(1, -1);
        assertEquals(List.of(), config.landingVariants().get(1).floors());
    }

    @Test
    void addingAndRemovingStopsKeepsTheFloorsOfVariants() {
        InteriorConfig config = InteriorConfig.DEFAULT.withLandingVariants(List.of(new LandingVariant(List.of(1, 2), FASCIA)));
        // 高さ 0, 10, 20 の停止階に 5 を足すと、10 と 20 は2番目と3番目になる
        InteriorConfig added = config.remapLandingFloors(new int[] {0, 10, 20}, new int[] {0, 5, 10, 20});
        assertEquals(List.of(2, 3), added.landingVariants().get(0).floors());
        // 10 を外すと、20 だけが残る
        InteriorConfig removed = added.remapLandingFloors(new int[] {0, 5, 10, 20}, new int[] {0, 5, 20});
        assertEquals(List.of(2), removed.landingVariants().get(0).floors());
    }

    @Test
    void theLandingDoorFollowsTheCarDoorUnlessChosen() {
        InteriorConfig config = InteriorConfig.DEFAULT.withMaterial(Part.DOOR, "minecraft:oak_planks");
        assertEquals("minecraft:oak_planks", config.landingDoorMaterial(config.landing()));
        Landing own = config.landing().withMaterial(Part.LANDING_DOOR, "minecraft:spruce_planks");
        assertEquals("minecraft:spruce_planks", config.landingDoorMaterial(own));
        assertThrows(IllegalArgumentException.class, () -> config.landing().withMaterial(Part.WALL, "minecraft:stone"));
    }

    @Test
    void theLandingSecurityWindowFollowsTheCarUnlessChosen() {
        InteriorConfig config = InteriorConfig.DEFAULT.withSecurityWindow(SecurityWindow.STANDARD);
        // 既定は、かごに合わせる
        assertEquals(LandingWindow.SAME_AS_CAR, config.landing().securityWindow());
        assertEquals(SecurityWindow.STANDARD, config.landingWindow(config.landing()));
        // 別に選べば、かごによらない（窓なしも選べる）
        assertEquals(SecurityWindow.LARGE, config.landingWindow(FASCIA));
        assertEquals(SecurityWindow.NONE, config.landingWindow(Landing.DEFAULT.withSecurityWindow(LandingWindow.NONE)));
    }
}

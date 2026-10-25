package net.lausiv1024.realelevator.core.design;

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
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.design.InteriorGeometry.Part;
import net.lausiv1024.realelevator.core.design.InteriorGeometry.Type;
import net.lausiv1024.realelevator.core.panel.WheelchairPanelLayout;

class InteriorGeometryTest {
    private static final double EPS = 1e-9;
    private static final double PX = 1.0 / 16;

    private static List<Part> of(Type type, List<Part> parts) {
        return parts.stream().filter(p -> p.type() == type).toList();
    }

    @ParameterizedTest
    @EnumSource(CarModel.class)
    void everythingStaysInsideTheCar(CarModel model) {
        CarSpec spec = CarSpec.of(model);
        Box in = CarGeometry.interior(spec);
        for (InteriorConfig.CeilingType ceiling : InteriorConfig.CeilingType.values()) {
            InteriorConfig config = InteriorConfig.DEFAULT.withCeiling(ceiling).withHandrail(InteriorConfig.HandrailPlacement.SIDES_AND_BACK);
            for (Part part : InteriorGeometry.parts(spec, false, config)) {
                Box b = part.box();
                assertTrue(b.minX() >= in.minX() - EPS && b.maxX() <= in.maxX() + EPS
                        && b.minY() >= in.minY() - EPS && b.maxY() <= in.maxY() + EPS
                        && b.minZ() >= in.minZ() - EPS && b.maxZ() <= in.maxZ() + EPS, model + " " + part);
            }
        }
    }

    @Test
    void wallsAreSplitIntoTwoOrThreePanels() {
        assertEquals(1, InteriorGeometry.divisions(0, 20 * PX).size());
        assertEquals(2, InteriorGeometry.divisions(0, 32 * PX).size());
        // P-6（奥行き20px）の側壁は2枚、幅34pxの奥壁は3枚
        List<Part> parts = InteriorGeometry.parts(CarSpec.of(CarModel.P6), false, InteriorConfig.DEFAULT.withCeiling(InteriorConfig.CeilingType.FULL));
        long wallSeams = of(Type.SEAM, parts).stream().filter(p -> p.box().sizeY() > 1).count();
        assertEquals(1 + 1 + 2, wallSeams);
    }

    @Test
    void baseboardsRunAlongTheSideAndBackWalls() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        List<Part> baseboards = of(Type.BASEBOARD, InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT));
        assertEquals(3, baseboards.size());
        for (Part part : baseboards) {
            assertEquals(0, part.box().minY(), EPS);
            assertEquals(InteriorGeometry.BASEBOARD_PX * PX, part.box().maxY(), EPS);
            // 出っ張らない（面に重ねるだけ）
            assertTrue(Math.min(part.box().sizeX(), part.box().sizeZ()) < 0.01);
        }
        // 二方向出入口には奥壁がない
        assertEquals(2, of(Type.BASEBOARD, InteriorGeometry.parts(spec, true, InteriorConfig.DEFAULT)).size());
    }

    @Test
    void ceilingsHaveTheirLights() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        assertEquals(4, of(Type.CEILING_GLOW, InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT)).size());
        List<Part> indirect = InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT.withCeiling(InteriorConfig.CeilingType.INDIRECT));
        assertEquals(1, of(Type.CEILING_GLOW, indirect).size());
        assertEquals(4, of(Type.CEILING_PANEL, indirect).size());
        Box in = CarGeometry.interior(spec);
        Box full = of(Type.CEILING_GLOW, InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT.withCeiling(InteriorConfig.CeilingType.FULL)))
                .get(0).box();
        assertEquals(in.sizeX(), full.sizeX(), EPS);
        assertEquals(in.sizeZ(), full.sizeZ(), EPS);
        // 全面照明だけ、天井の継ぎ目がない
        assertTrue(of(Type.SEAM, InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT.withCeiling(InteriorConfig.CeilingType.FULL)))
                .stream().allMatch(p -> p.box().sizeY() > 1));
        assertEquals(2, of(Type.SEAM, indirect).stream().filter(p -> p.box().sizeY() < 1).count());
    }

    @Test
    void handrailsFollowTheSettingAndStayBelowTheWheelchairPanel() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        assertTrue(of(Type.HANDRAIL, InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT.withHandrail(InteriorConfig.HandrailPlacement.NONE))).isEmpty());
        List<Part> sides = of(Type.HANDRAIL, InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT));
        assertEquals(2, sides.size());
        assertEquals(3, of(Type.HANDRAIL, InteriorGeometry.parts(spec, false,
                InteriorConfig.DEFAULT.withHandrail(InteriorConfig.HandrailPlacement.SIDES_AND_BACK))).size());
        assertEquals(2, of(Type.HANDRAIL, InteriorGeometry.parts(spec, true,
                InteriorConfig.DEFAULT.withHandrail(InteriorConfig.HandrailPlacement.SIDES_AND_BACK))).size());
        for (Part rail : sides) {
            assertTrue(rail.box().maxY() <= (WheelchairPanelLayout.BOTTOM_PX - 1) * PX + EPS, "below the wheelchair panel plate");
        }
        assertEquals(6, of(Type.HANDRAIL_BRACKET, InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT)).size());
        assertFalse(of(Type.HANDRAIL_BRACKET, InteriorGeometry.parts(spec, false, InteriorConfig.DEFAULT)).get(0).box()
                .intersects(sides.get(0).box()));
    }

    @Test
    void observationGlassIsDividedByMullionsWithAHandrailInFront() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        List<Part> parts = InteriorGeometry.parts(spec, false, true, InteriorConfig.DEFAULT);
        List<Part> mullions = of(Type.MULLION, parts);
        // 奥壁（幅34px）は3枚に分ける
        assertEquals(2, mullions.size());
        double waist = CarGeometry.WAIST_PX * PX;
        for (Part mullion : mullions) {
            assertEquals(waist, mullion.box().minY(), EPS);
            assertEquals(PX, mullion.box().sizeX(), EPS);
        }
        // 奥壁の継ぎ目は腰壁の分だけ
        double backFace = CarGeometry.interior(spec).minZ();
        assertTrue(of(Type.SEAM, parts).stream().filter(p -> Math.abs(p.box().minZ() - backFace) < 0.01 && p.box().sizeY() > 0.1)
                .allMatch(p -> p.box().maxY() <= waist + EPS));
        // 手すりが両側の側壁だけの設定でも、ガラスの前に付ける
        assertEquals(3, of(Type.HANDRAIL, parts).size());
        assertTrue(of(Type.HANDRAIL, InteriorGeometry.parts(spec, false, true,
                InteriorConfig.DEFAULT.withHandrail(InteriorConfig.HandrailPlacement.NONE))).isEmpty());
        assertTrue(of(Type.MULLION, InteriorGeometry.parts(spec, false, false, InteriorConfig.DEFAULT)).isEmpty());
    }
}

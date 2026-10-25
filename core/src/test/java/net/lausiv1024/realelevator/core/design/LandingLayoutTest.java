package net.lausiv1024.realelevator.core.design;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.design.InteriorConfig.FrameStyle;
import net.lausiv1024.realelevator.core.design.InteriorConfig.Landing;
import net.lausiv1024.realelevator.core.design.InteriorConfig.PanelStyle;
import net.lausiv1024.realelevator.core.design.LandingLayout.Part;
import net.lausiv1024.realelevator.core.design.LandingLayout.Type;
import net.lausiv1024.realelevator.core.design.LandingLayout.WallCell;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.display.HallLantern;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Kind;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.core.panel.FixtureConfig.HallIndicator;
import net.lausiv1024.realelevator.core.panel.FixtureConfig.HallLanternPlacement;

class LandingLayoutTest {
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

    /** 乗場の意匠のすべての組み合わせ。 */
    private static List<Landing> landings() {
        List<Landing> landings = new ArrayList<>();
        for (FrameStyle frame : FrameStyle.values()) {
            for (PanelStyle panel : PanelStyle.values()) {
                for (boolean plate : new boolean[] {true, false}) {
                    landings.add(Landing.DEFAULT.withFrame(frame).withPanel(panel).withLanternPlate(plate));
                }
            }
        }
        return landings;
    }

    /** 乗場の器具の配置のすべての組み合わせ。 */
    private static List<FixtureConfig> fixtures() {
        List<FixtureConfig> configs = new ArrayList<>();
        for (HallIndicator indicator : HallIndicator.values()) {
            for (HallLanternPlacement lantern : HallLanternPlacement.values()) {
                for (boolean wheelchair : new boolean[] {true, false}) {
                    for (boolean group : new boolean[] {true, false}) {
                        configs.add(FixtureConfig.DEFAULT.withHallIndicator(indicator).withHallLantern(lantern)
                                .withWheelchairPanel(wheelchair).withGroupControl(group));
                    }
                }
            }
        }
        return configs;
    }

    private static LandingLayout layout(CarSpec spec, FixtureConfig fixtures, Landing landing) {
        return LandingLayout.of(spec, fixtures, landing, 0);
    }

    /** 器具（ボタン、表示器、ランタン、操作盤の部品、枠）の箱。重なりを調べる対象。 */
    private static List<Box> fixtureBoxes(LandingLayout layout) {
        List<Box> boxes = new ArrayList<>();
        for (Part part : layout.parts()) {
            if (part.type() != Type.MARK_SYMBOL && part.type() != Type.KEY_SLOT && part.type() != Type.SILL
                    && part.type() != Type.SILL_GROOVE) {
                boxes.add(part.box());
            }
        }
        layout.buttons(1, 3).forEach(b -> boxes.add(b.box()));
        FloorIndicator.Frame indicator = layout.indicator("12", TravelDirection.UP);
        // 操作盤の表示器は表示窓（WINDOW）と同じ箱
        if (indicator != null && !boxes.contains(indicator.face())) {
            boxes.add(indicator.face());
        }
        layout.lantern(false).forEach(s -> boxes.add(s.box()));
        return boxes;
    }

    @ParameterizedTest
    @MethodSource("specs")
    void everythingFitsTheFrameRangeWithoutOverlapping(CarSpec spec) {
        double half = DoorGeometry.landingFrameHalfWidth(spec);
        Box entrance = CarGeometry.entrance(spec);
        for (Landing landing : landings()) {
            for (FixtureConfig config : fixtures()) {
                LandingLayout layout = layout(spec, config, landing);
                String name = landing + " " + layout.fixtures();
                List<Box> boxes = fixtureBoxes(layout);
                layout.lantern(true).forEach(s -> boxes.add(s.box()));
                for (Box box : boxes) {
                    assertTrue(box.minX() >= -half - EPS && box.maxX() <= half + EPS && box.minY() >= -EPS
                            && box.maxY() <= DoorGeometry.LANDING_FRAME_HEIGHT + EPS, name + ": " + box);
                    // 出入口の前にはかからない
                    boolean overEntrance = box.maxX() > entrance.minX() + EPS && box.minX() < entrance.maxX() - EPS
                            && box.minY() < entrance.maxY() - EPS;
                    assertFalse(overEntrance, name + ": " + box);
                }
                List<Box> pairs = fixtureBoxes(layout);
                for (int i = 0; i < pairs.size(); i++) {
                    for (int j = i + 1; j < pairs.size(); j++) {
                        assertFalse(pairs.get(i).intersects(pairs.get(j)), name + ": " + pairs.get(i) + " / " + pairs.get(j));
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @MethodSource("specs")
    void wallFillsTheFrameRangeOutsideTheFrameBlockByBlock(CarSpec spec) {
        double half = DoorGeometry.landingFrameHalfWidth(spec);
        Box entrance = CarGeometry.entrance(spec);
        for (Landing landing : landings()) {
            LandingLayout layout = layout(spec, FixtureConfig.DEFAULT, landing);
            List<Box> frame = layout.parts().stream().filter(p -> p.type() == Type.FRAME).map(Part::box).toList();
            double area = entrance.sizeX() * entrance.maxY();
            for (Box box : frame) {
                area += box.sizeX() * box.sizeY();
            }
            for (WallCell cell : layout.wall()) {
                Box box = cell.box();
                area += box.sizeX() * box.sizeY();
                // 1ブロックの中に収まり、u・v はそのブロックの左下からの位置
                double blockX = Math.floor(box.minX() + half + EPS) - half;
                double blockY = Math.floor(box.minY() + EPS);
                assertEquals(box.minX() - blockX, cell.u(), EPS);
                assertEquals(box.minY() - blockY, cell.v(), EPS);
                assertTrue(box.maxX() <= blockX + 1 + EPS && box.maxY() <= blockY + 1 + EPS, box.toString());
                assertEquals(DoorGeometry.landingFace(spec), box.maxZ(), EPS);
                for (Box f : frame) {
                    assertFalse(box.intersects(f), box + " / " + f);
                }
            }
            // 壁と枠と出入口で、三方枠の範囲をちょうど埋める
            assertEquals(half * 2 * DoorGeometry.LANDING_FRAME_HEIGHT, area, 1e-6, landing.toString());
        }
    }

    @Test
    void frameIsThinAndStandsOutOfTheWall() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        Box entrance = CarGeometry.entrance(spec);
        List<Box> frame = layout(spec, FixtureConfig.DEFAULT, Landing.DEFAULT).parts().stream()
                .filter(p -> p.type() == Type.FRAME).map(Part::box).toList();
        assertEquals(3, frame.size());
        assertEquals(LandingLayout.FRAME_PX * PX, frame.get(0).sizeX(), EPS);
        assertEquals(entrance.minX(), frame.get(0).maxX(), EPS);
        assertEquals(DoorGeometry.landingFace(spec) + PX, frame.get(0).maxZ(), EPS);
        assertEquals(entrance.maxY() + LandingLayout.FRAME_PX * PX, frame.get(2).maxY(), EPS);
        // 幕板付きは上の部分が三方枠の範囲の上端まで、埋め込みは右の枠が幅広
        Landing fascia = Landing.DEFAULT.withFrame(FrameStyle.FASCIA).withPanel(PanelStyle.EMBEDDED);
        List<Box> wide = layout(spec, FixtureConfig.DEFAULT, fascia).parts().stream()
                .filter(p -> p.type() == Type.FRAME).map(Part::box).toList();
        assertEquals(DoorGeometry.LANDING_FRAME_HEIGHT, wide.get(2).maxY(), EPS);
        assertEquals(LandingLayout.EMBEDDED_FRAME_PX * PX, wide.get(1).sizeX(), EPS);
    }

    @ParameterizedTest
    @MethodSource("specs")
    void buttonsSitInOneColumnRightOfTheFrame(CarSpec spec) {
        for (PanelStyle panel : PanelStyle.values()) {
            LandingLayout layout = layout(spec, FixtureConfig.DEFAULT.withWheelchairPanel(true), Landing.DEFAULT.withPanel(panel));
            List<Button> buttons = layout.buttons(1, 3);
            assertEquals(List.of(Kind.HALL_UP, Kind.HALL_DOWN, Kind.HALL_UP, Kind.HALL_DOWN), buttons.stream().map(Button::kind).toList());
            assertTrue(buttons.get(0).box().minY() > buttons.get(1).box().maxY());
            double usualBottom = buttons.get(1).box().minY();
            for (Button button : buttons) {
                assertEquals(buttons.get(0).box().minX(), button.box().minX(), EPS);
                assertTrue(button.box().minX() > CarGeometry.entrance(spec).maxX());
                assertEquals(1, button.floor());
            }
            for (Button button : buttons.subList(2, 4)) {
                assertTrue(button.wheelchair());
                assertTrue(button.box().maxY() < usualBottom);
            }
            // 最上階・最下階は片方だけ
            assertEquals(List.of(Kind.HALL_UP, Kind.HALL_UP), layout.buttons(0, 3).stream().map(Button::kind).toList());
            assertEquals(List.of(Kind.HALL_DOWN, Kind.HALL_DOWN), layout.buttons(2, 3).stream().map(Button::kind).toList());
        }
    }

    @Test
    void wallMountedPanelHasItsWindowOnTopAndKeySwitchAtTheBottom() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        LandingLayout layout = layout(spec, FixtureConfig.DEFAULT.withWheelchairPanel(true), Landing.DEFAULT);
        List<Part> parts = layout.parts();
        Box plate = only(parts, Type.PLATE);
        Box window = only(parts, Type.WINDOW);
        Box key = only(parts, Type.KEY);
        Box mark = only(parts, Type.MARK);
        List<Button> buttons = layout.buttons(1, 3);
        assertEquals(LandingLayout.PANEL_WIDTH_PX * PX, plate.sizeX(), EPS);
        // 枠から1px空ける
        assertEquals(CarGeometry.entrance(spec).maxX() + (LandingLayout.FRAME_PX + 1) * PX, plate.minX(), EPS);
        assertTrue(window.minY() > buttons.get(0).box().maxY());
        assertTrue(mark.maxY() < buttons.get(1).box().minY() && mark.minY() > buttons.get(2).box().maxY());
        assertTrue(key.maxY() < buttons.get(3).box().minY());
        for (Box box : List.of(window, key, mark)) {
            assertTrue(box.minY() >= plate.minY() && box.maxY() <= plate.maxY(), box.toString());
        }
        // 表示器は表示窓に、0.5px のドットで描く
        FloorIndicator.Frame indicator = layout.indicator("12", TravelDirection.UP);
        assertEquals(window, indicator.face());
        assertTrue(indicator.dots().stream().allMatch(d -> Math.abs(d.sizeY() - PX / 2) < EPS));
    }

    @Test
    void groupControlHasNoIndicatorAndCanPutTheLanternOverThePanel() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        FixtureConfig group = FixtureConfig.DEFAULT.withGroupControl(true).withHallLantern(HallLanternPlacement.RIGHT_OF_DOOR);
        LandingLayout layout = layout(spec, group, Landing.DEFAULT);
        assertNull(layout.indicator("1", TravelDirection.UP));
        assertTrue(layout.parts().stream().noneMatch(p -> p.type() == Type.WINDOW));
        List<HallLantern.Section> lantern = layout.lantern(false);
        Box plate = only(layout.parts(), Type.PLATE);
        Box button = layout.buttons(1, 3).get(0).box();
        for (HallLantern.Section section : lantern) {
            assertTrue(section.box().minY() > plate.maxY());
            assertTrue(section.box().minX() > button.minX() && section.box().maxX() < button.maxX());
        }
        // 表示窓がない分、ボタンは低い
        assertEquals(LandingLayout.DOWN_WITHOUT_WINDOW_PX * PX, layout.buttons(1, 3).get(1).box().minY(), EPS);
        // 群管理向けでなければ、扉の右は選べない
        assertEquals(HallLanternPlacement.BESIDE_DOOR, layout(spec, group.withGroupControl(false), Landing.DEFAULT).fixtures().hallLantern());
    }

    @Test
    void thinFrameCannotHoldBothTheIndicatorAndTheLanternOverTheDoor() {
        FixtureConfig both = FixtureConfig.DEFAULT.withHallIndicator(HallIndicator.OVER_DOOR).withHallLantern(HallLanternPlacement.OVER_DOOR);
        assertEquals(HallLanternPlacement.BESIDE_DOOR, LandingLayout.fit(both, Landing.DEFAULT).hallLantern());
        Landing fascia = Landing.DEFAULT.withFrame(FrameStyle.FASCIA);
        assertEquals(HallLanternPlacement.OVER_DOOR, LandingLayout.fit(both, fascia).hallLantern());
        assertEquals(List.of(HallLanternPlacement.NONE, HallLanternPlacement.BESIDE_DOOR),
                LandingLayout.lanternChoices(both, Landing.DEFAULT, true));
        assertEquals(List.of(HallLanternPlacement.NONE, HallLanternPlacement.BESIDE_DOOR, HallLanternPlacement.OVER_DOOR),
                LandingLayout.lanternChoices(both, fascia, true));
        assertEquals(List.of(HallLanternPlacement.NONE, HallLanternPlacement.BESIDE_DOOR, HallLanternPlacement.RIGHT_OF_DOOR),
                LandingLayout.lanternChoices(both.withGroupControl(true), fascia, false));
        // 群管理向けなら表示器はないので、細枠でも扉の上にランタンを置ける
        assertEquals(HallLanternPlacement.OVER_DOOR, LandingLayout.fit(both.withGroupControl(true), Landing.DEFAULT).hallLantern());
    }

    @Test
    void overTheDoorStacksTheLanternAboveTheIndicatorOnTheFascia() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        Box entrance = CarGeometry.entrance(spec);
        FixtureConfig both = FixtureConfig.DEFAULT.withHallIndicator(HallIndicator.OVER_DOOR).withHallLantern(HallLanternPlacement.OVER_DOOR);
        LandingLayout layout = layout(spec, both, Landing.DEFAULT.withFrame(FrameStyle.FASCIA));
        Box indicator = layout.indicator("1", TravelDirection.UP).face();
        List<HallLantern.Section> sections = layout.lantern(true);
        assertEquals(2, sections.size());
        assertEquals(Set.of(TravelDirection.UP), sections.get(0).directions());
        assertTrue(sections.get(1).box().minY() > indicator.maxY());
        assertEquals(entrance.minX(), sections.get(0).box().minX(), EPS);
        assertEquals(entrance.maxX(), sections.get(0).box().maxX(), EPS);
        // 幕板の面に付け、背面プレートは付けない
        assertEquals(DoorGeometry.landingFace(spec) + PX, indicator.minZ(), EPS);
        assertTrue(layout.parts().stream().noneMatch(p -> p.type() == Type.LANTERN_PLATE));

        // 細枠では、表示器は枠の上の壁に付ける
        Box thin = layout(spec, both, Landing.DEFAULT).indicator("1", TravelDirection.UP).face();
        assertTrue(thin.minY() > entrance.maxY() + LandingLayout.FRAME_PX * PX);
        assertEquals(DoorGeometry.landingFace(spec), thin.minZ(), EPS);
    }

    @Test
    void besideTheDoorIsAPlatedVerticalBarLeftOfTheFrame() {
        CarSpec spec = CarSpec.of(CarModel.P11);
        FixtureConfig config = FixtureConfig.DEFAULT.withHallLantern(HallLanternPlacement.BESIDE_DOOR);
        LandingLayout layout = layout(spec, config, Landing.DEFAULT);
        List<HallLantern.Section> middle = layout.lantern(false);
        Box up = middle.get(0).box();
        Box down = middle.get(1).box();
        assertTrue(up.sizeY() > up.sizeX() && up.minY() > down.maxY());
        Box plate = only(layout.parts(), Type.LANTERN_PLATE);
        double frameLeft = CarGeometry.entrance(spec).minX() - LandingLayout.FRAME_PX * PX;
        assertEquals(frameLeft - PX, plate.maxX(), EPS);
        assertEquals(up.minX() - LandingLayout.LANTERN_PLATE_MARGIN_PX * PX, plate.minX(), EPS);
        assertEquals(plate.maxZ(), up.minZ(), EPS);
        // 最上階・最下階では上下をつなげた1本
        List<HallLantern.Section> terminal = layout.lantern(true);
        assertEquals(1, terminal.size());
        assertEquals(down.minY(), terminal.get(0).box().minY(), EPS);
        assertEquals(up.maxY(), terminal.get(0).box().maxY(), EPS);
        // プレートなしなら壁に直に付ける
        LandingLayout bare = layout(spec, config, Landing.DEFAULT.withLanternPlate(false));
        assertTrue(bare.parts().stream().noneMatch(p -> p.type() == Type.LANTERN_PLATE));
        assertEquals(DoorGeometry.landingFace(spec), bare.lantern(false).get(0).box().minZ(), EPS);
        assertTrue(layout(spec, FixtureConfig.DEFAULT, Landing.DEFAULT).lantern(false).isEmpty());
    }

    @ParameterizedTest
    @MethodSource("specs")
    void sillsLieFlushWithTheFloorUnderTheDoors(CarSpec spec) {
        Box entrance = CarGeometry.entrance(spec);
        for (DoorGeometry.Side side : DoorGeometry.Side.values()) {
            List<Part> parts = LandingLayout.sill(spec, side, DoorGeometry.landingFace(spec) + PX);
            Box sill = only(parts, Type.SILL);
            assertEquals(0, sill.maxY(), 0.01);
            assertEquals(entrance.minX(), sill.minX(), EPS);
            assertEquals(entrance.maxX(), sill.maxX(), EPS);
            assertEquals(spec.door().tracks(), parts.stream().filter(p -> p.type() == Type.SILL_GROOVE).count());
            Box travel = DoorGeometry.travelBounds(spec, side);
            for (Part groove : parts) {
                assertTrue(groove.box().minZ() >= travel.minZ() && groove.box().maxZ() <= travel.maxZ() || groove.type() == Type.SILL);
            }
            // 多光軸ビームのすき間には敷居を置かない
            assertFalse(sill.intersects(DoorGeometry.beamZone(spec).move(0, -0.5, 0)));
        }
    }

    private static Box only(List<Part> parts, Type type) {
        List<Box> boxes = parts.stream().filter(p -> p.type() == type).map(Part::box).toList();
        assertEquals(1, boxes.size(), type.toString());
        return boxes.get(0);
    }
}

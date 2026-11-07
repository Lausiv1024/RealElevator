package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.design.LandingLayout;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.display.HallLantern;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.CarShapes;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/**
 * 乗場の描画。扉、枠と枠のまわりの壁、敷居、操作盤、階床表示器、ホールランタン（配置は {@link LandingLayout}）。
 *
 * <p>エンティティの位置は昇降路の中なので、明るさは乗場の側（出入口の前）のものを使う。
 */
public class LandingDoorRenderer extends EntityRenderer<LandingDoorEntity> {

    public LandingDoorRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(LandingDoorEntity door, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        int light = landingLight(door);
        int turns = door.quarterTurns();
        // かごが読み込まれていなければ、初期値の意匠で、表示器は消灯したままにする
        ElevatorCarEntity car = door.car();
        InteriorConfig interior = car != null ? car.interior() : InteriorConfig.DEFAULT;
        LandingLayout layout = door.layout();
        FixtureConfig fixtures = layout.fixtures();
        int rgb = fixtures.indicatorColor().rgb();

        // この階の乗場の意匠（階ごとの意匠があればそれ）
        InteriorConfig.Landing landing = door.landingDesign();
        TextureAtlasSprite doorSprite = MaterialSprites.side(interior.landingDoorMaterial(landing));
        double fraction = door.renderFraction(partialTick);
        List<Box> glass = new ArrayList<>();
        for (Box panel : CarShapes.doorPanels(door.spec(), door.facing(), DoorGeometry.Side.LANDING, fraction, door.entrance())) {
            BoxRenderer.renderDoorPanel(pose, consumer, panel, doorSprite, light, interior.landingWindow(landing), glass);
        }
        // 戸の上の、三方枠の張り出し
        TextureAtlasSprite frameSprite = MaterialSprites.side(landing.frameMaterial());
        for (Box soffit : DoorGeometry.soffits(door.spec(), DoorGeometry.Side.LANDING)) {
            BoxRenderer.renderBox(pose, consumer, soffit.rotateQuarterTurns(turns), frameSprite, light);
        }
        // 片開きの、戸袋と反対側の戸当たり
        for (Box jamb : DoorGeometry.jambReturns(door.spec(), DoorGeometry.Side.LANDING)) {
            BoxRenderer.renderBox(pose, consumer, jamb.rotateQuarterTurns(turns), frameSprite, light);
        }
        BoxRenderer.renderLanding(pose, consumer, layout.parts(), turns, light, frameSprite,
                MaterialSprites.side(BoxRenderer.STAINLESS));
        Box range = LandingLayout.bounds(door.spec(), door.faceOffset());
        Box wallRange = new Box(range.minX(), 0, range.minZ(), range.maxX(), DoorGeometry.LANDING_FRAME_HEIGHT, range.maxZ());
        ChameleonWall.Look wallLook = InteriorConfig.AUTO.equals(landing.wallMaterial()) ? ChameleonWall.look(door)
                : ChameleonWall.Look.of(landing.wallMaterial());
        // 半透明のブロック（色付きガラスなど）の壁は、ほかを描き終えてから半透明で描く
        if (!wallLook.translucent()) {
            BoxRenderer.renderWall(pose, consumer, layout.wall(), wallRange, turns, light, wallLook.sprite(), wallLook.rgb());
        }
        if (wallLook.seeThrough()) {
            // 透明な壁では、壁に付けた器具の裏の筐体が見える
            TextureAtlasSprite galvanized = BoxRenderer.blockSprite(BoxRenderer.GALVANIZED);
            for (Box housing : layout.housings()) {
                BoxRenderer.renderBox(pose, consumer, housing.rotateQuarterTurns(turns), galvanized, light);
            }
        }

        BoxRenderer.renderButtons(pose, consumer, door, turns, light, rgb);
        // フォントで描く設定なら、階名はドットで描かずに、あとでフォントで描く
        List<WorldText.OnFace> fontTexts = fixtures.indicatorStyle() == FixtureConfig.IndicatorStyle.FONT ? new ArrayList<>() : null;
        String label = car != null ? car.indicatorLabel(car.renderTick(partialTick)) : "";
        FloorIndicator.Frame indicator = car != null
                ? layout.indicator(fontTexts != null ? "" : label, car.indicatorDirection())
                : layout.indicator("", TravelDirection.NONE);
        if (indicator != null) {
            BoxRenderer.renderIndicator(pose, consumer, indicator, turns, light, rgb, fontTexts, label);
        }
        // 点灯する段は、点滅の消灯中は消す。群管理で割り当てた（まだ着いていない）呼びの段は、点滅せずに点ける
        Set<TravelDirection> blinking = door.litLanternSections();
        Set<TravelDirection> lit = EnumSet.noneOf(TravelDirection.class);
        if (car != null && HallLantern.isBlinkOn(car.lanternTicks(car.renderTick(partialTick)))) {
            lit.addAll(blinking);
        }
        for (TravelDirection direction : door.forecastLanternSections()) {
            if (!blinking.contains(direction)) {
                lit.add(direction);
            }
        }
        boolean terminal = car != null && door.floorIndex() >= 0 && car.isTerminalFloor(door.floorIndex());
        BoxRenderer.renderLantern(pose, consumer, layout.lantern(terminal), lit, turns, light, rgb);
        if (wallLook.translucent()) {
            BoxRenderer.renderWall(pose, bufferSource.getBuffer(Sheets.translucentCullBlockSheet()), layout.wall(), wallRange, turns, light,
                    wallLook.sprite(), wallLook.rgb());
        }
        // 防犯窓のガラスは半透明なので、最後に描く
        if (!glass.isEmpty()) {
            BoxRenderer.renderGlass(pose, bufferSource.getBuffer(Sheets.translucentCullBlockSheet()), glass,
                    BoxRenderer.WIRED_GLASS, light);
        }
        if (fontTexts != null) {
            WorldText.draw(poseStack, bufferSource, fontTexts);
        }
        super.render(door, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    /** 乗場の側の、出入口の前の明るさ。 */
    private static int landingLight(LandingDoorEntity door) {
        double front = DoorGeometry.landingFace(door.spec(), door.faceOffset()) + 0.5;
        Box local = new Box(0, 1.5, front, 0, 1.5, front).rotateQuarterTurns(door.quarterTurns());
        BlockPos pos = BlockPos.containing(door.getX() + local.minX(), door.getY() + local.minY(), door.getZ() + local.minZ());
        return LevelRenderer.getLightColor(door.level(), pos);
    }

    @Override
    public ResourceLocation getTextureLocation(LandingDoorEntity door) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.design.InteriorGeometry;
import net.lausiv1024.realelevator.core.design.LandingLayout;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.CarPanelLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.core.panel.WheelchairPanelLayout;
import net.lausiv1024.realelevator.entity.CarShapes;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * かごの描画。coreの形状の各部位を箱として描き、部位ごとの素材（ブロック）のテクスチャを1ブロック単位で敷き詰める。
 *
 * <p>かごはエンティティなので照明で周りを照らせない。代わりに、かごの中は周りの明るさにかかわらず照明がついている明るさ
 * （ブロックの明るさ15）で描き、天井の光る部分はさらに白く光らせる。
 */
public class ElevatorCarRenderer extends EntityRenderer<ElevatorCarEntity> {

    public ElevatorCarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0;
    }

    @Override
    public void render(ElevatorCarEntity car, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        // poseStack はバニラの補間位置に合わせてあるので、運転曲線での位置との差だけずらす
        poseStack.translate(0, car.renderY(partialTick) - Mth.lerp(partialTick, car.yo, car.getY()), 0);
        PoseStack.Pose pose = poseStack.last();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        // かごの中は照明がついている明るさで描く
        int light = LightTexture.pack(15, LightTexture.sky(packedLight));
        InteriorConfig interior = car.interior();
        TextureAtlasSprite wall = MaterialSprites.side(interior.wall());
        TextureAtlasSprite front = MaterialSprites.side(interior.front());
        TextureAtlasSprite transom = MaterialSprites.side(interior.transomMaterial());
        TextureAtlasSprite floor = MaterialSprites.side(interior.floor());
        // ガラスは半透明なので、ほかを描き終えてから描く
        List<Box> clearGlass = new ArrayList<>();
        List<Box> wiredGlass = new ArrayList<>();
        // かごの外を向く面は、亜鉛メッキ調で、まわりの明るさで描く
        Box outer = CarShapes.exterior(car.spec(), car.facing());
        BoxRenderer.Look outside = new BoxRenderer.Look(BoxRenderer.blockSprite(BoxRenderer.GALVANIZED), packedLight);
        for (Part part : car.parts()) {
            switch (part.type()) {
                case FLOOR -> BoxRenderer.renderCarBox(pose, consumer, part.box(), outer, new BoxRenderer.Look(floor, light), outside);
                case WALL -> BoxRenderer.renderCarBox(pose, consumer, part.box(), outer, new BoxRenderer.Look(wall, light), outside);
                case FRONT_WALL -> BoxRenderer.renderCarBox(pose, consumer, part.box(), outer, new BoxRenderer.Look(front, light), outside);
                case TRANSOM -> BoxRenderer.renderCarBox(pose, consumer, part.box(), outer, new BoxRenderer.Look(transom, light), outside);
                case CEILING -> BoxRenderer.renderCeiling(pose, consumer, part.box(), outer, light, outside);
                case GLASS -> clearGlass.add(part.box());
            }
        }
        BoxRenderer.renderInterior(pose, consumer, InteriorGeometry.parts(car.spec(), car.isThrough(), car.isObservation(), interior),
                car.facing().get2DDataValue(), light, MaterialSprites.side(interior.baseboard()),
                interior.carpet().blockId() != null ? MaterialSprites.side(interior.carpet().blockId()) : wall);
        double tick = car.renderTick(partialTick);
        TextureAtlasSprite door = MaterialSprites.side(interior.door());
        // 戸板は袖壁の外のレール（4枚戸の中の2枚などは、さらに1つ外）を通るので、戸の動く範囲までをかごの内側として面を分ける
        Box travel = DoorGeometry.travelBounds(car.spec(), DoorGeometry.Side.CAR);
        Box exterior = CarGeometry.exterior(car.spec());
        Box doorOuter = new Box(exterior.minX(), exterior.minY(), -travel.maxZ(), exterior.maxX(), exterior.maxY(), travel.maxZ())
                .rotateQuarterTurns(car.facing().get2DDataValue());
        for (Entrance entrance : car.entrances()) {
            double fraction = car.doorFractionAt(tick, entrance);
            for (Box panel : CarShapes.doorPanels(car.spec(), car.facing(), DoorGeometry.Side.CAR, fraction, entrance)) {
                // 乗場の側を向く面は、かごの外側と同じ亜鉛メッキ調
                BoxRenderer.renderDoorPanel(pose, consumer, panel, door, light, outside.sprite(), packedLight, doorOuter,
                        interior.securityWindow(), wiredGlass);
            }
            // 戸の上の、トランサムの張り出し
            for (Box soffit : DoorGeometry.soffits(car.spec(), DoorGeometry.Side.CAR)) {
                BoxRenderer.renderCarBox(pose, consumer, soffit.rotateQuarterTurns(car.facing().get2DDataValue() + entrance.quarterTurns()),
                        doorOuter, new BoxRenderer.Look(transom, light), new BoxRenderer.Look(transom, packedLight));
            }
            // 片開きの、戸袋と反対側の戸当たり
            for (Box jamb : DoorGeometry.jambReturns(car.spec(), DoorGeometry.Side.CAR)) {
                BoxRenderer.renderCarBox(pose, consumer, jamb.rotateQuarterTurns(car.facing().get2DDataValue() + entrance.quarterTurns()),
                        doorOuter, new BoxRenderer.Look(front, light), new BoxRenderer.Look(front, packedLight));
            }
            // 敷居（かごの床と同じ高さ）
            BoxRenderer.renderLanding(pose, consumer, LandingLayout.sill(car.spec(), DoorGeometry.Side.CAR, 0),
                    car.facing().get2DDataValue() + entrance.quarterTurns(), light, door, door);
        }
        FixtureConfig fixtures = car.fixtures();
        CarSpec spec = car.spec();
        int turns = car.facing().get2DDataValue();
        int rgb = fixtures.indicatorColor().rgb();
        // フォントで描く設定なら、階名はドットで描かずに、あとでまとめてフォントで描く
        List<WorldText.OnFace> fontTexts = fixtures.indicatorStyle() == FixtureConfig.IndicatorStyle.FONT ? new ArrayList<>() : null;
        BoxRenderer.renderButtons(pose, consumer, car, turns, light, rgb, fontTexts);
        String label = car.indicatorLabel(tick);
        String dotLabel = fontTexts != null ? "" : label;
        // 二方向出入口では、奥の出入口にも同じ操作盤と表示器を180°回して付ける
        CarPanelLayout panel = CarPanelLayout.of(spec, car.floorCount());
        for (Entrance entrance : car.entrances()) {
            int entranceTurns = turns + entrance.quarterTurns();
            for (boolean right : ButtonLayout.panelJambs(spec, fixtures.panelSides())) {
                BoxRenderer.renderParts(pose, consumer, panel.parts(spec, right, false), entranceTurns, light);
            }
            for (FloorIndicator.Frame indicator : FloorIndicator.car(spec, car.floorCount(), fixtures, dotLabel, car.indicatorDirection())) {
                BoxRenderer.renderIndicator(pose, consumer, indicator, entranceTurns, light, rgb, fontTexts, label);
            }
        }
        if (fixtures.wheelchairPanel()) {
            WheelchairPanelLayout wheelchair = WheelchairPanelLayout.of(spec, car.floorCount());
            for (boolean right : new boolean[] {true, false}) {
                BoxRenderer.renderParts(pose, consumer, wheelchair.parts(spec, right), turns, light);
            }
            for (FloorIndicator.Frame indicator : FloorIndicator.wheelchair(spec, car.floorCount(), dotLabel, car.indicatorDirection())) {
                BoxRenderer.renderIndicator(pose, consumer, indicator, turns, light, rgb, fontTexts, label);
            }
        }
        VertexConsumer glass = bufferSource.getBuffer(Sheets.translucentCullBlockSheet());
        BoxRenderer.renderGlass(pose, glass, clearGlass, BoxRenderer.CLEAR_GLASS, light);
        BoxRenderer.renderGlass(pose, glass, wiredGlass, BoxRenderer.WIRED_GLASS, light);
        if (fontTexts != null) {
            WorldText.draw(poseStack, bufferSource, fontTexts);
        }
        ButtonTags.draw(poseStack, bufferSource, car, turns, entityRenderDispatcher.cameraOrientation());
        poseStack.popPose();
        // シーブの側面は向きを気にせず描けるよう、裏面も描く
        HoistRenderer.render(car, partialTick, poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS)));
        super.render(car, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ElevatorCarEntity car) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

package net.lausiv1024.realelevator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.design.InteriorGeometry;
import net.lausiv1024.realelevator.core.design.LandingLayout;
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
        TextureAtlasSprite floor = MaterialSprites.side(interior.floor());
        for (Part part : car.parts()) {
            switch (part.type()) {
                case FLOOR -> BoxRenderer.renderBox(pose, consumer, part.box(), floor, light);
                case WALL -> BoxRenderer.renderBox(pose, consumer, part.box(), wall, light);
                case FRONT_WALL -> BoxRenderer.renderBox(pose, consumer, part.box(), front, light);
                case CEILING -> BoxRenderer.renderCeiling(pose, consumer, part.box(), light);
            }
        }
        BoxRenderer.renderInterior(pose, consumer, InteriorGeometry.parts(car.spec(), car.isThrough(), interior),
                car.facing().get2DDataValue(), light, MaterialSprites.side(interior.baseboard()));
        double tick = car.renderTick(partialTick);
        TextureAtlasSprite door = MaterialSprites.side(interior.door());
        for (Entrance entrance : car.entrances()) {
            double fraction = car.doorFractionAt(tick, entrance);
            for (Box panel : CarShapes.doorPanels(car.spec(), car.facing(), DoorGeometry.Side.CAR, fraction, entrance)) {
                BoxRenderer.renderBox(pose, consumer, panel, door, light);
            }
            // 敷居（かごの床と同じ高さ）
            BoxRenderer.renderLanding(pose, consumer, LandingLayout.sill(car.spec(), DoorGeometry.Side.CAR, 0),
                    car.facing().get2DDataValue() + entrance.quarterTurns(), light, door, door);
        }
        FixtureConfig fixtures = car.fixtures();
        CarSpec spec = car.spec();
        int turns = car.facing().get2DDataValue();
        int rgb = fixtures.indicatorColor().rgb();
        BoxRenderer.renderButtons(pose, consumer, car, turns, light, rgb);
        String label = car.indicatorLabel(tick);
        // 二方向出入口では、奥の出入口にも同じ操作盤と表示器を180°回して付ける
        CarPanelLayout panel = CarPanelLayout.of(spec, car.floorCount());
        for (Entrance entrance : car.entrances()) {
            int entranceTurns = turns + entrance.quarterTurns();
            for (boolean right : ButtonLayout.panelJambs(spec, fixtures.panelSides())) {
                BoxRenderer.renderParts(pose, consumer, panel.parts(spec, right, false), entranceTurns, light);
            }
            for (FloorIndicator.Frame indicator : FloorIndicator.car(spec, car.floorCount(), fixtures, label, car.indicatorDirection())) {
                BoxRenderer.renderIndicator(pose, consumer, indicator, entranceTurns, light, rgb);
            }
        }
        if (fixtures.wheelchairPanel()) {
            WheelchairPanelLayout wheelchair = WheelchairPanelLayout.of(spec, car.floorCount());
            for (boolean right : new boolean[] {true, false}) {
                BoxRenderer.renderParts(pose, consumer, wheelchair.parts(spec, right), turns, light);
            }
            for (FloorIndicator.Frame indicator : FloorIndicator.wheelchair(spec, car.floorCount(), label, car.indicatorDirection())) {
                BoxRenderer.renderIndicator(pose, consumer, indicator, turns, light, rgb);
            }
        }
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

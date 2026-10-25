package net.lausiv1024.realelevator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarGeometry.PartType;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.CarPanelLayout;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.core.panel.WheelchairPanelLayout;
import net.lausiv1024.realelevator.entity.CarShapes;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * かごの仮の描画。coreの形状の各部位を箱として描き、バニラのブロックのテクスチャを1ブロック単位で敷き詰める。
 * デザインシステム（フェーズ4）で、部位ごとの素材に差し替える。
 */
public class ElevatorCarRenderer extends EntityRenderer<ElevatorCarEntity> {
    private static final ResourceLocation WALL = ResourceLocation.withDefaultNamespace("block/iron_block");
    private static final ResourceLocation FLOOR = ResourceLocation.withDefaultNamespace("block/polished_andesite");
    private static final ResourceLocation CEILING = ResourceLocation.withDefaultNamespace("block/smooth_stone");
    static final ResourceLocation DOOR = ResourceLocation.withDefaultNamespace("block/light_gray_concrete");

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
        for (Part part : car.parts()) {
            BoxRenderer.renderBox(pose, consumer, part.box(), sprite(part.type()), packedLight);
        }
        double tick = car.renderTick(partialTick);
        TextureAtlasSprite door = BoxRenderer.blockSprite(DOOR);
        for (Entrance entrance : car.entrances()) {
            double fraction = car.doorFractionAt(tick, entrance);
            for (Box panel : CarShapes.doorPanels(car.spec(), car.facing(), DoorGeometry.Side.CAR, fraction, entrance)) {
                BoxRenderer.renderBox(pose, consumer, panel, door, packedLight);
            }
        }
        FixtureConfig fixtures = car.fixtures();
        CarSpec spec = car.spec();
        int turns = car.facing().get2DDataValue();
        int rgb = fixtures.indicatorColor().rgb();
        BoxRenderer.renderButtons(pose, consumer, car, turns, packedLight, rgb);
        String label = car.indicatorLabel(tick);
        // 二方向出入口では、奥の出入口にも同じ操作盤と表示器を180°回して付ける
        CarPanelLayout panel = CarPanelLayout.of(spec, car.floorCount());
        for (Entrance entrance : car.entrances()) {
            int entranceTurns = turns + entrance.quarterTurns();
            for (boolean right : ButtonLayout.panelJambs(spec, fixtures.panelSides())) {
                BoxRenderer.renderParts(pose, consumer, panel.parts(spec, right, false), entranceTurns, packedLight);
            }
            for (FloorIndicator.Frame indicator : FloorIndicator.car(spec, car.floorCount(), fixtures, label, car.indicatorDirection())) {
                BoxRenderer.renderIndicator(pose, consumer, indicator, entranceTurns, packedLight, rgb);
            }
        }
        if (fixtures.wheelchairPanel()) {
            WheelchairPanelLayout wheelchair = WheelchairPanelLayout.of(spec, car.floorCount());
            for (boolean right : new boolean[] {true, false}) {
                BoxRenderer.renderParts(pose, consumer, wheelchair.parts(spec, right), turns, packedLight);
            }
            for (FloorIndicator.Frame indicator : FloorIndicator.wheelchair(spec, car.floorCount(), label, car.indicatorDirection())) {
                BoxRenderer.renderIndicator(pose, consumer, indicator, turns, packedLight, rgb);
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

    private static TextureAtlasSprite sprite(PartType type) {
        return BoxRenderer.blockSprite(switch (type) {
            case FLOOR -> FLOOR;
            case WALL -> WALL;
            case CEILING -> CEILING;
        });
    }
}

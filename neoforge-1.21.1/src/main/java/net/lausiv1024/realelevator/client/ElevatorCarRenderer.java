package net.lausiv1024.realelevator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarGeometry.PartType;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
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
        double fraction = car.doorFractionAt(car.renderTick(partialTick));
        TextureAtlasSprite door = BoxRenderer.blockSprite(DOOR);
        for (Box panel : CarShapes.doorPanels(car.model(), car.facing(), DoorGeometry.Side.CAR, fraction)) {
            BoxRenderer.renderBox(pose, consumer, panel, door, packedLight);
        }
        BoxRenderer.renderButtons(pose, consumer, car, car.facing().get2DDataValue(), packedLight);
        double tick = car.renderTick(partialTick);
        for (FloorIndicator.Frame indicator : FloorIndicator.car(car.model(), car.floorCount(), car.fixtures(),
                car.floorLabel(car.indicatorFloor(tick)), car.indicatorDirection())) {
            BoxRenderer.renderIndicator(pose, consumer, indicator, car.facing().get2DDataValue(), packedLight);
        }
        poseStack.popPose();
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

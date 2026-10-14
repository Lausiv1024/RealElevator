package net.lausiv1024.realelevator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.CarShapes;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * 乗場の扉、▲▼ボタン、階床表示器の仮の描画。戸はかごの戸と同じテクスチャを使う。
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
        TextureAtlasSprite sprite = BoxRenderer.blockSprite(ElevatorCarRenderer.DOOR);
        double fraction = door.renderFraction(partialTick);
        for (Box panel : CarShapes.doorPanels(door.model(), door.facing(), DoorGeometry.Side.LANDING, fraction)) {
            BoxRenderer.renderBox(pose, consumer, panel, sprite, packedLight);
        }
        BoxRenderer.renderButtons(pose, consumer, door, door.facing().get2DDataValue(), packedLight);
        // かごが読み込まれていなければ、表示器は消灯したままにする
        ElevatorCarEntity car = door.car();
        FloorIndicator.Frame indicator = car != null
                ? FloorIndicator.hall(door.model(), car.fixtures(), car.floorLabel(car.indicatorFloor(car.renderTick(partialTick))),
                        car.indicatorDirection())
                : FloorIndicator.hall(door.model(), FixtureConfig.DEFAULT, "", TravelDirection.NONE);
        BoxRenderer.renderIndicator(pose, consumer, indicator, door.facing().get2DDataValue(), packedLight);
        super.render(door, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LandingDoorEntity door) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

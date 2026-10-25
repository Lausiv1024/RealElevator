package net.lausiv1024.realelevator.client;

import java.util.Set;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.display.HallLantern;
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
 * 乗場の扉、三方枠、▲▼ボタン、階床表示器、ホールランタンの仮の描画。戸はかごの戸と同じテクスチャを使う。
 */
public class LandingDoorRenderer extends EntityRenderer<LandingDoorEntity> {
    /** 三方枠（仮）のテクスチャ。デザインシステム（フェーズ4）で素材を選べるようにする。 */
    private static final ResourceLocation FRAME = ResourceLocation.withDefaultNamespace("block/smooth_stone");

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
        for (Box panel : CarShapes.doorPanels(door.spec(), door.facing(), DoorGeometry.Side.LANDING, fraction, door.entrance())) {
            BoxRenderer.renderBox(pose, consumer, panel, sprite, packedLight);
        }
        TextureAtlasSprite frame = BoxRenderer.blockSprite(FRAME);
        for (Box box : CarShapes.landingFrame(door.spec(), door.facing(), door.entrance())) {
            BoxRenderer.renderBox(pose, consumer, box, frame, packedLight);
        }
        // かごが読み込まれていなければ、表示器は消灯したままにする
        ElevatorCarEntity car = door.car();
        FixtureConfig fixtures = car != null ? car.fixtures() : FixtureConfig.DEFAULT;
        int rgb = fixtures.indicatorColor().rgb();
        BoxRenderer.renderButtons(pose, consumer, door, door.quarterTurns(), packedLight, rgb);
        FloorIndicator.Frame indicator = car != null
                ? FloorIndicator.hall(door.spec(), car.fixtures(), car.indicatorLabel(car.renderTick(partialTick)),
                        car.indicatorDirection(), door.faceOffset())
                : FloorIndicator.hall(door.spec(), FixtureConfig.DEFAULT, "", TravelDirection.NONE, door.faceOffset());
        BoxRenderer.renderIndicator(pose, consumer, indicator, door.quarterTurns(), packedLight, rgb);
        // 点灯する段は、点滅の消灯中は消す
        Set<TravelDirection> lit = car != null && HallLantern.isBlinkOn(car.lanternTicks(car.renderTick(partialTick)))
                ? door.litLanternSections() : Set.of();
        boolean terminal = car != null && door.floorIndex() >= 0 && car.isTerminalFloor(door.floorIndex());
        BoxRenderer.renderLantern(pose, consumer, HallLantern.sections(door.spec(), fixtures, terminal, door.faceOffset()), lit,
                door.quarterTurns(), packedLight, rgb);
        super.render(door, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LandingDoorEntity door) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}

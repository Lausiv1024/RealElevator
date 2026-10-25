package net.lausiv1024.realelevator.client;

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
    /** 操作盤の板とランタンの背面プレートの素材。 */
    private static final String STAINLESS = "realelevator:stainless_hairline";

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

        TextureAtlasSprite doorSprite = MaterialSprites.side(interior.door());
        double fraction = door.renderFraction(partialTick);
        for (Box panel : CarShapes.doorPanels(door.spec(), door.facing(), DoorGeometry.Side.LANDING, fraction, door.entrance())) {
            BoxRenderer.renderBox(pose, consumer, panel, doorSprite, light);
        }
        InteriorConfig.Landing landing = interior.landing();
        BoxRenderer.renderLanding(pose, consumer, layout.parts(), turns, light, MaterialSprites.side(landing.frameMaterial()),
                MaterialSprites.side(STAINLESS));
        if (InteriorConfig.AUTO.equals(landing.wallMaterial())) {
            ChameleonWall.Look look = ChameleonWall.look(door);
            BoxRenderer.renderWall(pose, consumer, layout.wall(), turns, light, look.sprite(), look.rgb());
        } else {
            BoxRenderer.renderWall(pose, consumer, layout.wall(), turns, light, MaterialSprites.side(landing.wallMaterial()), 0xFFFFFF);
        }

        BoxRenderer.renderButtons(pose, consumer, door, turns, light, rgb);
        FloorIndicator.Frame indicator = car != null
                ? layout.indicator(car.indicatorLabel(car.renderTick(partialTick)), car.indicatorDirection())
                : layout.indicator("", TravelDirection.NONE);
        if (indicator != null) {
            BoxRenderer.renderIndicator(pose, consumer, indicator, turns, light, rgb);
        }
        // 点灯する段は、点滅の消灯中は消す
        Set<TravelDirection> lit = car != null && HallLantern.isBlinkOn(car.lanternTicks(car.renderTick(partialTick)))
                ? door.litLanternSections() : Set.of();
        boolean terminal = car != null && door.floorIndex() >= 0 && car.isTerminalFloor(door.floorIndex());
        BoxRenderer.renderLantern(pose, consumer, layout.lantern(terminal), lit, turns, light, rgb);
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

package net.lausiv1024.realelevator.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarGeometry.PartType;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
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
            renderBox(pose, consumer, part.box(), sprite(part.type()), packedLight);
        }
        poseStack.popPose();
        super.render(car, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(ElevatorCarEntity car) {
        return TextureAtlas.LOCATION_BLOCKS;
    }

    private static TextureAtlasSprite sprite(PartType type) {
        ResourceLocation location = switch (type) {
            case FLOOR -> FLOOR;
            case WALL -> WALL;
            case CEILING -> CEILING;
        };
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(location);
    }

    private static void renderBox(PoseStack.Pose pose, VertexConsumer consumer, Box b, TextureAtlasSprite sprite, int light) {
        float x0 = (float) b.minX(), y0 = (float) b.minY(), z0 = (float) b.minZ();
        float x1 = (float) b.maxX(), y1 = (float) b.maxY(), z1 = (float) b.maxZ();
        // 各面を外向きに、反時計回りの頂点順で描く。(u, v) は面内の2軸で、どちらも1ブロックごとにテクスチャを繰り返す
        face(pose, consumer, sprite, light, 0, -1, 0, x0, x1, z0, z1, (u, v) -> new float[] {u, y0, v});
        face(pose, consumer, sprite, light, 0, 1, 0, x0, x1, z1, z0, (u, v) -> new float[] {u, y1, v});
        face(pose, consumer, sprite, light, 0, 0, -1, x1, x0, y0, y1, (u, v) -> new float[] {u, v, z0});
        face(pose, consumer, sprite, light, 0, 0, 1, x0, x1, y0, y1, (u, v) -> new float[] {u, v, z1});
        face(pose, consumer, sprite, light, -1, 0, 0, z0, z1, y0, y1, (u, v) -> new float[] {x0, v, u});
        face(pose, consumer, sprite, light, 1, 0, 0, z1, z0, y0, y1, (u, v) -> new float[] {x1, v, u});
    }

    private interface FacePoint {
        float[] at(float u, float v);
    }

    /**
     * 面を1ブロックごとのタイルに分けて描く。u は uStart から uEnd へ、v は vStart から vEnd へ進む。
     * 頂点は (uStart,vStart) → (uEnd,vStart) → (uEnd,vEnd) → (uStart,vEnd) の順で、法線の側から見て反時計回りになるよう、
     * 呼び出し側で u・v の向きを決めている。
     */
    private static void face(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite, int light,
                             float nx, float ny, float nz, float uStart, float uEnd, float vStart, float vEnd, FacePoint point) {
        float uLength = Math.abs(uEnd - uStart);
        float vLength = Math.abs(vEnd - vStart);
        float uSign = Math.signum(uEnd - uStart);
        float vSign = Math.signum(vEnd - vStart);
        for (float du = 0; du < uLength; du += 1) {
            float tileU = Math.min(1, uLength - du);
            for (float dv = 0; dv < vLength; dv += 1) {
                float tileV = Math.min(1, vLength - dv);
                float ua = uStart + uSign * du;
                float ub = ua + uSign * tileU;
                float va = vStart + vSign * dv;
                float vb = va + vSign * tileV;
                // テクスチャは下から上へ v が減る向き（画像の上が上）になるよう、1 − v で貼る
                vertex(pose, consumer, point.at(ua, va), sprite.getU(0), sprite.getV(1), light, nx, ny, nz);
                vertex(pose, consumer, point.at(ub, va), sprite.getU(tileU), sprite.getV(1), light, nx, ny, nz);
                vertex(pose, consumer, point.at(ub, vb), sprite.getU(tileU), sprite.getV(1 - tileV), light, nx, ny, nz);
                vertex(pose, consumer, point.at(ua, vb), sprite.getU(0), sprite.getV(1 - tileV), light, nx, ny, nz);
            }
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer consumer, float[] p, float u, float v, int light,
                               float nx, float ny, float nz) {
        consumer.addVertex(pose, p[0], p[1], p[2])
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}

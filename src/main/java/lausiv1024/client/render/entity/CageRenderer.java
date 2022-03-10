package lausiv1024.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevator;
import lausiv1024.client.entitymodel.FloorStandard;
import lausiv1024.client.entitymodel.RoofStandard;
import lausiv1024.client.entitymodel.WallStandard;
import lausiv1024.elevator.ElevatorDirection;
import lausiv1024.entity.CageEntity;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;

public class CageRenderer extends EntityRenderer<CageEntity> {
    protected final WallStandard ws = new WallStandard();
    protected final RoofStandard rs = new RoofStandard();
    protected final FloorStandard fs = new FloorStandard();

    private static final RenderType[] ARROW_UP = {getTexture("textures/blocks/floor_display/up0"), getTexture("textures/blocks/floor_display/up1"),
            getTexture("textures/blocks/floor_display/up2"), getTexture("textures/blocks/floor_display/up3"),
            getTexture("textures/blocks/floor_display/up4"), getTexture("textures/blocks/floor_display/up5"),
            getTexture("textures/blocks/floor_display/up6"),
            getTexture("textures/blocks/floor_display/up7")};
    private static final RenderType[] ARROW_DOWN = {getTexture("textures/blocks/floor_display/down0"),
            getTexture("textures/blocks/floor_display/down1"), getTexture("textures/blocks/floor_display/down2"), getTexture("textures/blocks/floor_display/down3"), getTexture("textures/blocks/floor_display/down4"),
            getTexture("textures/blocks/floor_display/down5"), getTexture("textures/blocks/floor_display/down6"),getTexture("textures/blocks/floor_display/down7")};

    public CageRenderer(EntityRendererManager dispatcher) {
        super(dispatcher);
    }

    @Override
    public void render(CageEntity cageEntity, float v, float v1, MatrixStack stack, IRenderTypeBuffer buffer, int color) {
        super.render(cageEntity, v, v1, stack, buffer, color);
        //RealElevator.LOGGER.info("combinedColor : {}", color);
        int d = cageEntity.getRotation();
        renderPart(cageEntity, v, v1, stack, buffer, color,
                ws, new ResourceLocation(RealElevator.ID, "textures/entities/cage/wall_standard_sample1.png"), d);
        renderPart(cageEntity, v, v1, stack, buffer, color,
                rs, new ResourceLocation(RealElevator.ID, "textures/entities/cage/roof_standard_1.png"), d);
        renderPart(cageEntity, v, v1, stack, buffer, color,
                fs, new ResourceLocation(RealElevator.ID, "textures/entities/cage/floor_standard_sample1.png"), d);
        renderFont(cageEntity, v, v1, stack, buffer, color);
        renderArrow(cageEntity, v, v1, stack, buffer, color);
    }

    @Override
    protected int getBlockLightLevel(CageEntity p_225624_1_, BlockPos p_225624_2_) {
        return 15;
    }

    private void renderPart(CageEntity entity, float v, float v1, MatrixStack stack, IRenderTypeBuffer buffer, int ctr, EntityModel model, ResourceLocation location, int rotation){
        stack.pushPose();
        stack.translate(0, 1.5, 0);
        stack.mulPose(Vector3f.YP.rotationDegrees(180.0f - v));
        stack.mulPose(Vector3f.YP.rotationDegrees(90.0f * rotation));

        stack.scale(1.f, -1.f, -1.f);

        IVertexBuilder builder = buffer.getBuffer(model.renderType(location));
        model.renderToBuffer(stack, builder, ctr, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        stack.popPose();
    }

    private void renderFont(CageEntity entity, float v, float v1, MatrixStack stack, IRenderTypeBuffer buffer, int ctr){
        String fl = entity.getCurFloor();

        stack.pushPose();
        Matrix4f matrix4f = stack.last().pose();
        FontRenderer fontRenderer = getFont();
        stack.translate(.0, 2.27, .0);
        stack.mulPose(Vector3f.YP.rotationDegrees(180.0f - v));
        stack.mulPose(Vector3f.YP.rotationDegrees(90.f * entity.getRotation()));
        stack.translate(-1.06, 0, 1.55);
        stack.scale(-0.018F, -0.018F, 0.018F);
        fontRenderer.drawInBatch(fl, -fontRenderer.width(fl) / 2.0f, -fontRenderer.lineHeight,
                NativeImage.combine(255, 255, 255, 255),
                false, matrix4f, buffer, false, 255, 15728704);
        stack.popPose();
    }

    private void renderArrow(CageEntity entity, float v, float v1, MatrixStack stack, IRenderTypeBuffer buffer, int ctr){
        int i = entity.getRenderDirection().nbt_index;
        RenderType[] types = i == 1 ? ARROW_UP : i == 2 ? ARROW_DOWN : null;
        if (types == null) return;
        stack.pushPose();
        stack.translate(.0, 2.3, 0);
        stack.mulPose(Vector3f.YP.rotationDegrees(90.0f * entity.getRotation()));
//        stack.translate(-1.06f, 0, 1.55);

        drawQuad(types[entity.getArrowFrame()], stack, buffer,
                new float[]{0.1f, -0.1f, 0.1f},
                new float[]{-0.1f, -0.1f, 0.1f},
                new float[]{-0.1f, 0.1f, 0.1f},
                new float[]{0.1f, 0.1f, 0.1f});

        stack.popPose();
    }

    private void drawQuad(RenderType type,MatrixStack stack, IRenderTypeBuffer buffer, float[] vertex1, float[] vertex2, float[] vertex3, float[] vertex4){
        Matrix4f matrix = stack.last().pose();
        IVertexBuilder builder = buffer.getBuffer(type);
        builder.vertex(matrix, vertex1[0], vertex2[1], vertex1[2]).uv(1, 0).endVertex();
        builder.vertex(matrix, vertex2[0], vertex2[1], vertex2[2]).uv(0, 0).endVertex();
        builder.vertex(matrix, vertex3[0], vertex3[1], vertex3[2]).uv(0, 1).endVertex();
        builder.vertex(matrix, vertex4[0], vertex4[1], vertex4[2]).uv(1, 1).endVertex();
    }

    private static RenderType getTexture(final String path){
        RenderType.State state = RenderType.State.builder().setTransparencyState(new RenderState.TransparencyState("translucent_transparency", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableAlphaTest();
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.disableAlphaTest();
        })).setTextureState(new RenderState.TextureState(new ResourceLocation(RealElevator.ID, path + ".png"), false, false)).createCompositeState(false);
        return RenderType.create("realelevator_texture_" + path, DefaultVertexFormats.POSITION_TEX, GL11.GL_QUADS, 256, false, true, state);
    }

    @Override
    public ResourceLocation getTextureLocation(CageEntity cage) {
        return AtlasTexture.LOCATION_BLOCKS;
    }
}

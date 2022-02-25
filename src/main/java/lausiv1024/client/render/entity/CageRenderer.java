package lausiv1024.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevator;
import lausiv1024.client.entitymodel.FloorStandard;
import lausiv1024.client.entitymodel.RoofStandard;
import lausiv1024.client.entitymodel.WallStandard;
import lausiv1024.entity.CageEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3f;

public class CageRenderer extends EntityRenderer<CageEntity> {
    protected final WallStandard ws = new WallStandard();
    protected final RoofStandard rs = new RoofStandard();
    protected final FloorStandard fs = new FloorStandard();

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
                rs, new ResourceLocation(RealElevator.ID, "textures/entities/cage/roof_standard_-1.png"), d);
        renderPart(cageEntity, v, v1, stack, buffer, color,
                fs, new ResourceLocation(RealElevator.ID, "textures/entities/cage/floor_standard_sample1.png"), d);
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

        stack.scale(1.0f, -1.0f, -1.0f);

        IVertexBuilder builder = buffer.getBuffer(model.renderType(location));
        model.renderToBuffer(stack, builder, ctr, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        stack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(CageEntity cage) {
        return AtlasTexture.LOCATION_BLOCKS;
    }
}

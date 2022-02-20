package lausiv1024.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevator;
import lausiv1024.client.entitymodel.FloorStandard;
import lausiv1024.client.entitymodel.RoofStandard;
import lausiv1024.client.entitymodel.WallStandard;
import lausiv1024.entity.CageEntity;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.util.ResourceLocation;
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
        renderPart(cageEntity, v, v1, stack, buffer, color,
                ws, new ResourceLocation(RealElevator.ID, "textures/entities/cage/wall_standard_sample.png"));
        renderPart(cageEntity, v, v1, stack, buffer, color,
                rs, new ResourceLocation(RealElevator.ID, "textures/entities/cage/roof_standard_1.png"));
        renderPart(cageEntity, v, v1, stack, buffer, color,
                fs, new ResourceLocation(RealElevator.ID, "textures/entities/cage/floor_standard_sample.png"));
    }

    private void renderPart(CageEntity entity, float v, float v1, MatrixStack stack, IRenderTypeBuffer buffer, int ctr, EntityModel model, ResourceLocation location){
        stack.pushPose();
        stack.mulPose(Vector3f.YP.rotationDegrees(180.0f - v));

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

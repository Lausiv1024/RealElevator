package lausiv1024.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.REItems;
import lausiv1024.RealElevator;
import lausiv1024.client.render.tileentity.ElevatorPartRender;
import lausiv1024.entity.EleButtonEntity;
import lausiv1024.entity.ElevatorPartEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EleButtonRenderer extends EntityRenderer<EleButtonEntity> {
    public static final EleButtonModel BUTTON_MODEL = new EleButtonModel();
    public static final ResourceLocation LOC_BUT = new ResourceLocation(RealElevator.ID, "textures/entities/cage_but_0.png");
    public static final ResourceLocation ACT_Y = new ResourceLocation(RealElevator.ID, "textures/entities/cage_but_1.png"); //Yellow Light
    public static final ResourceLocation ACT_W = new ResourceLocation(RealElevator.ID, "textures/entities/cage_but_2.png"); //White Light
    public EleButtonRenderer(EntityRendererManager entityRendererManager) {
        super(entityRendererManager);
    }

    @Override
    public void render(EleButtonEntity eleButtonEntity, float v, float v1, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int lightLevel) {
        matrixStack.pushPose();
        matrixStack.translate(0, -5.5 / 16.0, 0);
        matrixStack.scale(0.4f, 0.4f, 0.4f);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(eleButtonEntity.getButDirection().toYRot()));

        ResourceLocation res = LOC_BUT;
        if (eleButtonEntity.isActive() && eleButtonEntity.is_Enabled()){
            res = eleButtonEntity.getLightColor() == 0 ? ACT_Y : ACT_W;
        }

        IVertexBuilder builder = renderTypeBuffer.getBuffer(BUTTON_MODEL.renderType(res));
        BUTTON_MODEL.renderToBuffer(matrixStack, builder, lightLevel, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f );

        matrixStack.popPose();
        renderFnt(matrixStack, renderTypeBuffer, lightLevel, eleButtonEntity.getDisplayFloor(), eleButtonEntity.getButDirection());
        super.render(eleButtonEntity, v, v1, matrixStack, renderTypeBuffer, lightLevel);
    }

    private void renderFnt(MatrixStack stack, IRenderTypeBuffer buffer, int packedLight, String s, Direction direction){
        stack.pushPose();


        stack.translate(0.0, 0.04, 0);
        stack.mulPose(Vector3f.YP.rotationDegrees(direction.toYRot()));
        stack.translate(.0, .0, -0.015);
        stack.scale(-0.004F, -0.004F, 0.004F);

        FontRenderer fontRenderer = getFont();
        Matrix4f matrix4f = stack.last().pose();
        fontRenderer.drawInBatch(s, -fontRenderer.width(s) / 2.0f, -fontRenderer.lineHeight,
                NativeImage.combine(255, 255, 255, 255),
                false, matrix4f, buffer, false, 255, packedLight);

        stack.popPose();
    }

    @Override
    public ResourceLocation getTextureLocation(EleButtonEntity p_110775_1_) {
        return AtlasTexture.LOCATION_BLOCKS;
    }
}

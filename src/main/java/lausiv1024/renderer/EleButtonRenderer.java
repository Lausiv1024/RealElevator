package lausiv1024.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import lausiv1024.REItems;
import lausiv1024.entity.EleButtonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EleButtonRenderer extends EntityRenderer<EleButtonEntity> {
    Minecraft minecraft = Minecraft.getInstance();
    private final ItemRenderer itemRenderer;
    public EleButtonRenderer(EntityRendererManager entityRendererManager) {
        super(entityRendererManager);
        itemRenderer = minecraft.getItemRenderer();
    }

    @Override
    public void render(EleButtonEntity eleButtonEntity, float v, float v1, MatrixStack matrixStack, IRenderTypeBuffer renderTypeBuffer, int lightLevel) {
        Vector3d offset = this.getRenderOffset(eleButtonEntity, v1);
        matrixStack.pushPose();
        matrixStack.translate(offset.x(), offset.y() + 0.5, offset.z() - 0.65);
        matrixStack.scale(1, 1, 1);
        //matrixStack.mulPose(Vector3f.XP.rotationDegrees(eleButtonEntity.xRot));
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180 - eleButtonEntity.yRot));

        itemRenderer.renderStatic(new ItemStack(REItems.ELEVATOR_BUTTON.get()), ItemCameraTransforms.TransformType.GROUND, lightLevel, OverlayTexture.NO_OVERLAY,
                matrixStack, renderTypeBuffer);

        matrixStack.popPose();
        super.render(eleButtonEntity, v, v1, matrixStack, renderTypeBuffer, lightLevel);
    }

    @Override
    public ResourceLocation getTextureLocation(EleButtonEntity p_110775_1_) {
        return AtlasTexture.LOCATION_BLOCKS;
    }

    @Override
    public Vector3d getRenderOffset(EleButtonEntity p_225627_1_, float p_225627_2_) {
        return new Vector3d((double)((float)p_225627_1_.getDirection().getStepX() * 0.3F), -0.25D, (double)((float)p_225627_1_.getDirection().getStepZ() * 0.3F));
    }

}

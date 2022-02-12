package lausiv1024.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import lausiv1024.REItems;
import lausiv1024.RealElevator;
import lausiv1024.entity.CwtEntity;
import lausiv1024.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.entity.BoatRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

import javax.swing.text.LayeredHighlighter;

public class CwtEntityRenderer extends EntityRenderer<CwtEntity> {
    private final ItemRenderer itemRenderer;

    public CwtEntityRenderer(EntityRendererManager dispatcher) {
        super(dispatcher);
        itemRenderer = Minecraft.getInstance().getItemRenderer();
    }

    @Override
    public ResourceLocation getTextureLocation(CwtEntity cwt) {
        return AtlasTexture.LOCATION_BLOCKS;
    }

    @Override
    public void render(CwtEntity cwtEntity, float v, float v1, MatrixStack stack, IRenderTypeBuffer buffer, int lightLevel) {
        super.render(cwtEntity, v, v1, stack, buffer, lightLevel);
        stack.pushPose();
        Vector3d offset = getRenderOffset(cwtEntity, v1);
        stack.translate(-offset.x, -offset.y, -offset.z);
//        double partialTick = RenderUtil.getPartialTicks();
//        double x = MathHelper.lerp(partialTick, cwtEntity.xOld, cwtEntity.getX()) - cwtEntity.getX();
//        double y = MathHelper.lerp(partialTick, cwtEntity.yOld, cwtEntity.getY()) - cwtEntity.getY();
//        double z = MathHelper.lerp(partialTick, cwtEntity.zOld, cwtEntity.getZ()) - cwtEntity.getZ();
//        RealElevator.LOGGER.info("PartialTick = {}   Lerp pos : [x : {}, y : {}, z : {}]", partialTick, x, y, z);
//        stack.translate(x, y, z);

        if (cwtEntity.getAxis() == 1) {
            //これより前にTranslateを書いたら回転時に変な移動をしない
            stack.mulPose(Vector3f.YP.rotationDegrees(90));
            //あとに書くと状況に応じて変な移動をする
        }

        itemRenderer.renderStatic(new ItemStack(REItems.COUNTER_WEIGHT.get()),
                ItemCameraTransforms.TransformType.NONE, lightLevel, OverlayTexture.NO_OVERLAY, stack, buffer);
        stack.popPose();
    }
}

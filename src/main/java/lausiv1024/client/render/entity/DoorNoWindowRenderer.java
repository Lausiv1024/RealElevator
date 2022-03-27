package lausiv1024.client.render.entity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevator;
import lausiv1024.client.entitymodel.DoorNoWindow3;
import lausiv1024.entity.doors.ElevatorDoorNoWindow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DoorNoWindowRenderer extends EntityRenderer<ElevatorDoorNoWindow> {
    DoorNoWindow3 model = new DoorNoWindow3();
    Minecraft mc = Minecraft.getInstance();

    public DoorNoWindowRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    @Override
    public ResourceLocation getTextureLocation(ElevatorDoorNoWindow el) {
        return AtlasTexture.LOCATION_BLOCKS;
    }

    @Override
    public void render(ElevatorDoorNoWindow elevatorDoorNoWindow, float v, float v1, MatrixStack matrixStack, IRenderTypeBuffer buffer, int lightLevel) {
        super.render(elevatorDoorNoWindow, v, v1, matrixStack, buffer, lightLevel);
        renderDoor(elevatorDoorNoWindow, v, v1, matrixStack, buffer,lightLevel,
                model, new ResourceLocation(RealElevator.ID, "textures/entities/door/door_normal_no_window_dark_oak.png"), elevatorDoorNoWindow.getDirection1());
        mc.getModelManager().getModel(new ResourceLocation(""));

    }

    public void renderDoor(ElevatorDoorNoWindow elevatorDoorNoWindow, float v, float v1, MatrixStack stack, IRenderTypeBuffer buffer, int ctr, EntityModel model, ResourceLocation location, Direction direction){
        stack.pushPose();
        //int a = elevatorDoorNoWindow.isMirror1() ? -1 : 1;
        stack.translate(0, 1.5, 0);
        stack.mulPose(Vector3f.YP.rotationDegrees(180.0f - v));
        stack.mulPose(Vector3f.YP.rotationDegrees(direction.toYRot()));
        stack.scale(1.f, -1.f, -1.f);

        IVertexBuilder builder = buffer.getBuffer(model.renderType(location));
        model.renderToBuffer(stack, builder, ctr, OverlayTexture.NO_OVERLAY, 1.0f, 1.0f, 1.0f, 1.0f);
        stack.popPose();
    }

    @Override
    protected int getBlockLightLevel(ElevatorDoorNoWindow p_225624_1_, BlockPos p_225624_2_) {
        return 15;
    }
}

package lausiv1024.tileentity.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevatorCore;
import lausiv1024.blocks.ElevatorButtonBlock;
import lausiv1024.tileentity.LandingButtonBlockTE;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.crash.CrashReport;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import org.lwjgl.opengl.GL11;

public class LandingButtonTERenderer<T extends LandingButtonBlockTE> extends ElevatorPartRender<T>{
    private static final RenderType LIGHT_YELLOW = getTexture("hole_lantern_light_yellow");
    private static final RenderType LIGHT_WHITE = getTexture("hole_lantern_light_white");

    public LandingButtonTERenderer(TileEntityRendererDispatcher tileEntityRendererDispatcher) {
        super(tileEntityRendererDispatcher);
    }

    @Override
    protected void render() {
        RenderType type;
        if (tile.color == 0) type = LIGHT_YELLOW;
        else type = LIGHT_WHITE;
        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.5, 0.5);

        if (tile.getBlockState().getValue(ElevatorButtonBlock.FACING).getAxis() == Direction.Axis.Z){
            matrixStack.mulPose(new Quaternion(0,tile.getBlockState().getValue(ElevatorButtonBlock.FACING).toYRot(), 0, true));
        }else{
            matrixStack.mulPose(new Quaternion(0,tile.getBlockState().getValue(ElevatorButtonBlock.FACING).getOpposite().toYRot(), 0, true));
        }

        if (tile.up){
            drawQuad(type, new float[]{0.07f, 0.058f, 0.436f}, new float[]{-0.07f, 0.058f, 0.436f},
                    new float[]{-0.07f, 0.19f, 0.436f}, new float[]{0.07f, 0.19f, 0.436f});
        }
        if (tile.down){
            //drawQuad(type, new float[]{1.0f}, new float[]{1.0f}, new float[]{1.0f}, new float[]{1.0f});
        }
        matrixStack.popPose();
    }

    private void drawQuad(RenderType type, float[] vertex1, float[] vertex2, float[] vertex3, float[] vertex4){
        Matrix4f matrix = matrixStack.last().pose();
        IVertexBuilder builder = buffer.getBuffer(type);
        builder.vertex(matrix, vertex1[0], vertex2[1], vertex1[2]).uv(0, 1).endVertex();
        builder.vertex(matrix, vertex2[0], vertex2[1], vertex2[2]).uv(0, 1).endVertex();
        builder.vertex(matrix, vertex3[0], vertex3[1], vertex3[2]).uv(0, 1).endVertex();
        builder.vertex(matrix, vertex4[0], vertex4[1], vertex4[2]).uv(0, 1).endVertex();
    }

    private static RenderType getTexture(final String name){
        RenderType.State state = RenderType.State.builder().setTransparencyState(new RenderState.TransparencyState("translucent_transparency", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableAlphaTest();
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.disableAlphaTest();
        })).setTextureState(new RenderState.TextureState(new ResourceLocation(RealElevatorCore.ID, "textures/blocks/hole_lantern/" + name + ".png"), false, false)).createCompositeState(false);
        return RenderType.create("realelevator_texture_" + name, DefaultVertexFormats.POSITION_TEX, GL11.GL_QUADS, 256, false, true, state);
    }
}

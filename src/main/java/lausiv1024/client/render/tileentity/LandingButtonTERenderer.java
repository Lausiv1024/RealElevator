package lausiv1024.client.render.tileentity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevator;
import lausiv1024.blocks.ElevatorButtonBlock;
import lausiv1024.blocks.FloorController;
import lausiv1024.client.render.BoxRenderer;
import lausiv1024.tileentity.LandingButtonBlockTE;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;
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
        if (tile.getColor() == 0) type = LIGHT_YELLOW;
        else type = LIGHT_WHITE;
        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.5, 0.5);

        if (tile.getBlockState().getValue(ElevatorButtonBlock.FACING).getAxis() == Direction.Axis.Z){
            matrixStack.mulPose(new Quaternion(0,tile.getBlockState().getValue(ElevatorButtonBlock.FACING).toYRot(), 0, true));
        }else{
            matrixStack.mulPose(new Quaternion(0,tile.getBlockState().getValue(ElevatorButtonBlock.FACING).getOpposite().toYRot(), 0, true));
        }

        matrixStack.popPose();
        drawButLight();
    }

    private void drawButLight(){
        if (!tile.isCalled()) return;
        if (tile.getBlockState().getValue(FloorController.IS_SINGLE)){
            button(new Vector3f(-0.07f, -0.2f, 0.49f),
                    new Vector3f(0.07f, -0.06f, 0.43f));
        }else{
            if (tile.isUp()){
                button(new Vector3f(-0.07f, -0.07f, 0.49f),
                        new Vector3f(0.07f, 0.07f, 0.43f));
            }
            if (tile.isDown()){
                button(new Vector3f(-0.07f, -0.32f, 0.49f),
                        new Vector3f(0.07f, -0.18f, 0.43f));
            }
        }
    }

    private void button(Vector3f start, Vector3f end){
        matrixStack.pushPose();
        RenderType type1 = tile.getColor() == 1 ? LIGHT_WHITE : LIGHT_YELLOW;
        matrixStack.translate(0.5, 0.5, .5);
        Direction direction = tile.getBlockState().getValue(HorizontalBlock.FACING);
        if (direction.getNormal().getX() == 0)
            matrixStack.mulPose(new Quaternion(0, direction.toYRot(), 0, true));
        else
            matrixStack.mulPose(new Quaternion(0, direction.getOpposite().toYRot(), 0, true));
        BoxRenderer.render(type1, matrixStack, buffer, start, end);
        matrixStack.popPose();
    }


    private static RenderType getTexture(final String name){
        RenderType.State state = RenderType.State.builder().setTransparencyState(new RenderState.TransparencyState("translucent_transparency", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableAlphaTest();
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.disableAlphaTest();
        })).setTextureState(new RenderState.TextureState(new ResourceLocation(RealElevator.ID, "textures/blocks/hole_lantern/" + name + ".png"), false, false)).createCompositeState(false);
        return RenderType.create("realelevator_texture_" + name, DefaultVertexFormats.POSITION_TEX, GL11.GL_QUADS, 256, false, true, state);
    }
}

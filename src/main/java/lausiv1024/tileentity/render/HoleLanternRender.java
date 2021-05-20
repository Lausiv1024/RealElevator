package lausiv1024.tileentity.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lausiv1024.RealElevatorCore;
import lausiv1024.tileentity.HoleLanternTile;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public class HoleLanternRender extends ElevatorPartRender<HoleLanternTile> {

    private static final RenderType LIGHT_ON_White = getTexture("hole_lantern_light_white");
    private static final RenderType LIGHT_ON_YELLOW = getTexture("hole_lantern_light_yellow");

    public HoleLanternRender(TileEntityRendererDispatcher tileEntityRendererDispatcher) {
        super(tileEntityRendererDispatcher);

    }

    @Override
    protected void render() {
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
        return RenderType.create("movingelevators_texture_" + name, DefaultVertexFormats.POSITION_TEX, GL11.GL_QUADS, 256, false, true, state);
    }

    private void renderLight(){
        matrixStack.pushPose();
        matrixStack.popPose();
    }
}

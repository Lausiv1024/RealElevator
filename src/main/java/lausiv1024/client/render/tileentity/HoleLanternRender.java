package lausiv1024.client.render.tileentity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevator;
import lausiv1024.blocks.HoleLantern;
import lausiv1024.tileentity.HoleLanternTile;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class HoleLanternRender<T extends HoleLanternTile> extends ElevatorPartRender<T> {

    private static final RenderType LIGHT_ON_White = getTexture("hole_lantern_light_white_transpa");
    private static final RenderType LIGHT_ON_YELLOW = getTexture("hole_lantern_light_yellow_transpa");
    private static final float[] vertex1_surface = {0.07f, 0.44f,-0.415f};
    private static final float[] vertex2_surface = {-0.07f, 0.44f, -0.415f};
    private static final float[] vertex3_surface = {-0.07f, -0.45f, -0.415f};
    private static final float[] vertex4_surface = {0.07f, -0.45f, -0.415f};

    public HoleLanternRender(TileEntityRendererDispatcher tileEntityRendererDispatcher) {
        super(tileEntityRendererDispatcher);

    }

    @Override
    protected void render() {
        if (tile.getLightMode() != 0)
            renderLight(tile.getBlinking(), tile.getLightMode(), tile.getLightColor());
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

    //???????????????TER????????????????????????
    private void renderLight(boolean isblinking, int mode, int color){
            matrixStack.pushPose();
            matrixStack.translate(0.5, 0.5, 0.5);
            matrixStack.mulPose(new Quaternion(0,180 - tile.getBlockState().getValue(HoleLantern.FACING).toYRot(), 0, true));
            RenderType renderType;
            if (color == 0) renderType = LIGHT_ON_YELLOW;
            else renderType = LIGHT_ON_White;
            if (isblinking){
                if (tile.getLightTick() <= 10){
                    switch (mode){
                        case 1:
                            drawUP(renderType);
                            break;
                        case 2:
                            drawDown(renderType);
                            break;
                        case 3:
                            drawBoth(renderType);
                    }
                }
            }else{
                switch (mode){
                    case 1:
                        drawUP(renderType);
                        break;
                    case 2:
                        drawDown(renderType);
                        break;
                    case 3:
                        drawBoth(renderType);
                }
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

    private void drawBoth(RenderType type){
        //?????????????????????
        drawQuad(type, vertex1_surface, vertex2_surface, vertex3_surface, vertex4_surface);
        //??????????????????????????????wwww
        drawQuad(type,new float[]{0.07f, 0.44f, -0.5f} , new float[]{0.07f, 0.44f, -0.415f},
                new float[]{0.07f, -0.45f, -0.415f}, new float[]{0.07f,-0.45f,-0.5f});
        drawQuad(type, new float[]{-0.07f,-0.45f,-0.5f}, new float[]{-0.07f, -0.45f, -0.415f},
                new float[]{-0.07f, 0.44f, -0.415f}, new float[]{-0.07f, 0.44f, -0.5f});

        //?????????????????????????????????????????????
        drawQuad(type, new float[]{0.07f, 0.44f, -0.5f}, new float[]{-0.07f, 0.44f, -0.5f},
                new float[]{-0.07f, 0.44f, -0.415f}, new float[]{0.07f, 0.44f, -0.415f});
        //?????????
        drawQuad(type, new float[]{-0.07f, -0.45f, -0.5f}, new float[]{0.07f, -0.45f, -0.5f},
                new float[]{0.07f, -0.45f, -0.415f}, new float[]{-0.07f, -0.45f, -0.415f});
    }

    private void drawUP(RenderType type){
        float ypaaaa = 0f;
        //?????????????????????
        drawQuad(type, new float[]{0.07f, 0.44f,-0.415f}, new float[]{-0.07f, 0.44f, -0.415f},
                new float[]{-0.07f, ypaaaa, -0.415f}, new float[]{0.07f, ypaaaa, -0.415f});
        //??????????????????????????????wwww
        drawQuad(type,new float[]{0.07f, 0.44f, -0.5f} , new float[]{0.07f, 0.44f, -0.415f},
                new float[]{0.07f, ypaaaa, -0.415f}, new float[]{0.07f,ypaaaa,-0.5f});
        drawQuad(type, new float[]{-0.07f, ypaaaa,-0.5f}, new float[]{-0.07f, ypaaaa, -0.415f},
                new float[]{-0.07f, 0.44f, -0.415f}, new float[]{-0.07f, 0.44f, -0.5f});

        //?????????????????????????????????????????????
        drawQuad(type, new float[]{0.07f, 0.44f, -0.5f}, new float[]{-0.07f, 0.44f, -0.5f},
                new float[]{-0.07f, 0.44f, -0.415f}, new float[]{0.07f, 0.44f, -0.415f});
        //?????????
        drawQuad(type, new float[]{-0.07f, ypaaaa, -0.5f}, new float[]{0.07f, ypaaaa, -0.5f},
                new float[]{0.07f, ypaaaa, -0.415f}, new float[]{-0.07f, ypaaaa, -0.415f});
    }

    private void drawDown(RenderType type){
        float ypaaaa = 0f;
        //?????????????????????
        drawQuad(type, new float[]{0.07f, ypaaaa,-0.415f}, new float[]{-0.07f, ypaaaa, -0.415f},
                new float[]{-0.07f, -0.45f, -0.415f}, new float[]{0.07f, -0.45f, -0.415f});
        //??????????????????????????????wwww
        drawQuad(type,new float[]{0.07f, ypaaaa, -0.5f} , new float[]{0.07f, ypaaaa, -0.415f},
                new float[]{0.07f, -0.45f, -0.415f}, new float[]{0.07f,-0.45f,-0.5f});
        drawQuad(type, new float[]{-0.07f,-0.45f,-0.5f}, new float[]{-0.07f, -0.45f, -0.415f},
                new float[]{-0.07f, ypaaaa, -0.415f}, new float[]{-0.07f, ypaaaa, -0.5f});

        //?????????????????????????????????????????????
        drawQuad(type, new float[]{0.07f, ypaaaa, -0.5f}, new float[]{-0.07f, ypaaaa, -0.5f},
                new float[]{-0.07f, ypaaaa, -0.415f}, new float[]{0.07f, ypaaaa, -0.415f});
        //?????????
        drawQuad(type, new float[]{-0.07f, -0.45f, -0.5f}, new float[]{0.07f, -0.45f, -0.5f},
                new float[]{0.07f, -0.45f, -0.415f}, new float[]{-0.07f, -0.45f, -0.415f});
    }

}

package lausiv1024.client.render.tileentity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevatorCore;
import lausiv1024.blocks.FloorDisplay;
import lausiv1024.blocks.HoleLantern;
import lausiv1024.tileentity.FloorDisplayTile;
import lausiv1024.util.ElevatorDirection;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.NativeImage;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

@OnlyIn(Dist.CLIENT)
public class FloorDisplayRenderer<T extends FloorDisplayTile> extends ElevatorPartRender<T>{
    private static final RenderType[] ARROW_UP = {getTexture("up0"), getTexture("up1"),
    getTexture("up2"), getTexture("up3"), getTexture("up4"), getTexture("up5"), getTexture("up6"),
    getTexture("up7")};
    private static final RenderType[] ARROW_DOWN = {getTexture("down0"),
    getTexture("down1"), getTexture("down2"), getTexture("down3"), getTexture("down4"),
    getTexture("down5"), getTexture("down6"),getTexture("down7")};

    private static final float[] VER1 = {0.1f, 0.3f,-0.47f};
    private static final float[] VER2 = {-0.1f, 0.3f, -0.47f};
    private static final float[] VER3 = {-0.1f, 0.1f, -0.47f};
    private static final float[] VER4 = {0.1f, 0.1f, -0.47f};
    private static final RenderType AA = getTexture("up0");
    public FloorDisplayRenderer(TileEntityRendererDispatcher tileEntityRendererDispatcher) {
        super(tileEntityRendererDispatcher);
    }

    @Override
    protected void render() {
        drawArrow();
        drawString(tile.getCurFloorName());
        //RealElevatorCore.LOGGER.info(combinedLight);
    }

    private static RenderType getTexture(final String name){
        RenderType.State state = RenderType.State.builder().setTransparencyState(new RenderState.TransparencyState("translucent_transparency", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableAlphaTest();
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.disableAlphaTest();
        })).setTextureState(new RenderState.TextureState(new ResourceLocation(RealElevatorCore.ID, "textures/blocks/floor_display/" + name + ".png"), false, false)).createCompositeState(false);
        return RenderType.create("realelevator_texture_" + name, DefaultVertexFormats.POSITION_TEX, GL11.GL_QUADS, 256, false, true, state);
    }

    private void drawQuad(RenderType type, float[] vertex1, float[] vertex2, float[] vertex3, float[] vertex4){
        Matrix4f matrix = matrixStack.last().pose();
        IVertexBuilder builder = buffer.getBuffer(type);
        builder.vertex(matrix, vertex1[0], vertex2[1], vertex1[2]).uv(1, 0).endVertex();
        builder.vertex(matrix, vertex2[0], vertex2[1], vertex2[2]).uv(0, 0).endVertex();
        builder.vertex(matrix, vertex3[0], vertex3[1], vertex3[2]).uv(0, 1).endVertex();
        builder.vertex(matrix, vertex4[0], vertex4[1], vertex4[2]).uv(1, 1).endVertex();
    }

    private void drawArrow(){
        RenderType[] renderTypes;
        if (tile.getRenderingDirection() == ElevatorDirection.UP) renderTypes = ARROW_UP;
        else if (tile.getRenderingDirection() == ElevatorDirection.DOWN) renderTypes = ARROW_DOWN;
        else return;

        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.5, 0.5);
        matrixStack.mulPose(new Quaternion(0,180 - tile.getBlockState().getValue(HoleLantern.FACING).toYRot(), 0, true));

        drawQuad(renderTypes[tile.arrowFrame], VER1, VER2, VER3, VER4);

        matrixStack.popPose();
    }

    private void drawString(String s){
        Direction direction = tile.getBlockState().getValue(FloorDisplay.FACING);
        if (s == null) return;
        FontRenderer fontRenderer = this.renderer.font;
        matrixStack.pushPose();
        if (direction == Direction.WEST) matrixStack.translate(0.03, 0.35, 0.5);
        else if (direction == Direction.EAST) matrixStack.translate(0.97, 0.35, 0.5);
        else if (direction == Direction.SOUTH) matrixStack.translate(0.5, 0.35, 0.97);
        else if (direction == Direction.NORTH) matrixStack.translate(0.5, 0.35, 0.03);
        if (direction == Direction.SOUTH || direction == Direction.NORTH)
            matrixStack.mulPose(new Quaternion(0,tile.getBlockState().getValue(FloorDisplay.FACING).toYRot(), 0, true));
        else matrixStack.mulPose(new Quaternion(0,180 - tile.getBlockState().getValue(FloorDisplay.FACING).getOpposite().toYRot(), 0, true));
        if (s.length() <= 2)
            matrixStack.scale(-0.015f, -0.015f, 0.01f);
        else matrixStack.scale(-0.01f, -0.01f, 0.01f);
        fontRenderer.drawInBatch(s, -fontRenderer.width(s) / 2f, -fontRenderer.lineHeight, NativeImage.combine(255, 255, 255, 255),false, matrixStack.last().pose(), buffer, false, 255, 15728704);
        matrixStack.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(FloorDisplayTile p_188185_1_) {
        return true;
    }
}

package lausiv1024.client.render.tileentity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevator;
import lausiv1024.blocks.FloorController;
import lausiv1024.blocks.FloorDisplay;
import lausiv1024.blocks.HoleLantern;
import lausiv1024.client.render.BoxRenderer;
import lausiv1024.elevator.ElevatorDirection;
import lausiv1024.tileentity.FloorControllerTE;
import net.minecraft.block.HorizontalBlock;
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
import net.minecraft.util.math.vector.Vector3f;
import org.lwjgl.opengl.GL11;

public class FlControllerRenderer<T extends FloorControllerTE> extends ElevatorPartRender<T>{
    private static final double floorDispHeight = 0.75;
    private static final RenderType[] ARROW_UP = {getTexture("textures/blocks/floor_display/","up0"), getTexture("textures/blocks/floor_display/","up1"),
            getTexture("textures/blocks/floor_display/","up2"), getTexture("textures/blocks/floor_display/","up3"), getTexture("textures/blocks/floor_display/","up4"), getTexture("textures/blocks/floor_display/","up5"), getTexture("textures/blocks/floor_display/","up6"),
            getTexture("textures/blocks/floor_display/","up7")};
    private static final RenderType[] ARROW_DOWN = {getTexture("textures/blocks/floor_display/","down0"),
            getTexture("textures/blocks/floor_display/","down1"), getTexture("textures/blocks/floor_display/","down2"), getTexture("textures/blocks/floor_display/","down3"), getTexture("textures/blocks/floor_display/","down4"),
            getTexture("textures/blocks/floor_display/","down5"), getTexture("textures/blocks/floor_display/","down6"),getTexture("textures/blocks/floor_display/","down7")};

    private static final RenderType LIGHT_YELLOW = getTexture("textures/blocks/hole_lantern/","hole_lantern_light_yellow");
    private static final RenderType LIGHT_WHITE = getTexture("textures/blocks/hole_lantern/","hole_lantern_light_white");

    private static final float[] VER1 = {0.08f, 0.65f,-0.48f};
    private static final float[] VER2 = {-0.08f, 0.65f, -0.48f};
    private static final float[] VER3 = {-0.08f, 0.48f, -0.48f};
    private static final float[] VER4 = {0.08f, 0.48f, -0.48f};


    //TODO : とりあえずこうしたが、別にディレクトリ名とファイル名を分ける必要がないので後でどうにかする予定
    private static RenderType getTexture(final String dir ,final String name){
        RenderType.State state = RenderType.State.builder().setTransparencyState(new RenderState.TransparencyState("translucent_transparency", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableAlphaTest();
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.disableAlphaTest();
        })).setTextureState(new RenderState.TextureState(new ResourceLocation(RealElevator.ID, dir + name + ".png"), false, false)).createCompositeState(false);
        return RenderType.create("realelevator_texture_" + name, DefaultVertexFormats.POSITION_TEX, GL11.GL_QUADS, 256, false, true, state);
    }

    public FlControllerRenderer(TileEntityRendererDispatcher tileEntityRendererDispatcher) {
        super(tileEntityRendererDispatcher);
    }

    private void drawQuad(RenderType type, float[] vertex1, float[] vertex2, float[] vertex3, float[] vertex4){
        Matrix4f matrix = matrixStack.last().pose();
        IVertexBuilder builder = buffer.getBuffer(type);
        builder.vertex(matrix, vertex1[0], vertex2[1], vertex1[2]).uv(1, 0).endVertex();
        builder.vertex(matrix, vertex2[0], vertex2[1], vertex2[2]).uv(0, 0).endVertex();
        builder.vertex(matrix, vertex3[0], vertex3[1], vertex3[2]).uv(0, 1).endVertex();
        builder.vertex(matrix, vertex4[0], vertex4[1], vertex4[2]).uv(1, 1).endVertex();
    }


    @Override
    protected void render() {
        drawArrow();
        drawString(tile.getCurFlName());
        drawButLight();
    }

    private void drawArrow(){
        RenderType[] renderTypes;
        if (tile.getRenderingDirection() == ElevatorDirection.UP) renderTypes = ARROW_UP;
        else if (tile.getRenderingDirection() == ElevatorDirection.DOWN) renderTypes = ARROW_DOWN;
        else return;

        matrixStack.pushPose();
        matrixStack.translate(0.5, 0.5, 0.5);
        matrixStack.mulPose(new Quaternion(0,180 - tile.getBlockState().getValue(HoleLantern.FACING).toYRot(), 0, true));

        drawQuad(renderTypes[tile.getArrowFrame()], VER1, VER2, VER3, VER4);

        matrixStack.popPose();
    }

    private void drawButLight(){
        if (tile.getBlockState().getValue(FloorController.IS_SINGLE) && tile.isCalled()){
            button(new Vector3f(-0.07f, -0.38f, 0.49f),
                    new Vector3f(0.07f, -0.24f, 0.43f));
        }else{
            if (tile.isUp()){
                button(new Vector3f(-0.07f, -0.26f, 0.49f),
                        new Vector3f(0.07f, -0.12f, 0.43f));
            }
            if (tile.isDown()){
                button(new Vector3f(-0.07f, -0.51f, 0.49f),
                        new Vector3f(0.07f, -0.37f, 0.43f));
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

    private void drawString(String s){
        Direction direction = tile.getBlockState().getValue(FloorDisplay.FACING);
        if (s == null) return;
        FontRenderer fontRenderer = this.renderer.font;
        matrixStack.pushPose();
        if (direction == Direction.WEST) matrixStack.translate(0.03, floorDispHeight, 0.5);
        else if (direction == Direction.EAST) matrixStack.translate(0.97, floorDispHeight, 0.5);
        else if (direction == Direction.SOUTH) matrixStack.translate(0.5, floorDispHeight, 0.97);
        else if (direction == Direction.NORTH) matrixStack.translate(0.5, floorDispHeight, 0.03);
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
    public boolean shouldRenderOffScreen(FloorControllerTE p_188185_1_) {
        return true;
    }

}

package lausiv1024.renderer;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import lausiv1024.RealElevatorCore;
import lausiv1024.entity.doors.ElevatorDoorNoWindow;
import lausiv1024.util.EleVeneerType;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ModelManager;
import net.minecraft.client.renderer.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix3f;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class DoorNoWindowRenderer extends EntityRenderer<ElevatorDoorNoWindow> {
    private static final String BLOCKSTATE_ID = "elevatordoor_no_window";

    private static final RenderType RENDER_TYPE = getTexture(new ResourceLocation("minecraft:block/oak_planks"));
    private static final ResourceLocation PART31 = new ResourceLocation(RealElevatorCore.ID, BLOCKSTATE_ID + 31);
    private static final ResourceLocation PART32 = new ResourceLocation(RealElevatorCore.ID, BLOCKSTATE_ID + 32);
    private static final ResourceLocation PART41 = new ResourceLocation(RealElevatorCore.ID, BLOCKSTATE_ID + 41);
    private static final ResourceLocation PART42 = new ResourceLocation(RealElevatorCore.ID, BLOCKSTATE_ID + 42);

    private static final ModelResourceLocation[] TALL3_TYPE1 = {
//            new ModelResourceLocation(new ResourceLocation(RealElevatorCore.ID, BLOCKSTATE_ID + "31"), "type=2"),
//            new ModelResourceLocation(new ResourceLocation(RealElevatorCore.ID, BLOCKSTATE_ID + "32"), "part=2")
            new ModelResourceLocation(PART31, "type=" + EleVeneerType.getSurfaceFromId(0).name().toLowerCase(Locale.ROOT)),
            new ModelResourceLocation(PART31, "type=" + EleVeneerType.getSurfaceFromId(0).name().toLowerCase(Locale.ROOT))
    };

    private static final ModelResourceLocation[] TALL4 = {
//            new ModelResourceLocation(new ResourceLocation(RealElevatorCore.ID, BLOCKSTATE_ID + "41"), "part=1"),
//            new ModelResourceLocation(new ResourceLocation(RealElevatorCore.ID, BLOCKSTATE_ID + "42"), "part=2")
    };

    private final Minecraft minecraft = Minecraft.getInstance();

    public DoorNoWindowRenderer(EntityRendererManager p_i46179_1_) {
        super(p_i46179_1_);
    }

    @Override
    public ResourceLocation getTextureLocation(ElevatorDoorNoWindow el) {
        return AtlasTexture.LOCATION_BLOCKS;
    }

    @Override
    public void render(ElevatorDoorNoWindow elevatorDoorNoWindow, float v, float v1, MatrixStack matrixStack, IRenderTypeBuffer buffer, int lightLevel) {
        Vector3d offset = this.getRenderOffset(elevatorDoorNoWindow, v1);
        matrixStack.pushPose();
        int l1 =WorldRenderer.getLightColor(elevatorDoorNoWindow.level, new BlockPos(MathHelper.floor(elevatorDoorNoWindow.getX()), MathHelper.floor(elevatorDoorNoWindow.getY()), MathHelper.floor(elevatorDoorNoWindow.getZ())));

        float posFixX = Math.abs(elevatorDoorNoWindow.yRot) == 180 || elevatorDoorNoWindow.yRot == 90 ? -0.5f :
                elevatorDoorNoWindow.yRot == 0 || elevatorDoorNoWindow.yRot == -90 ? 0.5f : 0;
        float posFixZ = Math.abs(elevatorDoorNoWindow.yRot) == 180|| elevatorDoorNoWindow.yRot == -90 ? -0.5f
                : elevatorDoorNoWindow.yRot == 0 || elevatorDoorNoWindow.yRot == 90 ? 0.5f : 0;

        matrixStack.translate(offset.x() + posFixX, offset.y(), offset.z() + posFixZ);
        matrixStack.scale(1, 1, 1);
        matrixStack.mulPose(Vector3f.YP.rotationDegrees(180 - elevatorDoorNoWindow.yRot));

        BlockRendererDispatcher dispatcher = minecraft.getBlockRenderer();
        ModelManager modelManager = dispatcher.getBlockModelShaper().getModelManager();
        //事前に作っておく
        String bState = "type=" + EleVeneerType.getSurfaceFromId(elevatorDoorNoWindow.surfaceType).name().toLowerCase(Locale.ROOT);
        //ドアの高さが3ブロック
        if (elevatorDoorNoWindow.tall == 3){
            ModelResourceLocation r1 = new ModelResourceLocation(PART31, bState);
            ModelResourceLocation r2 = new ModelResourceLocation(PART32, bState);
            dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer.getBuffer(Atlases.cutoutBlockSheet()),
                    (BlockState) null, modelManager.getModel(r1), 1.0f, 1.0f, 1.0f, l1, OverlayTexture.NO_OVERLAY);
            matrixStack.translate(0, 1.5, 0);
            dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer.getBuffer(Atlases.cutoutBlockSheet()),
                    (BlockState) null, modelManager.getModel(r2), 1.0f, 1.0f, 1.0f, l1, OverlayTexture.NO_OVERLAY);
//            dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer.getBuffer(Atlases.cutoutBlockSheet()),
//                    (BlockState) null, modelManager.getModel(TALL3[0]), 1.0f, 1.0f, 1.0f, l1, OverlayTexture.NO_OVERLAY);
//            matrixStack.translate(0, 1.5, 0);
//            dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer.getBuffer(Atlases.cutoutBlockSheet()),
//                    (BlockState)null, modelManager.getModel(TALL3[1]), 1.0f, 1.0f, 1.0f, l1, OverlayTexture.NO_OVERLAY);

        }else{
            ModelResourceLocation r1 = new ModelResourceLocation(PART41, bState);
            ModelResourceLocation r2 = new ModelResourceLocation(PART42, bState);
            dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer.getBuffer(Atlases.cutoutBlockSheet()),
                    (BlockState)null, modelManager.getModel(r1), 1.0f, 1.0f, 1.0f, l1, OverlayTexture.NO_OVERLAY);
            matrixStack.translate(0, 2, 0);
            dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer.getBuffer(Atlases.cutoutBlockSheet()),
                    (BlockState)null, modelManager.getModel(r2), 1.0f, 1.0f, 1.0f, l1, OverlayTexture.NO_OVERLAY);

//            dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer.getBuffer(Atlases.cutoutBlockSheet()),
//                    (BlockState)null, modelManager.getModel(TALL4[0]), 1.0f, 1.0f, 1.0f, l1, OverlayTexture.NO_OVERLAY);
//            matrixStack.translate(0, 2, 0);
//            dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer.getBuffer(Atlases.cutoutBlockSheet()),
//                    (BlockState)null, modelManager.getModel(TALL4[1]), 1.0f, 1.0f, 1.0f, l1, OverlayTexture.NO_OVERLAY);
        }

        matrixStack.popPose();
        //あああ
//        matrixStack.pushPose();
//        //RealElevatorCore.LOGGER.info("l1 > " + l1 + " , lightLevel > " + lightLevel);
//        matrixStack.translate(offset.x, offset.y, offset.z);
//        matrixStack.mulPose(new Quaternion(0, 180 - elevatorDoorNoWindow.yRot, 0, true));
//        matrixStack.translate(-0.5, 0, -0.06);
//        for (int i = 0; i < elevatorDoorNoWindow.tall; i++){
//            matrixStack.translate(0, i == 0 ? 0 : 1, 0);
//            renderBlockSurface(elevatorDoorNoWindow, v, v1, matrixStack, buffer, lightLevel, dispatcher);
//        }
//        matrixStack.popPose();

        super.render(elevatorDoorNoWindow, v, v1, matrixStack, buffer, lightLevel);
    }

    private static RenderType.State createState(ResourceLocation location){
        return RenderType.State.builder().setTextureState(new RenderState.TextureState(location, false, false)).setTexturingState(new RenderState.TexturingState("door", () ->{
            RenderSystem.enableLighting();
        }, () ->{

        })).createCompositeState(false);
    }

    private static RenderType getTexture(final ResourceLocation resourceLocation){
        RenderType.State state = RenderType.State.builder().setTransparencyState(new RenderState.TransparencyState("translucent_transparency", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            RenderSystem.enableAlphaTest();
        }, () -> {
            RenderSystem.disableBlend();
            RenderSystem.disableAlphaTest();
        })).setTextureState(new RenderState.TextureState(new ResourceLocation(resourceLocation.getNamespace(), "textures/" + resourceLocation.getPath() + ".png"), false /*これをtrueにするとぼやける*/, true /*とくに違いはなかった*/)).createCompositeState(true /*かわらん*/);
        RenderType.State state1 = createState(resourceLocation);
        return RenderType.create("realelevator_texture_block", DefaultVertexFormats.POSITION_TEX, GL11.GL_QUADS, 255, true /*かわんねー*/, true/*えーっと、変わりません！*/, state);
    }

    private void renderBlockSurface(ElevatorDoorNoWindow elevatorDoorNoWindow, float v, float v1, MatrixStack stack, IRenderTypeBuffer buffer, int lightLevel, BlockRendererDispatcher dispatcher){
        IBakedModel model = dispatcher.getBlockModel(elevatorDoorNoWindow.blockState);
        List<BakedQuad> quads = model.getQuads(elevatorDoorNoWindow.blockState, elevatorDoorNoWindow.getDirection(), getFont().random);
        if (quads.size() > 0){
            ResourceLocation blockSurfaceTextureLocation = quads.get(0).getSprite().getName();
            RenderType renderType =getTexture(blockSurfaceTextureLocation);
            renderQuad(renderType, stack, buffer, lightLevel);
        }
    }

    private void renderQuad(RenderType type, MatrixStack stack, IRenderTypeBuffer buffer, int lightLevel){
        Matrix4f matrix4f = stack.last().pose();
        Matrix3f matrix3f = stack.last().normal();
        IVertexBuilder builder = buffer.getBuffer(type);
        builder.vertex(matrix4f, (float) 0, (float) 0, (float) 0).color(0, 0, 0, 0).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightLevel).normal(matrix3f, 0, -1, 0).endVertex();
        builder.vertex(matrix4f, (float) 0, (float) 1, (float) 0).color(0, 0, 0, 0).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightLevel).normal(matrix3f, 0, -1, 0).endVertex();
        builder.vertex(matrix4f, (float) 1, (float) 1, (float) 0).color(0, 0, 0, 0).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightLevel).normal(matrix3f, 0, -1, 0).endVertex();
        builder.vertex(matrix4f, (float) 1, (float) 0, (float) 0).color(0, 0, 0, 0).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lightLevel).normal(matrix3f, 0, -1, 0).endVertex();
    }
}

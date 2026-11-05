package net.lausiv1024.realelevator.client;

import java.util.List;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.Normal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/**
 * ワールドの中に、ゲームのフォントで文字を描く。階床表示器と行先ボタンの階名（フォントで描く設定のとき）と、
 * 行先ボタンに視線を合わせたときに浮かぶ階名に使う。
 *
 * <p>フォントはこのMOD専用の {@value #FONT_PATH}（中身はバニラの英数字と Unifont を参照する）。バニラのフォントを差し替える
 * リソースパックの影響を受けにくくし、このフォントだけを差し替えることもできるようにする。
 */
final class WorldText {
    private static final String FONT_PATH = "indicator";
    static final ResourceLocation FONT = ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, FONT_PATH);
    /** フォントの文字の高さ [フォントの単位]（英数字は7、Unifont は8）。 */
    private static final float GLYPH_HEIGHT = 8;
    /** 面に描く文字を、面から浮かせる量 [ブロック]。 */
    private static final double LIFT = 0.0005;
    /** 浮かぶ階名の大きさ [ブロック／フォントの単位]。 */
    private static final float TAG_SCALE = 0.0125F;

    /** 面に描く文字。area は描く場所（回したあとの、ローカル座標）。 */
    record OnFace(Box area, Normal normal, String text, int rgb, int light) {
    }

    private WorldText() {
    }

    private static Component text(String text) {
        return Component.literal(text).withStyle(Style.EMPTY.withFont(FONT));
    }

    /** texts を描く。ほかの描画のバッファを使い終えてから呼ぶ（文字は別のバッファに描くため）。 */
    static void draw(PoseStack poseStack, MultiBufferSource buffers, List<OnFace> texts) {
        for (OnFace text : texts) {
            draw(poseStack, buffers, text);
        }
    }

    /** 面の向き normal の、場所 area に収まるよう縮めて、中央に描く。 */
    private static void draw(PoseStack poseStack, MultiBufferSource buffers, OnFace onFace) {
        Font font = Minecraft.getInstance().font;
        Component component = text(onFace.text());
        int width = font.width(component);
        if (width == 0) {
            return;
        }
        Box area = onFace.area();
        Normal normal = onFace.normal();
        boolean alongX = normal.x() == 0;
        double areaWidth = alongX ? area.sizeX() : area.sizeZ();
        float scale = (float) Math.min(areaWidth / width, area.sizeY() / GLYPH_HEIGHT);
        double x = normal == Normal.POS_X ? area.maxX() + LIFT : normal == Normal.NEG_X ? area.minX() - LIFT : (area.minX() + area.maxX()) / 2;
        double z = normal == Normal.POS_Z ? area.maxZ() + LIFT : normal == Normal.NEG_Z ? area.minZ() - LIFT : (area.minZ() + area.maxZ()) / 2;
        poseStack.pushPose();
        poseStack.translate(x, (area.minY() + area.maxY()) / 2, z);
        // フォントの文字は +Z の側から見て正しく読める。面の向きへ回す
        float yaw = switch (normal) {
            case POS_Z -> 0;
            case NEG_Z -> 180;
            case POS_X -> 90;
            case NEG_X -> -90;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.scale(scale, -scale, scale);
        font.drawInBatch(component, -width / 2.0F, -GLYPH_HEIGHT / 2, 0xFF000000 | onFace.rgb(), false, poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, onFace.light());
        poseStack.popPose();
    }

    /** ネームタグのように、カメラの方を向いた階名を (x, y, z) に浮かべる。 */
    static void drawTag(PoseStack poseStack, MultiBufferSource buffers, double x, double y, double z, String text, Quaternionf camera) {
        Font font = Minecraft.getInstance().font;
        Component component = text(text);
        float left = -font.width(component) / 2.0F;
        int background = (int) (Minecraft.getInstance().options.getBackgroundOpacity(0.25F) * 255.0F) << 24;
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.mulPose(camera);
        poseStack.scale(TAG_SCALE, -TAG_SCALE, TAG_SCALE);
        // 壁などに隠れても読めるよう、薄く透かして描いてから、そのまま描く
        font.drawInBatch(component, left, 0, 0x20FFFFFF, false, poseStack.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, background,
                LightTexture.FULL_BRIGHT);
        font.drawInBatch(component, left, 0, 0xFFFFFFFF, false, poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0,
                LightTexture.FULL_BRIGHT);
        poseStack.popPose();
    }
}

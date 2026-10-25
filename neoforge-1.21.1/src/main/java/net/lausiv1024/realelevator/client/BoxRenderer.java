package net.lausiv1024.realelevator.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.display.HallLantern;
import net.lausiv1024.realelevator.core.panel.ButtonGlyphs;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.PanelPart;
import net.lausiv1024.realelevator.entity.ButtonHost;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * 箱を描く。ブロックのテクスチャを、各面に1ブロック単位で敷き詰める。
 */
final class BoxRenderer {
    /** 色を掛けて使う白い下地。 */
    private static final ResourceLocation WHITE_BASE = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final ResourceLocation INDICATOR_FACE = ResourceLocation.withDefaultNamespace("block/black_concrete");
    private static final ResourceLocation INDICATOR_DOT = ResourceLocation.withDefaultNamespace("block/white_concrete");
    /** 操作盤の板（仮のステンレス。デザインシステムの素材で差し替える）。 */
    private static final ResourceLocation PLATE = ResourceLocation.withDefaultNamespace("block/iron_block");
    private static final int WHITE = 0xFFFFFF;
    /** ボタンの地の色（明るいステンレス）。 */
    private static final int BUTTON_BODY = 0xC9CDD1;
    /** 消灯している図柄の色。 */
    private static final int GLYPH_OFF = 0x4A4D52;
    /** 車椅子マークの色。 */
    private static final int WHEELCHAIR_BLUE = 0x2F5FB8;
    /** 消灯しているホールランタンの色（くすんだ乳白色）。 */
    private static final int LANTERN_OFF = 0x6E6A60;
    /** ボタンの図柄は毎フレーム組み立て直さないよう覚えておく。 */
    private static final Map<GlyphKey, ButtonGlyphs.Glyph> GLYPHS = new ConcurrentHashMap<>();
    private static final int MAX_GLYPHS = 4096;

    private record GlyphKey(ButtonLayout.Button button, String label) {
    }

    private BoxRenderer() {
    }

    /**
     * ボタンを描く。面に図柄（行先ボタンは階名）を描き、応答灯がついているボタンは、図柄を litRgb の色で、
     * まわりの明るさによらず光って見えるようにする。
     *
     * @param quarterTurns ボタンの座標（かごのローカル座標）を回す回数
     */
    static void renderButtons(PoseStack.Pose pose, VertexConsumer consumer, ButtonHost host, int quarterTurns, int light, int litRgb) {
        TextureAtlasSprite base = blockSprite(WHITE_BASE);
        for (int i = 0; i < host.buttons().size(); i++) {
            ButtonLayout.Button button = host.buttons().get(i);
            renderBox(pose, consumer, button.box().rotateQuarterTurns(quarterTurns), base, light, BUTTON_BODY);
            ButtonGlyphs.Glyph glyph = glyph(button, host.buttonLabel(i));
            boolean on = host.isButtonLit(i);
            for (Box dot : glyph.symbol()) {
                Box rotated = dot.rotateQuarterTurns(quarterTurns);
                if (on) {
                    renderBox(pose, consumer, rotated, base, LightTexture.FULL_BRIGHT, litRgb);
                } else {
                    renderBox(pose, consumer, rotated, base, light, GLYPH_OFF);
                }
            }
        }
    }

    private static ButtonGlyphs.Glyph glyph(ButtonLayout.Button button, String label) {
        if (GLYPHS.size() > MAX_GLYPHS) {
            GLYPHS.clear();
        }
        return GLYPHS.computeIfAbsent(new GlyphKey(button, label), key -> ButtonGlyphs.of(key.button(), key.label()));
    }

    /** 操作盤の板・帯・車椅子マークを描く。 */
    static void renderParts(PoseStack.Pose pose, VertexConsumer consumer, List<PanelPart> parts, int quarterTurns, int light) {
        TextureAtlasSprite plate = blockSprite(PLATE);
        TextureAtlasSprite black = blockSprite(INDICATOR_FACE);
        TextureAtlasSprite base = blockSprite(WHITE_BASE);
        for (PanelPart part : parts) {
            Box box = part.box().rotateQuarterTurns(quarterTurns);
            switch (part.type()) {
                case PLATE -> renderBox(pose, consumer, box, plate, light);
                case BAND, WINDOW -> renderBox(pose, consumer, box, black, light);
                case MARK -> renderBox(pose, consumer, box, base, light, WHEELCHAIR_BLUE);
                case MARK_SYMBOL -> renderBox(pose, consumer, box, base, light, WHITE);
            }
        }
    }

    /**
     * 階床表示器を描く。点灯しているドットは、まわりの明るさによらず rgb の色で光って見えるようにする。
     *
     * @param quarterTurns 表示器の座標（かごのローカル座標）を回す回数
     */
    static void renderIndicator(PoseStack.Pose pose, VertexConsumer consumer, FloorIndicator.Frame frame, int quarterTurns, int light,
                                int rgb) {
        renderBox(pose, consumer, frame.face().rotateQuarterTurns(quarterTurns), blockSprite(INDICATOR_FACE), light);
        TextureAtlasSprite dot = blockSprite(INDICATOR_DOT);
        for (Box box : frame.dots()) {
            renderBox(pose, consumer, box.rotateQuarterTurns(quarterTurns), dot, LightTexture.FULL_BRIGHT, rgb);
        }
    }

    /**
     * ホールランタンを描く。点灯している段は、まわりの明るさによらず rgb の色で光って見えるようにする。
     *
     * @param quarterTurns ランタンの座標（かごのローカル座標）を回す回数
     */
    static void renderLantern(PoseStack.Pose pose, VertexConsumer consumer, List<HallLantern.Section> sections,
                              Set<TravelDirection> lit, int quarterTurns, int light, int rgb) {
        TextureAtlasSprite lens = blockSprite(INDICATOR_DOT);
        for (HallLantern.Section section : sections) {
            Box box = section.box().rotateQuarterTurns(quarterTurns);
            if (section.isLit(lit)) {
                renderBox(pose, consumer, box, lens, LightTexture.FULL_BRIGHT, rgb);
            } else {
                renderBox(pose, consumer, box, lens, light, LANTERN_OFF);
            }
        }
    }

    static TextureAtlasSprite blockSprite(ResourceLocation location) {
        return Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(location);
    }

    static void renderBox(PoseStack.Pose pose, VertexConsumer consumer, Box b, TextureAtlasSprite sprite, int light) {
        renderBox(pose, consumer, b, sprite, light, WHITE);
    }

    /** テクスチャに色 rgb（0xRRGGBB）を掛けて描く。 */
    static void renderBox(PoseStack.Pose pose, VertexConsumer consumer, Box b, TextureAtlasSprite sprite, int light, int rgb) {
        float x0 = (float) b.minX(), y0 = (float) b.minY(), z0 = (float) b.minZ();
        float x1 = (float) b.maxX(), y1 = (float) b.maxY(), z1 = (float) b.maxZ();
        // 各面を外向きに、反時計回りの頂点順で描く。(u, v) は面内の2軸で、どちらも1ブロックごとにテクスチャを繰り返す
        Face face = new Face(pose, consumer, sprite, light, rgb);
        face.draw(0, -1, 0, x0, x1, z0, z1, (u, v) -> new float[] {u, y0, v});
        face.draw(0, 1, 0, x0, x1, z1, z0, (u, v) -> new float[] {u, y1, v});
        face.draw(0, 0, -1, x1, x0, y0, y1, (u, v) -> new float[] {u, v, z0});
        face.draw(0, 0, 1, x0, x1, y0, y1, (u, v) -> new float[] {u, v, z1});
        face.draw(-1, 0, 0, z0, z1, y0, y1, (u, v) -> new float[] {x0, v, u});
        face.draw(1, 0, 0, z1, z0, y0, y1, (u, v) -> new float[] {x1, v, u});
    }

    private interface FacePoint {
        float[] at(float u, float v);
    }

    /** 1つの箱を描くあいだ共通の値。 */
    private record Face(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite, int light, int rgb) {
        /**
         * 面を1ブロックごとのタイルに分けて描く。u は uStart から uEnd へ、v は vStart から vEnd へ進む。
         * 頂点は (uStart,vStart) → (uEnd,vStart) → (uEnd,vEnd) → (uStart,vEnd) の順で、法線の側から見て反時計回りになるよう、
         * 呼び出し側で u・v の向きを決めている。
         */
        void draw(float nx, float ny, float nz, float uStart, float uEnd, float vStart, float vEnd, FacePoint point) {
            float uLength = Math.abs(uEnd - uStart);
            float vLength = Math.abs(vEnd - vStart);
            float uSign = Math.signum(uEnd - uStart);
            float vSign = Math.signum(vEnd - vStart);
            for (float du = 0; du < uLength; du += 1) {
                float tileU = Math.min(1, uLength - du);
                for (float dv = 0; dv < vLength; dv += 1) {
                    float tileV = Math.min(1, vLength - dv);
                    float ua = uStart + uSign * du;
                    float ub = ua + uSign * tileU;
                    float va = vStart + vSign * dv;
                    float vb = va + vSign * tileV;
                    // テクスチャは下から上へ v が減る向き（画像の上が上）になるよう、1 − v で貼る
                    vertex(point.at(ua, va), sprite.getU(0), sprite.getV(1), nx, ny, nz);
                    vertex(point.at(ub, va), sprite.getU(tileU), sprite.getV(1), nx, ny, nz);
                    vertex(point.at(ub, vb), sprite.getU(tileU), sprite.getV(1 - tileV), nx, ny, nz);
                    vertex(point.at(ua, vb), sprite.getU(0), sprite.getV(1 - tileV), nx, ny, nz);
                }
            }
        }

        private void vertex(float[] p, float u, float v, float nx, float ny, float nz) {
            consumer.addVertex(pose, p[0], p[1], p[2])
                    .setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}

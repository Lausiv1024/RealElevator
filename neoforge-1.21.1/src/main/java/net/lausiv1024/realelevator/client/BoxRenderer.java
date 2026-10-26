package net.lausiv1024.realelevator.client;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.design.InteriorGeometry;
import net.lausiv1024.realelevator.core.design.LandingLayout;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.door.DoorWindows;
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
    /** かごと乗場の操作盤の板、ランタンの背面プレートの素材（ステンレスのヘアライン）。 */
    static final String STAINLESS = "realelevator:stainless_hairline";
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

    /** かごの外側の面のテクスチャ（亜鉛メッキ調）。 */
    static final ResourceLocation GALVANIZED = ResourceLocation.fromNamespaceAndPath("realelevator", "block/galvanized");

    /** 面の見た目と明るさ。 */
    record Look(TextureAtlasSprite sprite, int light, int rgb) {
        Look(TextureAtlasSprite sprite, int light) {
            this(sprite, light, WHITE);
        }
    }

    /**
     * かごの部位を描く。面ごとに、かごの中を向く面（面の少し外側の点がかごの外形 outer の中にある面）は inside で、
     * かごの外を向く面は outside で描く。かごの中の照明の明るさが、外から見た面に効かないようにするため。
     */
    static void renderCarBox(PoseStack.Pose pose, VertexConsumer consumer, Box b, Box outer, Look inside, Look outside) {
        float x0 = (float) b.minX(), y0 = (float) b.minY(), z0 = (float) b.minZ();
        float x1 = (float) b.maxX(), y1 = (float) b.maxY(), z1 = (float) b.maxZ();
        double cx = (b.minX() + b.maxX()) / 2;
        double cy = (b.minY() + b.maxY()) / 2;
        double cz = (b.minZ() + b.maxZ()) / 2;
        double e = 0.01;
        face(pose, consumer, inside(outer, cx, b.minY() - e, cz) ? inside : outside).draw(0, -1, 0, x0, x1, z0, z1, (u, v) -> new float[] {u, y0, v});
        face(pose, consumer, inside(outer, cx, b.maxY() + e, cz) ? inside : outside).draw(0, 1, 0, x0, x1, z1, z0, (u, v) -> new float[] {u, y1, v});
        face(pose, consumer, inside(outer, cx, cy, b.minZ() - e) ? inside : outside).draw(0, 0, -1, x1, x0, y0, y1, (u, v) -> new float[] {u, v, z0});
        face(pose, consumer, inside(outer, cx, cy, b.maxZ() + e) ? inside : outside).draw(0, 0, 1, x0, x1, y0, y1, (u, v) -> new float[] {u, v, z1});
        face(pose, consumer, inside(outer, b.minX() - e, cy, cz) ? inside : outside).draw(-1, 0, 0, z0, z1, y0, y1, (u, v) -> new float[] {x0, v, u});
        face(pose, consumer, inside(outer, b.maxX() + e, cy, cz) ? inside : outside).draw(1, 0, 0, z1, z0, y0, y1, (u, v) -> new float[] {x1, v, u});
    }

    private static Face face(PoseStack.Pose pose, VertexConsumer consumer, Look look) {
        return new Face(pose, consumer, look.sprite(), look.light(), look.rgb());
    }

    private static boolean inside(Box box, double x, double y, double z) {
        return x > box.minX() && x < box.maxX() && y > box.minY() && y < box.maxY() && z > box.minZ() && z < box.maxZ();
    }

    /** 天井の板（裏側から見える面も含めて、明るい灰色）。 */
    private static final int CEILING_COLOR = 0xD6D6D2;
    /** 天井の光る部分（少し暖かい白）。 */
    private static final int GLOW_COLOR = 0xFFF7E6;
    /** 壁と天井の板の継ぎ目の色。 */
    private static final int SEAM_COLOR = 0x3E3A36;
    /** 手すりの棒の色（暗い茶）。 */
    private static final int HANDRAIL_COLOR = 0x2E2622;
    /** 手すりの金具の色（ステンレス）。 */
    private static final int BRACKET_COLOR = 0xC9CDD1;

    /** かごの天井（かごの中から見える面は明るい灰色の板、外側は outside）を描く。 */
    static void renderCeiling(PoseStack.Pose pose, VertexConsumer consumer, Box box, Box outer, int light, Look outside) {
        renderCarBox(pose, consumer, box, outer, new Look(blockSprite(WHITE_BASE), light, CEILING_COLOR), outside);
    }

    /**
     * かご内装の細部（壁の継ぎ目、幅木、天井の板と照明、手すり）を描く。天井の光る部分は、まわりの明るさによらず光って見えるようにする。
     *
     * @param baseboard 幅木の素材のテクスチャ
     * @param carpet    カーペットのテクスチャ（カーペットがなければ使わない）
     */
    static void renderInterior(PoseStack.Pose pose, VertexConsumer consumer, List<InteriorGeometry.Part> parts, int quarterTurns, int light,
                               TextureAtlasSprite baseboard, TextureAtlasSprite carpet) {
        TextureAtlasSprite base = blockSprite(WHITE_BASE);
        for (InteriorGeometry.Part part : parts) {
            Box box = part.box().rotateQuarterTurns(quarterTurns);
            switch (part.type()) {
                case SEAM -> renderBox(pose, consumer, box, base, light, SEAM_COLOR);
                case BASEBOARD -> renderBox(pose, consumer, box, baseboard, light);
                case CEILING_PANEL -> renderBox(pose, consumer, box, base, light, CEILING_COLOR);
                case CEILING_GLOW -> renderBox(pose, consumer, box, base, LightTexture.FULL_BRIGHT, GLOW_COLOR);
                case HANDRAIL -> renderBox(pose, consumer, box, base, light, HANDRAIL_COLOR);
                case HANDRAIL_BRACKET, MULLION -> renderBox(pose, consumer, box, base, light, BRACKET_COLOR);
                case CARPET -> renderBox(pose, consumer, box, carpet, light);
            }
        }
    }

    /** 防犯窓の網入りガラスと、展望仕様のガラスのテクスチャ（半透明）。 */
    static final ResourceLocation WIRED_GLASS = ResourceLocation.fromNamespaceAndPath("realelevator", "block/wired_glass");
    static final ResourceLocation CLEAR_GLASS = ResourceLocation.fromNamespaceAndPath("realelevator", "block/clear_glass");

    /**
     * 戸板を描く。防犯窓があれば、窓のまわりの枠だけを描き、穴にはめるガラスの箱を glass に加える（あとで {@link #renderGlass} で描く）。
     */
    static void renderDoorPanel(PoseStack.Pose pose, VertexConsumer consumer, Box panel, TextureAtlasSprite sprite, int light,
                                InteriorConfig.SecurityWindow window, List<Box> glass) {
        renderDoorPanel(pose, consumer, panel, sprite, light, light, null, window, glass);
    }

    /**
     * かごの戸板を描く。かごの外形 outer の中を向く面は insideLight、外を向く面は outsideLight の明るさで描く（outer が null ならすべて insideLight）。
     */
    static void renderDoorPanel(PoseStack.Pose pose, VertexConsumer consumer, Box panel, TextureAtlasSprite sprite, int insideLight,
                                int outsideLight, Box outer, InteriorConfig.SecurityWindow window, List<Box> glass) {
        DoorWindows.Pieces pieces = DoorWindows.split(panel, window);
        for (Box piece : pieces.frame()) {
            if (outer == null) {
                renderBox(pose, consumer, piece, sprite, insideLight);
            } else {
                renderCarBox(pose, consumer, piece, outer, new Look(sprite, insideLight), new Look(sprite, outsideLight));
            }
        }
        if (pieces.glass() != null) {
            glass.add(pieces.glass());
        }
    }

    /**
     * 半透明のガラスを描く。consumer は {@code Sheets#translucentCullBlockSheet} のもの。この描画は不透明なもの（かご・戸・三方枠）を
     * 描き終えてからまとめて描かれるので、ガラス越しにそれらが見える（ほかの半透明の描画では、先に描かれて奥のものを隠してしまう）。
     */
    static void renderGlass(PoseStack.Pose pose, VertexConsumer consumer, List<Box> boxes, ResourceLocation texture, int light) {
        TextureAtlasSprite sprite = blockSprite(texture);
        for (Box box : boxes) {
            renderBox(pose, consumer, box, sprite, light);
        }
    }

    /** 敷居の色（アルミ）。 */
    private static final int SILL_COLOR = 0xB4B8BC;
    /** 敷居の溝の色。 */
    private static final int GROOVE_COLOR = 0x3A3C40;
    /** 鍵スイッチの色と鍵穴の色。 */
    private static final int KEY_COLOR = 0x9DA2A8;
    private static final int KEY_SLOT_COLOR = 0x1E2023;

    /**
     * 乗場の枠・敷居・操作盤の部品・ランタンの背面プレートを描く（{@link LandingLayout#parts}、{@link LandingLayout#sill}）。
     *
     * @param frame     枠の素材のテクスチャ
     * @param stainless 操作盤の板とランタンの背面プレートのテクスチャ
     */
    static void renderLanding(PoseStack.Pose pose, VertexConsumer consumer, List<LandingLayout.Part> parts, int quarterTurns, int light,
                              TextureAtlasSprite frame, TextureAtlasSprite stainless) {
        TextureAtlasSprite base = blockSprite(WHITE_BASE);
        TextureAtlasSprite black = blockSprite(INDICATOR_FACE);
        for (LandingLayout.Part part : parts) {
            Box box = part.box().rotateQuarterTurns(quarterTurns);
            switch (part.type()) {
                case FRAME -> renderBox(pose, consumer, box, frame, light);
                case SILL -> renderBox(pose, consumer, box, base, light, SILL_COLOR);
                case SILL_GROOVE -> renderBox(pose, consumer, box, base, light, GROOVE_COLOR);
                case PLATE, LANTERN_PLATE -> renderBox(pose, consumer, box, stainless, light);
                case WINDOW -> renderBox(pose, consumer, box, black, light);
                case MARK -> renderBox(pose, consumer, box, base, light, WHEELCHAIR_BLUE);
                case MARK_SYMBOL -> renderBox(pose, consumer, box, base, light, WHITE);
                case KEY -> renderBox(pose, consumer, box, base, light, KEY_COLOR);
                case KEY_SLOT -> renderBox(pose, consumer, box, base, light, KEY_SLOT_COLOR);
            }
        }
    }

    /**
     * 乗場の枠のまわりの壁を描く。乗場の側の面は、まわりのブロックとテクスチャの継ぎ目がそろうように貼る。
     * 昇降路の側（裏）の面も、同じテクスチャを左右を映して貼る（昇降路の中から見て抜けないように）。三方枠の範囲 range の外周
     * （左右の端・上端・下端）にあたる面も描く（隣にブロックがないときに抜けないように）。
     *
     * @param rgb テクスチャに掛ける色（草などの色付きのブロック用。ふつうは白）
     */
    static void renderWall(PoseStack.Pose pose, VertexConsumer consumer, List<LandingLayout.WallCell> cells, Box range, int quarterTurns,
                           int light, TextureAtlasSprite sprite, int rgb) {
        double e = 1e-6;
        for (LandingLayout.WallCell cell : cells) {
            Box b = cell.box();
            Face edge = new Face(pose, consumer, sprite, light, rgb);
            Box r = b.rotateQuarterTurns(quarterTurns);
            if (b.maxY() >= range.maxY() - e) {
                drawFace(edge, r, 0, 1, 0);
            }
            if (b.minY() <= range.minY() + e) {
                drawFace(edge, r, 0, -1, 0);
            }
            if (b.minX() <= range.minX() + e) {
                float[] n = rotate(-1, 0, quarterTurns);
                drawFace(edge, r, (int) n[0], 0, (int) n[1]);
            }
            if (b.maxX() >= range.maxX() - e) {
                float[] n = rotate(1, 0, quarterTurns);
                drawFace(edge, r, (int) n[0], 0, (int) n[1]);
            }
            float u0 = (float) cell.u();
            float v0 = (float) cell.v();
            float u1 = u0 + (float) b.sizeX();
            float v1 = v0 + (float) b.sizeY();
            float[] normal = rotate(0, 1, quarterTurns);
            Face face = new Face(pose, consumer, sprite, light, rgb);
            // 乗場の側（+Z）から見て反時計回り。テクスチャは下から上へ v が減る向き
            face.vertex(point(b.minX(), b.minY(), b.maxZ(), quarterTurns), sprite.getU(u0), sprite.getV(1 - v0), normal[0], 0, normal[1]);
            face.vertex(point(b.maxX(), b.minY(), b.maxZ(), quarterTurns), sprite.getU(u1), sprite.getV(1 - v0), normal[0], 0, normal[1]);
            face.vertex(point(b.maxX(), b.maxY(), b.maxZ(), quarterTurns), sprite.getU(u1), sprite.getV(1 - v1), normal[0], 0, normal[1]);
            face.vertex(point(b.minX(), b.maxY(), b.maxZ(), quarterTurns), sprite.getU(u0), sprite.getV(1 - v1), normal[0], 0, normal[1]);
            // 裏の面（−Z から見て反時計回り）
            face.vertex(point(b.maxX(), b.minY(), b.minZ(), quarterTurns), sprite.getU(u1), sprite.getV(1 - v0), -normal[0], 0, -normal[1]);
            face.vertex(point(b.minX(), b.minY(), b.minZ(), quarterTurns), sprite.getU(u0), sprite.getV(1 - v0), -normal[0], 0, -normal[1]);
            face.vertex(point(b.minX(), b.maxY(), b.minZ(), quarterTurns), sprite.getU(u0), sprite.getV(1 - v1), -normal[0], 0, -normal[1]);
            face.vertex(point(b.maxX(), b.maxY(), b.minZ(), quarterTurns), sprite.getU(u1), sprite.getV(1 - v1), -normal[0], 0, -normal[1]);
        }
    }

    /** 箱 b の、外向きの法線が (nx, ny, nz) の面を描く（テクスチャは1ブロックごとに敷き詰める）。 */
    private static void drawFace(Face face, Box b, int nx, int ny, int nz) {
        float x0 = (float) b.minX(), y0 = (float) b.minY(), z0 = (float) b.minZ();
        float x1 = (float) b.maxX(), y1 = (float) b.maxY(), z1 = (float) b.maxZ();
        if (ny < 0) {
            face.draw(0, -1, 0, x0, x1, z0, z1, (u, v) -> new float[] {u, y0, v});
        } else if (ny > 0) {
            face.draw(0, 1, 0, x0, x1, z1, z0, (u, v) -> new float[] {u, y1, v});
        } else if (nz < 0) {
            face.draw(0, 0, -1, x1, x0, y0, y1, (u, v) -> new float[] {u, v, z0});
        } else if (nz > 0) {
            face.draw(0, 0, 1, x0, x1, y0, y1, (u, v) -> new float[] {u, v, z1});
        } else if (nx < 0) {
            face.draw(-1, 0, 0, z0, z1, y0, y1, (u, v) -> new float[] {x0, v, u});
        } else {
            face.draw(1, 0, 0, z1, z0, y0, y1, (u, v) -> new float[] {x1, v, u});
        }
    }

    /** ローカル座標の点を、{@link Box#rotateQuarterTurns} と同じ向きに回したもの。 */
    private static float[] point(double x, double y, double z, int quarterTurns) {
        float[] xz = rotate((float) x, (float) z, quarterTurns);
        return new float[] {xz[0], (float) y, xz[1]};
    }

    /** (x, z) を Y軸まわりに90°ずつ回す（(x, z) → (−z, x)）。 */
    private static float[] rotate(float x, float z, int quarterTurns) {
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
            float nx = -z;
            z = x;
            x = nx;
        }
        return new float[] {x, z};
    }

    /** 操作盤の板・帯・車椅子マークを描く。 */
    static void renderParts(PoseStack.Pose pose, VertexConsumer consumer, List<PanelPart> parts, int quarterTurns, int light) {
        TextureAtlasSprite plate = MaterialSprites.side(STAINLESS);
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

        void vertex(float[] p, float u, float v, float nx, float ny, float nz) {
            consumer.addVertex(pose, p[0], p[1], p[2])
                    .setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, nx, ny, nz);
        }
    }
}

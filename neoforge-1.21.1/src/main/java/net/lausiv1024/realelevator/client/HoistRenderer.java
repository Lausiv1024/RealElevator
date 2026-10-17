package net.lausiv1024.realelevator.client;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Point;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism.Sheave;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

/**
 * 昇降路の機構（巻上機・シーブ・ロープ・釣合い重り）の仮の描画。形と動きは core の {@link HoistMechanism} から求める。
 * 色はバニラのブロックのテクスチャに色を掛けて付ける（素材はデザインシステムで差し替える）。
 */
final class HoistRenderer {
    private static final ResourceLocation PLAIN = ResourceLocation.withDefaultNamespace("block/white_concrete");
    private static final ResourceLocation METAL = ResourceLocation.withDefaultNamespace("block/iron_block");
    private static final int ROPE = 0x3A3A3A;
    private static final int SHEAVE_RIM = 0x9A9A9A;
    /** シーブの側面の、回っているのがわかるよう暗くする扇形の色。 */
    private static final int SHEAVE_SPOKE = 0x4A4A4A;
    private static final int SHEAVE_FACE = 0x7A7A7A;
    private static final int MOTOR = 0x5C6670;
    private static final int COUNTERWEIGHT = 0x6A6A6A;
    private static final int RAIL = 0xB0B0B0;
    private static final int BRACKET = 0x606060;
    /** シーブを近似する多角形の辺の数と、そのうち暗くする扇形の間隔。 */
    private static final int SHEAVE_SIDES = 12;
    private static final int SPOKE_EVERY = 3;

    private HoistRenderer() {
    }

    /**
     * かごのレンダラーから呼ぶ。poseStack の原点は、バニラが補間したかごの位置。
     */
    static void render(ElevatorCarEntity car, float partialTick, PoseStack poseStack, VertexConsumer consumer) {
        HoistMechanism hoist = car.hoist();
        if (hoist == null) {
            return;
        }
        double carY = car.renderY(partialTick);
        Frame frame = new Frame(poseStack.last(), consumer, car.level(), car.getX(), car.getZ(),
                Mth.lerp(partialTick, car.yo, car.getY()), car.facing().get2DDataValue());
        TextureAtlasSprite plain = BoxRenderer.blockSprite(PLAIN);
        TextureAtlasSprite metal = BoxRenderer.blockSprite(METAL);

        frame.box(hoist.motor(), plain, MOTOR);
        for (Box rail : hoist.counterweightRails()) {
            frame.box(rail, metal, RAIL);
        }
        for (Box hitch : hoist.hitches()) {
            frame.box(hitch, plain, BRACKET);
        }
        frame.box(hoist.counterweight(carY), metal, COUNTERWEIGHT);
        for (Box bracket : hoist.counterweightSheaveBrackets(carY)) {
            frame.box(bracket, plain, BRACKET);
        }
        for (Box bracket : hoist.carSheaveBrackets(carY)) {
            frame.box(bracket, plain, BRACKET);
        }
        for (Sheave sheave : hoist.sheaves(carY)) {
            frame.sheave(sheave, plain);
        }
        double ropeHalf = HoistMechanism.ROPE_WIDTH_PX / 16 / 2;
        for (List<Point> rope : hoist.ropes(carY)) {
            for (int i = 1; i < rope.size(); i++) {
                frame.beam(rope.get(i - 1), rope.get(i), ropeHalf, plain, ROPE);
            }
        }
    }

    /**
     * かごのローカル座標（高さはワールドのY座標）から、描画の座標への変換と、頂点の書き出し。
     *
     * @param baseY        poseStack の原点のY座標
     * @param quarterTurns かごの向き（{@link Box#rotateQuarterTurns} と同じ回し方）
     */
    private record Frame(PoseStack.Pose pose, VertexConsumer consumer, Level level, double carX, double carZ,
                         double baseY, int quarterTurns) {
        void box(Box local, TextureAtlasSprite sprite, int rgb) {
            Box box = local.rotateQuarterTurns(quarterTurns).move(0, -baseY, 0);
            int light = lightAt((box.minX() + box.maxX()) / 2, (box.minY() + box.maxY()) / 2 + baseY, (box.minZ() + box.maxZ()) / 2);
            BoxRenderer.renderBox(pose, consumer, box, sprite, light, rgb);
        }

        /** 太さ 2×half の四角い棒を a から b へ描く。 */
        void beam(Point a, Point b, double half, TextureAtlasSprite sprite, int rgb) {
            double[] p = toRender(a);
            double[] q = toRender(b);
            double[] d = normalize(q[0] - p[0], q[1] - p[1], q[2] - p[2]);
            if (d == null) {
                return;
            }
            double[] helper = Math.abs(d[1]) > 0.9 ? new double[] {1, 0, 0} : new double[] {0, 1, 0};
            double[] s = normalize(cross(d, helper));
            double[] t = cross(d, s);
            int light = lightAt((p[0] + q[0]) / 2, (p[1] + q[1]) / 2 + baseY, (p[2] + q[2]) / 2);
            // 棒の4つの側面。s, t の向きに half ずつ広げる
            double[][] around = {{1, 1}, {-1, 1}, {-1, -1}, {1, -1}};
            for (int i = 0; i < 4; i++) {
                double[] c0 = around[i];
                double[] c1 = around[(i + 1) % 4];
                double[] n = normalize(s[0] * (c0[0] + c1[0]) + t[0] * (c0[1] + c1[1]),
                        s[1] * (c0[0] + c1[0]) + t[1] * (c0[1] + c1[1]),
                        s[2] * (c0[0] + c1[0]) + t[2] * (c0[1] + c1[1]));
                double[] o0 = offset(s, t, c0, half);
                double[] o1 = offset(s, t, c1, half);
                quad(sprite, rgb, light, n,
                        add(p, o0), add(q, o0), add(q, o1), add(p, o1));
            }
        }

        /** シーブを多角柱で描く。側面の扇形を一定の間隔で暗くし、回っているのがわかるようにする。 */
        void sheave(Sheave sheave, TextureAtlasSprite sprite) {
            Point c = sheave.centre();
            // 軸と、回転の基準になる2つの向き（u から v へが axis まわりの正の回転）
            double[] axis = sheave.axis() == HoistMechanism.Axis.X ? new double[] {1, 0, 0} : new double[] {0, 0, 1};
            double[] u = sheave.axis() == HoistMechanism.Axis.X ? new double[] {0, 1, 0} : new double[] {1, 0, 0};
            double[] v = sheave.axis() == HoistMechanism.Axis.X ? new double[] {0, 0, 1} : new double[] {0, 1, 0};
            double half = sheave.width() / 2;
            double r = sheave.radius();
            double[] at = toRender(c);
            int light = lightAt(at[0], at[1] + baseY, at[2]);

            double[][] front = new double[SHEAVE_SIDES][];
            double[][] back = new double[SHEAVE_SIDES][];
            for (int i = 0; i < SHEAVE_SIDES; i++) {
                double a = sheave.angle() + 2 * Math.PI * i / SHEAVE_SIDES;
                double ox = r * (Math.cos(a) * u[0] + Math.sin(a) * v[0]);
                double oy = r * (Math.cos(a) * u[1] + Math.sin(a) * v[1]);
                double oz = r * (Math.cos(a) * u[2] + Math.sin(a) * v[2]);
                front[i] = toRender(new Point(c.x() + ox + axis[0] * half, c.y() + oy, c.z() + oz + axis[2] * half));
                back[i] = toRender(new Point(c.x() + ox - axis[0] * half, c.y() + oy, c.z() + oz - axis[2] * half));
            }
            double[] frontCentre = toRender(new Point(c.x() + axis[0] * half, c.y(), c.z() + axis[2] * half));
            double[] backCentre = toRender(new Point(c.x() - axis[0] * half, c.y(), c.z() - axis[2] * half));
            double[] n = rotate(axis);
            double[] nBack = {-n[0], -n[1], -n[2]};
            for (int i = 0; i < SHEAVE_SIDES; i++) {
                int j = (i + 1) % SHEAVE_SIDES;
                // 側面の扇形（三角形を、最後の頂点を重ねた四角形として描く）
                int face = i % SPOKE_EVERY == 0 ? SHEAVE_SPOKE : SHEAVE_FACE;
                quad(sprite, face, light, n, frontCentre, front[i], front[j], front[j]);
                quad(sprite, face, light, nBack, backCentre, back[j], back[i], back[i]);
                // 外周
                double[] radial = normalize(front[i][0] + front[j][0] - 2 * frontCentre[0],
                        front[i][1] + front[j][1] - 2 * frontCentre[1],
                        front[i][2] + front[j][2] - 2 * frontCentre[2]);
                quad(sprite, SHEAVE_RIM, light, radial, back[i], back[j], front[j], front[i]);
            }
        }

        private void quad(TextureAtlasSprite sprite, int rgb, int light, double[] n,
                          double[] a, double[] b, double[] c, double[] d) {
            // 色だけを使うので、テクスチャの中央の小さな範囲を貼る
            float u0 = sprite.getU(0.25f);
            float u1 = sprite.getU(0.75f);
            float v0 = sprite.getV(0.25f);
            float v1 = sprite.getV(0.75f);
            vertex(a, u0, v1, n, rgb, light);
            vertex(b, u1, v1, n, rgb, light);
            vertex(c, u1, v0, n, rgb, light);
            vertex(d, u0, v0, n, rgb, light);
        }

        private void vertex(double[] p, float u, float v, double[] n, int rgb, int light) {
            consumer.addVertex(pose, (float) p[0], (float) p[1], (float) p[2])
                    .setColor((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 255)
                    .setUv(u, v)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(light)
                    .setNormal(pose, (float) n[0], (float) n[1], (float) n[2]);
        }

        /** かごのローカル座標の点を、描画の座標（かごの向きに回し、Y は原点からの差）にする。 */
        private double[] toRender(Point p) {
            double[] r = rotate(new double[] {p.x(), p.y(), p.z()});
            r[1] -= baseY;
            return r;
        }

        /** 向きを Y 軸まわりに回す（{@link Box#rotateQuarterTurns} と同じ、1回で (x, z) → (−z, x)）。 */
        private double[] rotate(double[] v) {
            double x = v[0];
            double z = v[2];
            for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
                double nx = -z;
                z = x;
                x = nx;
            }
            return new double[] {x, v[1], z};
        }

        /** 描画の座標（Y はワールド）での明るさ。 */
        private int lightAt(double x, double worldY, double z) {
            return LevelRenderer.getLightColor(level, BlockPos.containing(carX + x, worldY, carZ + z));
        }
    }

    private static double[] offset(double[] s, double[] t, double[] corner, double half) {
        return new double[] {
                (s[0] * corner[0] + t[0] * corner[1]) * half,
                (s[1] * corner[0] + t[1] * corner[1]) * half,
                (s[2] * corner[0] + t[2] * corner[1]) * half};
    }

    private static double[] add(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double[] normalize(double[] v) {
        return normalize(v[0], v[1], v[2]);
    }

    private static double[] normalize(double x, double y, double z) {
        double length = Math.sqrt(x * x + y * y + z * z);
        return length < 1e-9 ? null : new double[] {x / length, y / length, z / length};
    }
}

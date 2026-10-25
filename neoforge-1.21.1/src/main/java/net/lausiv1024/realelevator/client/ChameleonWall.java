package net.lausiv1024.realelevator.client;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.design.LandingLayout;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 乗場の枠のまわりの壁を、まわりの壁のブロックに合わせる（カメレオン）。三方枠の範囲のすぐ左右と上の、同じ壁の列のブロックのうち、
 * いちばん多い不透明な立方体のブロックの見た目をまねる。まわりにブロックがなければ滑らかな石。
 *
 * <p>壁を張り替えると追従するよう、乗場ごとに {@value #REFRESH_TICKS} tick おきに調べ直す。
 */
final class ChameleonWall {
    private static final int REFRESH_TICKS = 20;
    private static final BlockState FALLBACK = Blocks.SMOOTH_STONE.defaultBlockState();

    /** まねるブロックと、その面の色（草などの色付きのブロック用）。 */
    record Look(TextureAtlasSprite sprite, int rgb) {
    }

    private record Entry(Look look, long time) {
    }

    private static final Map<LandingDoorEntity, Entry> CACHE = new WeakHashMap<>();

    private ChameleonWall() {
    }

    static Look look(LandingDoorEntity door) {
        Level level = door.level();
        long time = level.getGameTime();
        Entry entry = CACHE.get(door);
        if (entry == null || time - entry.time() >= REFRESH_TICKS || time < entry.time()) {
            entry = new Entry(sample(door), time);
            CACHE.put(door, entry);
        }
        return entry.look();
    }

    private static Look sample(LandingDoorEntity door) {
        Level level = door.level();
        Box range = LandingLayout.bounds(door.spec(), door.faceOffset());
        double half = range.maxX();
        // 壁の列の中心（乗場の面の 0.5 ブロック奥）
        double z = DoorGeometry.landingFace(door.spec(), door.faceOffset()) - 0.5;
        int height = DoorGeometry.LANDING_FRAME_HEIGHT;
        Map<BlockState, Integer> counts = new LinkedHashMap<>();
        Map<BlockState, BlockPos> where = new LinkedHashMap<>();
        for (int y = 0; y < height; y++) {
            count(door, level, -half - 0.5, y + 0.5, z, counts, where);
            count(door, level, half + 0.5, y + 0.5, z, counts, where);
        }
        for (double x = -half + 0.5; x < half; x += 1) {
            count(door, level, x, height + 0.5, z, counts, where);
        }
        BlockState best = FALLBACK;
        int bestCount = 0;
        for (Map.Entry<BlockState, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        BakedQuad quad = MaterialSprites.quad(best, landingDirection(door));
        if (quad == null) {
            return new Look(MaterialSprites.side("minecraft:smooth_stone"), 0xFFFFFF);
        }
        int rgb = 0xFFFFFF;
        BlockPos pos = where.get(best);
        if (quad.isTinted() && pos != null) {
            rgb = Minecraft.getInstance().getBlockColors().getColor(best, level, pos, quad.getTintIndex()) & 0xFFFFFF;
        }
        return new Look(quad.getSprite(), rgb);
    }

    /** ローカル座標 (x, y, z) のブロックが不透明な立方体なら数える。 */
    private static void count(LandingDoorEntity door, Level level, double x, double y, double z, Map<BlockState, Integer> counts,
                              Map<BlockState, BlockPos> where) {
        Box local = new Box(x, y, z, x, y, z).rotateQuarterTurns(door.quarterTurns());
        BlockPos pos = BlockPos.containing(door.getX() + local.minX(), door.getY() + local.minY(), door.getZ() + local.minZ());
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() && state.isSolidRender(level, pos)) {
            counts.merge(state, 1, Integer::sum);
            where.putIfAbsent(state, pos);
        }
    }

    /** 乗場の側（ローカル座標の +Z）のワールドの向き。 */
    static Direction landingDirection(LandingDoorEntity door) {
        Box local = new Box(0, 0, 1, 0, 0, 1).rotateQuarterTurns(door.quarterTurns());
        return Direction.getNearest(local.minX(), 0, local.minZ());
    }
}

package net.lausiv1024.realelevator.entity;

import net.minecraft.world.level.Level;

/**
 * かごの運転曲線を読む時刻（かごの位置・当たり判定・乗っているエンティティの運搬・描画に使う）。
 *
 * <p>サーバーではゲーム時刻そのもの。クライアントのゲーム時刻は、サーバーから1秒ごとに届く時刻で上書きされるので、
 * サーバーが少しでも遅れると1〜2tick巻き戻り、走行中のかごと乗っているプレイヤーがまとめて跳ねる（速いほど大きい）。
 * そこでクライアントでは、毎tick 1ずつ進め、ゲーム時刻とのずれを1tickあたり最大 {@value #MAX_CORRECTION} tick ずつ寄せる。
 * ずれが {@value #SNAP_TICKS} tick を超えたら（ワールドに入った直上など）、そのまま合わせる。
 */
public final class ElevatorClock {
    private static final double MAX_CORRECTION = 0.2;
    private static final double SNAP_TICKS = 20;

    private static double clientTime = Double.NaN;
    private static Level clientLevel;

    private ElevatorClock() {
    }

    /** level での今の時刻 [tick]。 */
    public static double now(Level level) {
        if (level.isClientSide && level == clientLevel && !Double.isNaN(clientTime)) {
            return clientTime;
        }
        return level.getGameTime();
    }

    /**
     * クライアントのtickの初め（ワールドのtickより前）に呼ぶ。
     *
     * @param advance このtickでワールドの時刻が進むか（一時停止中や、tickを止めているときは進まない）
     */
    public static void tickClient(Level level, boolean advance) {
        double expected = level.getGameTime() + (advance ? 1 : 0);
        if (level != clientLevel || Double.isNaN(clientTime) || Math.abs(expected - clientTime) > SNAP_TICKS) {
            clientLevel = level;
            clientTime = expected;
            return;
        }
        if (advance) {
            clientTime += 1;
        }
        clientTime += Math.clamp(expected - clientTime, -MAX_CORRECTION, MAX_CORRECTION);
    }
}

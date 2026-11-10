package net.lausiv1024.realelevator.core.announce;

/**
 * 到着チャイムの種類。種類ごとに、群管理の即時予報・上り・下りの3つの音をまとめて切り替える。
 *
 * <p>音は {@code tools/sounds/make_chime.py} で合成する。和音は同時に鳴らす。
 * <ul>
 * <li>チャイム1：予報 G#5、上り G#5 → C6、下り C6 → G#5（間隔0.45秒）</li>
 * <li>チャイム2：予報 [G5, A#5]、上り D#5 → G5、下り G5 → D#5（間隔0.25秒）</li>
 * <li>チャイム3：予報 [G5, A#5]、上り C5 → F5 → G5 → [A5, C6]、下り C6 → G5 → F5 → C5（間隔0.25秒）</li>
 * <li>チャイム4：予報 [G5, A#5]、上りも下りも C5 → [F5, A4]（間隔0.3秒）</li>
 * </ul>
 */
public enum ChimeType {
    /** 鳴らさない。鳴らさなくても、ホールランタンは鳴らすはずだったタイミングで点滅を始める。 */
    NONE(0),
    CHIME_1(41),
    CHIME_2(61),
    CHIME_3(71),
    CHIME_4(62);

    private final int ringTicks;

    ChimeType(int ringTicks) {
        this.ringTicks = ringTicks;
    }

    /**
     * 到着チャイムが鳴っている長さ [tick]（最後の音を鳴らすまでの間隔＋最後の音の余韻。余韻はチャイム1が1.6秒、チャイム2〜4が2.8秒）。
     * そのあいだは同じ階で重ねて鳴らさない。
     */
    public int ringTicks() {
        return ringTicks;
    }

    public boolean rings() {
        return this != NONE;
    }
}

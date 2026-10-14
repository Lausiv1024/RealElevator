package net.lausiv1024.realelevator.core.control;

/**
 * 運転の時間に関する値 [tick]。
 */
public final class OperationTiming {
    /** 戸が閉まって発車を決めてから、実際に動き始めるまでの遅れ（0.5秒）。実機のブレーキ解放などにかかる時間を模す。 */
    public static final int START_DELAY_TICKS = 10;

    private OperationTiming() {
    }
}

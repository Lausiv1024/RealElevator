package net.lausiv1024.realelevator.core.control;

/**
 * 運転の時間に関する値 [tick]。
 */
public final class OperationTiming {
    /** 戸が閉まって発車を決めてから、実際に動き始めるまでの遅れ（0.5秒）。実機のブレーキ解放などにかかる時間を模す。 */
    public static final int START_DELAY_TICKS = 10;
    /** 行先ボタンの2度押しキャンセルで、応答灯の点いたボタンを2回続けて押したとみなす間隔の上限（0.5秒）。 */
    public static final int DOUBLE_PRESS_TICKS = 10;
    /** 停電で着床したとき、乗客が降りるまで戸を開けておく時間（10秒）。そのあと閉めて休止する。 */
    public static final int OUTAGE_DOOR_OPEN_TICKS = 200;

    private OperationTiming() {
    }
}

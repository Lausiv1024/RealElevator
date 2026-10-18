package net.lausiv1024.realelevator.core.door;

import net.lausiv1024.realelevator.core.control.CallType;

/**
 * 戸の開閉時間と開放待機時間 [tick]。寸法と速度がどちらも実機の1.5倍なので、時間は実機と同じになる。
 *
 * @param wheelchairDwellTicks 車椅子用操作盤・乗場の車椅子用ボタンで呼ばれたときの開放待機
 * @param wheelchairCloseTicks 車椅子用操作盤・乗場の車椅子用ボタンで呼ばれたときの、閉じるのにかかる時間（ゆっくり閉じる）
 */
public record DoorTiming(int openTicks, int closeTicks, int hallDwellTicks, int carDwellTicks,
                         int wheelchairDwellTicks, int wheelchairCloseTicks) {
    /** 開くのに2.0秒、閉じるのに2.5秒。開放待機は乗場呼びで4秒、かご呼びで2秒。車椅子用は開放待機10秒で、閉じるのに約3.8秒（1.5倍）。 */
    public static final DoorTiming STANDARD = new DoorTiming(40, 50, 80, 40, 200, 75);

    public DoorTiming {
        if (openTicks <= 0 || closeTicks <= 0 || hallDwellTicks < 0 || carDwellTicks < 0
                || wheelchairDwellTicks < 0 || wheelchairCloseTicks <= 0) {
            throw new IllegalArgumentException("invalid door timing");
        }
    }

    public int dwellTicks(CallType callType) {
        return switch (callType) {
            case HALL -> hallDwellTicks;
            case CAR -> carDwellTicks;
        };
    }

    /** 呼びの種類 callType で、車椅子用のボタンから呼ばれたかどうかを含めた開放待機。 */
    public int dwellTicks(CallType callType, boolean wheelchair) {
        return wheelchair ? Math.max(wheelchairDwellTicks, dwellTicks(callType)) : dwellTicks(callType);
    }
}

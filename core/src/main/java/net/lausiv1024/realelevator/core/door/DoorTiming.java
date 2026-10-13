package net.lausiv1024.realelevator.core.door;

import net.lausiv1024.realelevator.core.control.CallType;

/**
 * 戸の開閉時間と開放待機時間 [tick]。寸法と速度がどちらも実機の1.5倍なので、時間は実機と同じになる。
 */
public record DoorTiming(int openTicks, int closeTicks, int hallDwellTicks, int carDwellTicks) {
    /** 開くのに2.0秒、閉じるのに2.5秒。開放待機は乗場呼びで4秒、かご呼びで2秒。 */
    public static final DoorTiming STANDARD = new DoorTiming(40, 50, 80, 40);

    public DoorTiming {
        if (openTicks <= 0 || closeTicks <= 0 || hallDwellTicks < 0 || carDwellTicks < 0) {
            throw new IllegalArgumentException("invalid door timing");
        }
    }

    public int dwellTicks(CallType callType) {
        return switch (callType) {
            case HALL -> hallDwellTicks;
            case CAR -> carDwellTicks;
        };
    }
}

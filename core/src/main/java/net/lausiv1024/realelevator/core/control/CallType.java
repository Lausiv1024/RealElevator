package net.lausiv1024.realelevator.core.control;

/**
 * 呼びの種類。開放待機時間の長さが変わる。
 */
public enum CallType {
    /** 乗場呼び（▲▼ボタン）。乗る人を待つので開放待機を長めにする。 */
    HALL,
    /** かご呼び（行先ボタン）。降りるだけなので開放待機を短めにする。 */
    CAR
}

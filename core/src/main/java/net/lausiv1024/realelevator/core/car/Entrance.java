package net.lausiv1024.realelevator.core.car;

/**
 * かごの出入口。二方向出入口（前後・スルー型）では、正面と奥の両方にある。
 */
public enum Entrance {
    /** 正面（設置用ブロックのある乗場の側）。 */
    FRONT,
    /** 奥。正面の形を、かごの中心のまわりに180°回したもの。 */
    REAR;

    /** かごのローカル座標で、正面の形を何回90°回すとこの出入口の形になるか。 */
    public int quarterTurns() {
        return this == FRONT ? 0 : 2;
    }
}

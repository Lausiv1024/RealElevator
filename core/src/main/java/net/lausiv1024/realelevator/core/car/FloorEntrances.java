package net.lausiv1024.realelevator.core.car;

import java.util.EnumSet;
import java.util.Set;

/**
 * 二方向出入口で、ある階で使う出入口（前／後／両方）。一方向出入口のかごでは、どの階も前だけ。
 */
public enum FloorEntrances {
    FRONT(EnumSet.of(Entrance.FRONT)),
    REAR(EnumSet.of(Entrance.REAR)),
    BOTH(EnumSet.of(Entrance.FRONT, Entrance.REAR));

    private final Set<Entrance> entrances;

    FloorEntrances(Set<Entrance> entrances) {
        this.entrances = entrances;
    }

    public boolean has(Entrance entrance) {
        return entrances.contains(entrance);
    }

    /** この階で使う出入口（変更しないこと）。 */
    public Set<Entrance> entrances() {
        return EnumSet.copyOf(entrances);
    }

    /** 最下階（設置用ブロックの階）で選べるか。最下階は正面を必ず使う。 */
    public boolean allowedOnLowestFloor() {
        return has(Entrance.FRONT);
    }
}

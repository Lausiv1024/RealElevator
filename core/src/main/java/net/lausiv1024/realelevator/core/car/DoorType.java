package net.lausiv1024.realelevator.core.car;

/**
 * 扉の方式。左右は乗場から見た向き。
 */
public enum DoorType {
    /** 2枚中央開き（CO）。 */
    CENTER(1),
    /** 2枚片開き（2S）、左開き。開くと2枚とも乗場から見て左（−X 側）の戸袋に入り、出入口は右へ寄る。 */
    SIDE_LEFT(2),
    /** 2枚片開き（2S）、右開き。開くと2枚とも乗場から見て右（+X 側）の戸袋に入り、出入口は左へ寄る。 */
    SIDE_RIGHT(2),
    /** 4枚中央開き（4CO）。左右に2枚ずつ。大型の機種だけで選べ、出入口をCOより広くできる。 */
    FOUR_CENTER(2);

    private final int tracks;

    DoorType(int tracks) {
        this.tracks = tracks;
    }

    /** 戸のレールの本数（かご・乗場それぞれ）。2枚以上が重なって開く方式は、前後に並んだ2本のレールを使う。 */
    public int tracks() {
        return tracks;
    }

    /** 片開き（2S）か。 */
    public boolean isSideOpening() {
        return this == SIDE_LEFT || this == SIDE_RIGHT;
    }

    /** 機種 model で選べるか。 */
    public boolean isAvailable(CarModel model) {
        return this != FOUR_CENTER || model.fourPanelEntranceWidthPx() > 0;
    }
}

package net.lausiv1024.realelevator.core.announce;

import net.lausiv1024.realelevator.core.control.TravelDirection;

/**
 * 1回のアナウンス。サーバーからクライアントへはこれだけを送り、どう鳴らすかはクライアントの再生方法に任せる。
 *
 * @param type       種別
 * @param floorLabel 階名（到着のときだけ使う）
 * @param direction  進行方向（行先方向のときだけ使う）
 */
public record Announcement(Type type, String floorLabel, TravelDirection direction) {
    public enum Type {
        /** 到着して戸を開き始めるとき。「◯階です。」 */
        ARRIVAL,
        /** 戸が開ききったとき。「上へ参ります。」「下へ参ります。」 */
        DIRECTION,
        /** 戸を閉める前。「ドアが閉まります。」 */
        DOOR_CLOSE
    }

    public static Announcement arrival(String floorLabel) {
        return new Announcement(Type.ARRIVAL, floorLabel, TravelDirection.NONE);
    }

    public static Announcement direction(TravelDirection direction) {
        return new Announcement(Type.DIRECTION, "", direction);
    }

    public static Announcement doorClose() {
        return new Announcement(Type.DOOR_CLOSE, "", TravelDirection.NONE);
    }
}

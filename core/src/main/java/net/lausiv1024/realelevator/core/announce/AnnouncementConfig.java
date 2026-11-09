package net.lausiv1024.realelevator.core.announce;

/**
 * アナウンスと到着チャイムの設定。
 *
 * @param arrival   到着して戸を開くときに「◯階です。」を流すか
 * @param doorClose 戸を閉める前に「ドアが閉まります。」を流すか。流すときは、自動で閉めるときだけ流し終わるのを待つ
 * @param chime     到着チャイム（と群管理の即時予報）の種類。なしでも、ホールランタンは同じタイミングで点滅を始める
 */
public record AnnouncementConfig(boolean arrival, boolean doorClose, ChimeType chime) {
    /** アナウンスは流し、到着チャイムは有償の付加仕様なので鳴らさない。 */
    public static final AnnouncementConfig DEFAULT = new AnnouncementConfig(true, true, ChimeType.NONE);

    public AnnouncementConfig withArrival(boolean value) {
        return new AnnouncementConfig(value, doorClose, chime);
    }

    public AnnouncementConfig withDoorClose(boolean value) {
        return new AnnouncementConfig(arrival, value, chime);
    }

    public AnnouncementConfig withChime(ChimeType value) {
        return new AnnouncementConfig(arrival, doorClose, value);
    }
}

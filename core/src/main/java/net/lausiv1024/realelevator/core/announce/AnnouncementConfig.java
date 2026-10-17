package net.lausiv1024.realelevator.core.announce;

/**
 * アナウンスの設定。
 *
 * @param arrival   到着して戸を開くときに「◯階です。」を流すか
 * @param doorClose 戸を閉める前に「ドアが閉まります。」を流すか。流すときは、自動で閉めるときだけ流し終わるのを待つ
 */
public record AnnouncementConfig(boolean arrival, boolean doorClose) {
    public static final AnnouncementConfig DEFAULT = new AnnouncementConfig(true, true);

    public AnnouncementConfig withArrival(boolean value) {
        return new AnnouncementConfig(value, doorClose);
    }

    public AnnouncementConfig withDoorClose(boolean value) {
        return new AnnouncementConfig(arrival, value);
    }
}

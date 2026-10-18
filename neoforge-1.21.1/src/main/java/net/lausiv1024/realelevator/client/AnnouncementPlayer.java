package net.lausiv1024.realelevator.client;

import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;

/**
 * アナウンスの鳴らし方。クライアントごとに差し替えられる（同梱音声、将来は VOICEVOX など）。
 */
public interface AnnouncementPlayer {
    /** car のアナウンスを鳴らす。同じかごで別のアナウンスが鳴っているあいだは、新しいアナウンスは流さずに捨てる（途中で打ち切ったり、流し直したりしない）。 */
    void play(ElevatorCarEntity car, Announcement announcement);

    /** 毎tick呼ばれる。つなげて鳴らす音声の続きを進める。 */
    void tick();
}

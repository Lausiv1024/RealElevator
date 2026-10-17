package net.lausiv1024.realelevator.core.announce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.lausiv1024.realelevator.core.control.TravelDirection;

class AnnouncementScriptTest {
    @Test
    void arrivalSaysTheFloor() {
        assertEquals(List.of("floor_3"), AnnouncementScript.clips(Announcement.arrival("3")));
        assertEquals(List.of("floor_12"), AnnouncementScript.clips(Announcement.arrival("12")));
        assertEquals("3階です。", AnnouncementScript.text(Announcement.arrival("3")));
    }

    @Test
    void directionIsASeparateAnnouncement() {
        assertEquals(List.of("dir_up"), AnnouncementScript.clips(Announcement.direction(TravelDirection.UP)));
        assertEquals(List.of("dir_down"), AnnouncementScript.clips(Announcement.direction(TravelDirection.DOWN)));
        assertEquals(List.of(), AnnouncementScript.clips(Announcement.direction(TravelDirection.NONE)));
        assertEquals("上へ参ります。", AnnouncementScript.text(Announcement.direction(TravelDirection.UP)));
        assertEquals("", AnnouncementScript.text(Announcement.direction(TravelDirection.NONE)));
    }

    @Test
    void basementAndRooftop() {
        assertEquals(List.of("floor_b2"), AnnouncementScript.clips(Announcement.arrival("B2")));
        assertEquals(List.of("floor_r"), AnnouncementScript.clips(Announcement.arrival("R")));
        assertEquals("地下2階です。", AnnouncementScript.text(Announcement.arrival("b2")));
        assertEquals("屋上です。", AnnouncementScript.text(Announcement.arrival("R")));
    }

    @Test
    void unsupportedFloorsAreSkipped() {
        // 同梱音声の範囲外の階名は、階のクリップを流さない
        assertNull(AnnouncementScript.floorClip("51"));
        assertNull(AnnouncementScript.floorClip("B6"));
        assertNull(AnnouncementScript.floorClip("MF"));
        assertNull(AnnouncementScript.floorClip("0"));
        assertEquals(List.of(), AnnouncementScript.clips(Announcement.arrival("51")));
        // 読み上げなら範囲外の数字も読める
        assertEquals("51階です。", AnnouncementScript.text(Announcement.arrival("51")));
        assertEquals("floor_50", AnnouncementScript.floorClip("50"));
    }

    @Test
    void doorClose() {
        assertEquals(List.of("door_close"), AnnouncementScript.clips(Announcement.doorClose()));
        assertEquals("ドアが閉まります。", AnnouncementScript.text(Announcement.doorClose()));
        assertEquals(30, AnnouncementScript.DOOR_CLOSE_WAIT_TICKS);
    }
}

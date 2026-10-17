package net.lausiv1024.realelevator.entity;

import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.minecraft.nbt.CompoundTag;

/**
 * 配置とアナウンスの設定を NBT に読み書きする。かごと制御盤で共通に使う。
 */
public final class ConfigTags {
    private ConfigTags() {
    }

    public static CompoundTag writeFixtures(FixtureConfig fixtures) {
        CompoundTag tag = new CompoundTag();
        tag.putString("PanelSides", fixtures.panelSides().name());
        tag.putString("CarIndicator", fixtures.carIndicator().name());
        tag.putBoolean("LeftPanelIndicator", fixtures.leftPanelIndicator());
        tag.putString("HallIndicator", fixtures.hallIndicator().name());
        tag.putString("HallLantern", fixtures.hallLantern().name());
        return tag;
    }

    public static FixtureConfig readFixtures(CompoundTag tag) {
        FixtureConfig defaults = FixtureConfig.DEFAULT;
        return new FixtureConfig(
                parseEnum(FixtureConfig.PanelSides.class, tag.getString("PanelSides"), defaults.panelSides()),
                parseEnum(FixtureConfig.CarIndicator.class, tag.getString("CarIndicator"), defaults.carIndicator()),
                tag.getBoolean("LeftPanelIndicator"),
                parseEnum(FixtureConfig.HallIndicator.class, tag.getString("HallIndicator"), defaults.hallIndicator()),
                parseEnum(FixtureConfig.HallLanternPlacement.class, tag.getString("HallLantern"), defaults.hallLantern()));
    }

    public static CompoundTag writeAnnouncements(AnnouncementConfig announcements) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Arrival", announcements.arrival());
        tag.putBoolean("DoorClose", announcements.doorClose());
        tag.putBoolean("Chime", announcements.chime());
        return tag;
    }

    /** 書かれていない項目は初期値にする。 */
    public static AnnouncementConfig readAnnouncements(CompoundTag tag) {
        AnnouncementConfig defaults = AnnouncementConfig.DEFAULT;
        return new AnnouncementConfig(
                tag.contains("Arrival") ? tag.getBoolean("Arrival") : defaults.arrival(),
                tag.contains("DoorClose") ? tag.getBoolean("DoorClose") : defaults.doorClose(),
                tag.contains("Chime") ? tag.getBoolean("Chime") : defaults.chime());
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

package net.lausiv1024.realelevator.entity;

import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
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
        tag.putBoolean("WheelchairPanel", fixtures.wheelchairPanel());
        tag.putBoolean("OpenExtension", fixtures.openExtension());
        tag.putString("IndicatorColor", fixtures.indicatorColor().name());
        tag.putBoolean("GroupControl", fixtures.groupControl());
        return tag;
    }

    public static FixtureConfig readFixtures(CompoundTag tag) {
        FixtureConfig defaults = FixtureConfig.DEFAULT;
        return new FixtureConfig(
                parseEnum(FixtureConfig.PanelSides.class, tag.getString("PanelSides"), defaults.panelSides()),
                parseEnum(FixtureConfig.CarIndicator.class, tag.getString("CarIndicator"), defaults.carIndicator()),
                tag.getBoolean("LeftPanelIndicator"),
                parseEnum(FixtureConfig.HallIndicator.class, tag.getString("HallIndicator"), defaults.hallIndicator()),
                parseEnum(FixtureConfig.HallLanternPlacement.class, tag.getString("HallLantern"), defaults.hallLantern()),
                tag.getBoolean("WheelchairPanel"),
                parseEnum(FixtureConfig.IndicatorColor.class, tag.getString("IndicatorColor"), defaults.indicatorColor()),
                tag.getBoolean("GroupControl"),
                tag.getBoolean("OpenExtension"));
    }

    public static CompoundTag writeInterior(InteriorConfig interior) {
        CompoundTag tag = new CompoundTag();
        for (InteriorConfig.Part part : InteriorConfig.Part.values()) {
            tag.putString(part.name(), interior.material(part));
        }
        tag.putString("Ceiling", interior.ceiling().name());
        tag.putString("Handrail", interior.handrail().name());
        tag.putString("SecurityWindow", interior.securityWindow().name());
        tag.putString("Carpet", interior.carpet().name());
        InteriorConfig.Landing landing = interior.landing();
        tag.putString("LandingFrame", landing.frame().name());
        tag.putString("LandingPanel", landing.panel().name());
        tag.putBoolean("LanternPlate", landing.lanternPlate());
        return tag;
    }

    /** 書かれていない項目は初期値にする。 */
    public static InteriorConfig readInterior(CompoundTag tag) {
        InteriorConfig defaults = InteriorConfig.DEFAULT;
        InteriorConfig interior = new InteriorConfig(defaults.wall(), defaults.front(), defaults.transom(), defaults.door(), defaults.floor(),
                defaults.baseboard(), parseEnum(InteriorConfig.Carpet.class, tag.getString("Carpet"), defaults.carpet()),
                parseEnum(InteriorConfig.CeilingType.class, tag.getString("Ceiling"), defaults.ceiling()),
                parseEnum(InteriorConfig.HandrailPlacement.class, tag.getString("Handrail"), defaults.handrail()),
                parseEnum(InteriorConfig.SecurityWindow.class, tag.getString("SecurityWindow"), defaults.securityWindow()),
                new InteriorConfig.Landing(
                        parseEnum(InteriorConfig.FrameStyle.class, tag.getString("LandingFrame"), defaults.landing().frame()),
                        parseEnum(InteriorConfig.PanelStyle.class, tag.getString("LandingPanel"), defaults.landing().panel()),
                        defaults.landing().frameMaterial(), defaults.landing().wallMaterial(),
                        tag.contains("LanternPlate") ? tag.getBoolean("LanternPlate") : defaults.landing().lanternPlate()));
        for (InteriorConfig.Part part : InteriorConfig.Part.values()) {
            if (tag.contains(part.name())) {
                interior = interior.withMaterial(part, tag.getString(part.name()));
            }
        }
        return interior;
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

package net.lausiv1024.realelevator.entity;

import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.announce.ChimeType;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/**
 * 配置とアナウンスの設定を NBT に読み書きする。かごと制御盤で共通に使う。
 */
public final class ConfigTags {
    private ConfigTags() {
    }

    public static CompoundTag writeFixtures(FixtureConfig fixtures) {
        CompoundTag tag = new CompoundTag();
        tag.putString("PanelSides", fixtures.panelSides().name());
        tag.putBoolean("LeftPanelIndicator", fixtures.leftPanelIndicator());
        tag.putString("HallIndicator", fixtures.hallIndicator().name());
        tag.putString("HallLantern", fixtures.hallLantern().name());
        tag.putBoolean("WheelchairPanel", fixtures.wheelchairPanel());
        tag.putBoolean("OpenExtension", fixtures.openExtension());
        tag.putString("IndicatorStyle", fixtures.indicatorStyle().name());
        tag.putString("IndicatorColor", fixtures.indicatorColor().name());
        tag.putBoolean("GroupControl", fixtures.groupControl());
        return tag;
    }

    public static FixtureConfig readFixtures(CompoundTag tag) {
        FixtureConfig defaults = FixtureConfig.DEFAULT;
        return new FixtureConfig(
                parseEnum(FixtureConfig.PanelSides.class, tag.getString("PanelSides"), defaults.panelSides()),
                tag.getBoolean("LeftPanelIndicator"),
                parseEnum(FixtureConfig.HallIndicator.class, tag.getString("HallIndicator"), defaults.hallIndicator()),
                parseEnum(FixtureConfig.HallLanternPlacement.class, tag.getString("HallLantern"), defaults.hallLantern()),
                tag.getBoolean("WheelchairPanel"),
                parseEnum(FixtureConfig.IndicatorColor.class, tag.getString("IndicatorColor"), defaults.indicatorColor()),
                tag.getBoolean("GroupControl"),
                tag.getBoolean("OpenExtension"),
                parseEnum(FixtureConfig.IndicatorStyle.class, tag.getString("IndicatorStyle"), FixtureConfig.IndicatorStyle.DOT));
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
        tag.putString("LandingSecurityWindow", landing.securityWindow().name());
        if (!interior.landingVariants().isEmpty()) {
            ListTag variants = new ListTag();
            for (InteriorConfig.LandingVariant variant : interior.landingVariants()) {
                CompoundTag v = writeLanding(variant.landing());
                v.putIntArray("Floors", variant.floors().stream().mapToInt(Integer::intValue).toArray());
                variants.add(v);
            }
            tag.put("LandingVariants", variants);
        }
        return tag;
    }

    /** 乗場の意匠（階ごとの意匠用）。 */
    public static CompoundTag writeLanding(InteriorConfig.Landing landing) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Frame", landing.frame().name());
        tag.putString("Panel", landing.panel().name());
        tag.putString("FrameMaterial", landing.frameMaterial());
        tag.putString("WallMaterial", landing.wallMaterial());
        tag.putBoolean("LanternPlate", landing.lanternPlate());
        tag.putString("DoorMaterial", landing.doorMaterial());
        tag.putString("SecurityWindow", landing.securityWindow().name());
        return tag;
    }

    /** 書かれていない項目は fallback の値にする。 */
    public static InteriorConfig.Landing readLanding(CompoundTag tag, InteriorConfig.Landing fallback) {
        return new InteriorConfig.Landing(
                parseEnum(InteriorConfig.FrameStyle.class, tag.getString("Frame"), fallback.frame()),
                parseEnum(InteriorConfig.PanelStyle.class, tag.getString("Panel"), fallback.panel()),
                tag.contains("FrameMaterial") ? tag.getString("FrameMaterial") : fallback.frameMaterial(),
                tag.contains("WallMaterial") ? tag.getString("WallMaterial") : fallback.wallMaterial(),
                tag.contains("LanternPlate") ? tag.getBoolean("LanternPlate") : fallback.lanternPlate(),
                tag.contains("DoorMaterial") ? tag.getString("DoorMaterial") : fallback.doorMaterial(),
                parseEnum(InteriorConfig.LandingWindow.class, tag.getString("SecurityWindow"), fallback.securityWindow()));
    }

    /** 書かれていない項目は初期値にする。 */
    public static InteriorConfig readInterior(CompoundTag tag) {
        InteriorConfig defaults = InteriorConfig.DEFAULT;
        InteriorConfig.SecurityWindow carWindow = parseEnum(InteriorConfig.SecurityWindow.class, tag.getString("SecurityWindow"),
                defaults.securityWindow());
        List<InteriorConfig.LandingVariant> variants = new ArrayList<>();
        for (Tag t : tag.getList("LandingVariants", Tag.TAG_COMPOUND)) {
            CompoundTag v = (CompoundTag) t;
            List<Integer> floors = new ArrayList<>();
            for (int floor : v.getIntArray("Floors")) {
                floors.add(floor);
            }
            if (variants.size() < InteriorConfig.MAX_LANDING_VARIANTS) {
                variants.add(new InteriorConfig.LandingVariant(floors, readLanding(v, InteriorConfig.Landing.DEFAULT)));
            }
        }
        InteriorConfig interior = new InteriorConfig(defaults.wall(), defaults.front(), defaults.transom(), defaults.door(), defaults.floor(),
                defaults.baseboard(), parseEnum(InteriorConfig.Carpet.class, tag.getString("Carpet"), defaults.carpet()),
                parseEnum(InteriorConfig.CeilingType.class, tag.getString("Ceiling"), defaults.ceiling()),
                parseEnum(InteriorConfig.HandrailPlacement.class, tag.getString("Handrail"), defaults.handrail()),
                carWindow,
                new InteriorConfig.Landing(
                        parseEnum(InteriorConfig.FrameStyle.class, tag.getString("LandingFrame"), defaults.landing().frame()),
                        parseEnum(InteriorConfig.PanelStyle.class, tag.getString("LandingPanel"), defaults.landing().panel()),
                        defaults.landing().frameMaterial(), defaults.landing().wallMaterial(),
                        tag.contains("LanternPlate") ? tag.getBoolean("LanternPlate") : defaults.landing().lanternPlate(),
                        defaults.landing().doorMaterial(),
                        // 乗場の防犯窓がない（かごと乗場で分ける前の）設定は、かごに合わせる
                        parseEnum(InteriorConfig.LandingWindow.class, tag.getString("LandingSecurityWindow"),
                                InteriorConfig.LandingWindow.SAME_AS_CAR)),
                variants);
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
        tag.putString("ChimeType", announcements.chime().name());
        return tag;
    }

    /** 書かれていない項目は初期値にする。 */
    public static AnnouncementConfig readAnnouncements(CompoundTag tag) {
        AnnouncementConfig defaults = AnnouncementConfig.DEFAULT;
        return new AnnouncementConfig(
                tag.contains("Arrival") ? tag.getBoolean("Arrival") : defaults.arrival(),
                tag.contains("DoorClose") ? tag.getBoolean("DoorClose") : defaults.doorClose(),
                readChime(tag, defaults.chime()));
    }

    /** チャイムの種類。種類を選べるようになる前の保存（オン／オフ）は、オンならチャイム1にする。 */
    private static ChimeType readChime(CompoundTag tag, ChimeType fallback) {
        if (tag.contains("ChimeType")) {
            return parseEnum(ChimeType.class, tag.getString("ChimeType"), fallback);
        }
        if (tag.contains("Chime")) {
            return tag.getBoolean("Chime") ? ChimeType.CHIME_1 : ChimeType.NONE;
        }
        return fallback;
    }

    public static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

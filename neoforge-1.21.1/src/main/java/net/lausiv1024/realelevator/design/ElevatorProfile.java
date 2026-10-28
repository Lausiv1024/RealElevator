package net.lausiv1024.realelevator.design;

import java.util.List;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/**
 * エレベーターのプロファイル。設置用ブロックの設定（{@code ElevatorPlan#settingsTag} の形）から、建物ごとに違う停止階・階名・
 * 読み上げ名・停止階ごとの出入口を除いた「仕様と意匠」だけを取り出したもの。仕様書のアイテムと、手元のファイルに保存する。
 *
 * <ul>
 * <li>設置する前だけ変えられる項目：機種、扉、速度（加速度・ジャーク）、ピット、釣合おもりの位置、二方向出入口、展望仕様。</li>
 * <li>設置したあとも変えられる項目：器具の配置、アナウンス、乗場開放、意匠（かごと乗場）。</li>
 * <li>名前（{@value #MAX_NAME_LENGTH}文字まで）。</li>
 * </ul>
 *
 * 設置用ブロックにはすべての項目を、設置したかごには設置したあとも変えられる項目だけを反映する。
 */
public final class ElevatorProfile {
    public static final int MAX_NAME_LENGTH = 32;
    private static final String NAME = "Name";
    /** 設置する前だけ変えられる項目。 */
    private static final List<String> INSTALL_KEYS = List.of("Model", "Door", "Speed", "Acceleration", "Jerk", "Pit", "Counterweight",
            "Through", "Observation");
    /** 設置したあとも変えられる項目。 */
    private static final List<String> RENEWAL_KEYS = List.of("Fixtures", "Announcements", "LandingOpen", "Interior");

    private ElevatorProfile() {
    }

    /** 設定 settings からプロファイルの項目を取り出す（名前は付けない）。 */
    public static CompoundTag extract(CompoundTag settings) {
        CompoundTag profile = new CompoundTag();
        copy(settings, profile, INSTALL_KEYS);
        copy(settings, profile, RENEWAL_KEYS);
        return profile;
    }

    /** 送られてきたものなどから、プロファイルの項目と名前だけを残したもの。 */
    public static CompoundTag sanitize(CompoundTag tag) {
        return withName(extract(tag), name(tag));
    }

    /** 設置する前の設定 settings に、プロファイルのすべての項目を上書きしたもの（停止階・階名などはそのまま）。 */
    public static CompoundTag mergeInstall(CompoundTag settings, CompoundTag profile) {
        CompoundTag merged = settings.copy();
        copy(profile, merged, INSTALL_KEYS);
        copy(profile, merged, RENEWAL_KEYS);
        return merged;
    }

    /** 設置したかごの設定 settings に、プロファイルの設置したあとも変えられる項目だけを上書きしたもの。 */
    public static CompoundTag mergeRenewal(CompoundTag settings, CompoundTag profile) {
        CompoundTag merged = settings.copy();
        copy(profile, merged, RENEWAL_KEYS);
        return merged;
    }

    /** 中身があるか（白紙の仕様書でないか）。 */
    public static boolean isPresent(CompoundTag profile) {
        return profile.contains("Model", Tag.TAG_STRING);
    }

    public static String name(CompoundTag profile) {
        return trimName(profile.getString(NAME));
    }

    /** 名前を付けたもの。空なら名前をなくす。 */
    public static CompoundTag withName(CompoundTag profile, String name) {
        CompoundTag named = profile.copy();
        String trimmed = trimName(name);
        if (trimmed.isEmpty()) {
            named.remove(NAME);
        } else {
            named.putString(NAME, trimmed);
        }
        return named;
    }

    private static String trimName(String name) {
        String trimmed = name.strip();
        return trimmed.length() > MAX_NAME_LENGTH ? trimmed.substring(0, MAX_NAME_LENGTH) : trimmed;
    }

    private static void copy(CompoundTag from, CompoundTag to, List<String> keys) {
        for (String key : keys) {
            Tag value = from.get(key);
            if (value != null) {
                to.put(key, value.copy());
            }
        }
    }
}

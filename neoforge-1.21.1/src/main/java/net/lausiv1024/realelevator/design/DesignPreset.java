package net.lausiv1024.realelevator.design;

import java.util.Locale;

import javax.annotation.Nullable;

import com.google.gson.JsonObject;

import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;

/**
 * 意匠のプリセット。かごと乗場の素材と設定をまとめて入れる。データパックの {@code data/<名前空間>/elevator_presets/<名前>.json} で定義する。
 *
 * <pre>{@code
 * {
 *   "name": "preset.realelevator.standard",   // 表示名（翻訳キーか、そのままの文字）
 *   "order": 0,                               // 並び順（小さいほど先）
 *   "interior": {"wall": "...", "front": "...", "transom": "#front", "door": "...", "floor": "...", "baseboard": "...",
 *                "carpet": "none", "ceiling": "downlight", "handrail": "sides", "security_window": "none"},
 *   "landing": {"frame": "thin", "panel": "wall_mounted", "frame_material": "...", "wall_material": "", "lantern_plate": true},
 *   "car_indicator": "over_door",             // 省略可。かごの表示器の位置
 *   "observation": true                       // 省略可。展望仕様（設置する前だけ効く）
 * }
 * }</pre>
 * 書かれていない項目は、標準の内装（{@link InteriorConfig#DEFAULT}）の値にする。
 *
 * @param carIndicator かごの表示器の位置。null なら変えない
 */
public record DesignPreset(ResourceLocation id, String name, int order, InteriorConfig interior, @Nullable FixtureConfig.CarIndicator carIndicator,
                           boolean observation) {
    public static DesignPreset parse(ResourceLocation id, JsonObject json) {
        InteriorConfig d = InteriorConfig.DEFAULT;
        JsonObject in = GsonHelper.getAsJsonObject(json, "interior", new JsonObject());
        JsonObject landing = GsonHelper.getAsJsonObject(json, "landing", new JsonObject());
        InteriorConfig.Landing l = d.landing();
        InteriorConfig interior = new InteriorConfig(
                GsonHelper.getAsString(in, "wall", d.wall()),
                GsonHelper.getAsString(in, "front", d.front()),
                GsonHelper.getAsString(in, "transom", d.transom()),
                GsonHelper.getAsString(in, "door", d.door()),
                GsonHelper.getAsString(in, "floor", d.floor()),
                GsonHelper.getAsString(in, "baseboard", d.baseboard()),
                parse(InteriorConfig.Carpet.class, in, "carpet", d.carpet()),
                parse(InteriorConfig.CeilingType.class, in, "ceiling", d.ceiling()),
                parse(InteriorConfig.HandrailPlacement.class, in, "handrail", d.handrail()),
                parse(InteriorConfig.SecurityWindow.class, in, "security_window", d.securityWindow()),
                new InteriorConfig.Landing(
                        parse(InteriorConfig.FrameStyle.class, landing, "frame", l.frame()),
                        parse(InteriorConfig.PanelStyle.class, landing, "panel", l.panel()),
                        GsonHelper.getAsString(landing, "frame_material", l.frameMaterial()),
                        GsonHelper.getAsString(landing, "wall_material", l.wallMaterial()),
                        GsonHelper.getAsBoolean(landing, "lantern_plate", l.lanternPlate())));
        FixtureConfig.CarIndicator carIndicator = json.has("car_indicator")
                ? parse(FixtureConfig.CarIndicator.class, json, "car_indicator", null) : null;
        return new DesignPreset(id, GsonHelper.getAsString(json, "name", id.toString()), GsonHelper.getAsInt(json, "order", 1000),
                interior, carIndicator, GsonHelper.getAsBoolean(json, "observation", false));
    }

    private static <E extends Enum<E>> E parse(Class<E> type, JsonObject json, String key, E fallback) {
        if (!json.has(key)) {
            return fallback;
        }
        return ConfigTags.parseEnum(type, GsonHelper.getAsString(json, key).toUpperCase(Locale.ROOT), fallback);
    }

    /** クライアントへ送る形。 */
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", id.toString());
        tag.putString("Name", name);
        tag.putInt("Order", order);
        tag.put("Interior", ConfigTags.writeInterior(interior));
        if (carIndicator != null) {
            tag.putString("CarIndicator", carIndicator.name());
        }
        tag.putBoolean("Observation", observation);
        return tag;
    }

    public static DesignPreset fromTag(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
        FixtureConfig.CarIndicator carIndicator = tag.contains("CarIndicator")
                ? ConfigTags.parseEnum(FixtureConfig.CarIndicator.class, tag.getString("CarIndicator"), null) : null;
        return new DesignPreset(id != null ? id : ResourceLocation.withDefaultNamespace("unknown"), tag.getString("Name"), tag.getInt("Order"),
                ConfigTags.readInterior(tag.getCompound("Interior")), carIndicator, tag.getBoolean("Observation"));
    }
}

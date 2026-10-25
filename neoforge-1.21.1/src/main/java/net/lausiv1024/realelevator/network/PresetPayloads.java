package net.lausiv1024.realelevator.network;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.design.DesignPreset;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 意匠のプリセットのやりとり。
 */
public final class PresetPayloads {
    private PresetPayloads() {
    }

    /** プリセットの一覧と、自由設定がオンか（サーバー → クライアント）。 */
    public record Sync(CompoundTag data) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, "presets"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.COMPOUND_TAG, Sync::data,
                Sync::new);

        public static Sync of(List<DesignPreset> presets, boolean freeMaterials) {
            CompoundTag data = new CompoundTag();
            ListTag list = new ListTag();
            presets.forEach(preset -> list.add(preset.toTag()));
            data.put("Presets", list);
            data.putBoolean("FreeMaterials", freeMaterials);
            return new Sync(data);
        }

        public List<DesignPreset> presets() {
            List<DesignPreset> presets = new ArrayList<>();
            for (Tag tag : data.getList("Presets", Tag.TAG_COMPOUND)) {
                presets.add(DesignPreset.fromTag((CompoundTag) tag));
            }
            return presets;
        }

        public boolean freeMaterials() {
            return data.getBoolean("FreeMaterials");
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}

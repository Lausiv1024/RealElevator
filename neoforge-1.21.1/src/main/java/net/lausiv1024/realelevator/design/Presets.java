package net.lausiv1024.realelevator.design;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.RealElevatorConfig;
import net.lausiv1024.realelevator.network.PresetPayloads;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * データパックから読み込んだ意匠のプリセット（{@link DesignPreset}）。サーバーで読み込み、ログインしたときとデータパックを
 * 読み込み直したときに、自由設定がオンかどうかといっしょにクライアントへ送る。
 */
@EventBusSubscriber(modid = RealElevator.MODID)
public final class Presets extends SimpleJsonResourceReloadListener {
    public static final String DIRECTORY = "elevator_presets";
    private static volatile List<DesignPreset> presets = List.of();

    private Presets() {
        super(new Gson(), DIRECTORY);
    }

    /** 読み込んだプリセット（並び順）。 */
    public static List<DesignPreset> all() {
        return presets;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<DesignPreset> loaded = new ArrayList<>();
        files.forEach((id, json) -> {
            try {
                loaded.add(DesignPreset.parse(id, GsonHelper.convertToJsonObject(json, "preset")));
            } catch (RuntimeException e) {
                RealElevator.LOGGER.error("Couldn't load elevator preset {}", id, e);
            }
        });
        loaded.sort(Comparator.comparingInt(DesignPreset::order).thenComparing(p -> p.id().toString()));
        presets = List.copyOf(loaded);
    }

    @SubscribeEvent
    static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new Presets());
    }

    @SubscribeEvent
    static void onDatapackSync(OnDatapackSyncEvent event) {
        PresetPayloads.Sync payload = PresetPayloads.Sync.of(presets, RealElevatorConfig.freeMaterials());
        event.getRelevantPlayers().forEach(player -> PacketDistributor.sendToPlayer(player, payload));
    }
}

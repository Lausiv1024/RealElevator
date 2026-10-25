package net.lausiv1024.realelevator.client;

import java.util.List;

import net.lausiv1024.realelevator.design.DesignPreset;
import net.lausiv1024.realelevator.network.PresetPayloads;

/**
 * サーバーから届いた意匠のプリセットと、自由設定がオンかどうか。デザインの画面で使う。
 */
public final class ClientPresets {
    private static List<DesignPreset> presets = List.of();
    private static boolean freeMaterials;

    private ClientPresets() {
    }

    public static void handle(PresetPayloads.Sync payload) {
        presets = List.copyOf(payload.presets());
        freeMaterials = payload.freeMaterials();
    }

    static List<DesignPreset> presets() {
        return presets;
    }

    static boolean freeMaterials() {
        return freeMaterials;
    }
}

package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.core.announce.ChimeType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, RealElevator.MODID);

    /** チャイム1の到着チャイム（上り）。音は tools/sounds/make_chime.py で合成したもの。音の並びは {@link ChimeType}。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_UP = register("chime_up");
    /** チャイム1の到着チャイム（下り）。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_DOWN = register("chime_down");
    /** チャイム1の群管理の即時予報（上りも下りも同じ）。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_FORECAST = register("chime_forecast");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_2_UP = register("chime_2_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_2_DOWN = register("chime_2_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_2_FORECAST = register("chime_2_forecast");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_3_UP = register("chime_3_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_3_DOWN = register("chime_3_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_3_FORECAST = register("chime_3_forecast");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_4_UP = register("chime_4_up");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_4_DOWN = register("chime_4_down");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_4_FORECAST = register("chime_4_forecast");
    /** ナッジング（強制戸閉）のブザー（0.5秒の1回分）。音は tools/sounds/make_buzzer.py で合成したもの。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> NUDGE_BUZZER = register("nudge_buzzer");

    /** 非常呼びボタン（飾り）を押したときの短い音。音は tools/sounds/make_beep.py で合成したもの。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> EMERGENCY_BEEP = register("emergency_beep");

    private ModSounds() {
    }

    /** チャイムの種類 type の到着チャイム（上り。方向が決まっていないときも）。 */
    public static SoundEvent chimeUp(ChimeType type) {
        return (switch (type) {
            case CHIME_2 -> CHIME_2_UP;
            case CHIME_3 -> CHIME_3_UP;
            case CHIME_4 -> CHIME_4_UP;
            default -> CHIME_UP;
        }).get();
    }

    /** チャイムの種類 type の到着チャイム（下り）。 */
    public static SoundEvent chimeDown(ChimeType type) {
        return (switch (type) {
            case CHIME_2 -> CHIME_2_DOWN;
            case CHIME_3 -> CHIME_3_DOWN;
            case CHIME_4 -> CHIME_4_DOWN;
            default -> CHIME_DOWN;
        }).get();
    }

    /** チャイムの種類 type の、群管理の即時予報。 */
    public static SoundEvent chimeForecast(ChimeType type) {
        return (switch (type) {
            case CHIME_2 -> CHIME_2_FORECAST;
            case CHIME_3 -> CHIME_3_FORECAST;
            case CHIME_4 -> CHIME_4_FORECAST;
            default -> CHIME_FORECAST;
        }).get();
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, name)));
    }
}

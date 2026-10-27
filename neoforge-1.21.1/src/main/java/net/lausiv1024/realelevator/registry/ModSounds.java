package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, RealElevator.MODID);

    /** 到着チャイム（上り、1回）。音は tools/sounds/make_chime.py で合成したもの。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_UP = register("chime_up");
    /** 到着チャイム（下り、2回）。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_DOWN = register("chime_down");
    /** 群管理の即時予報（G#5 の単音。上りも下りも同じ）。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIME_FORECAST = register("chime_forecast");
    /** ナッジング（強制戸閉）のブザー（0.5秒の1回分）。音は tools/sounds/make_buzzer.py で合成したもの。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> NUDGE_BUZZER = register("nudge_buzzer");

    /** 非常呼びボタン（飾り）を押したときの短い音。音は tools/sounds/make_beep.py で合成したもの。 */
    public static final DeferredHolder<SoundEvent, SoundEvent> EMERGENCY_BEEP = register("emergency_beep");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name,
                () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(RealElevator.MODID, name)));
    }
}

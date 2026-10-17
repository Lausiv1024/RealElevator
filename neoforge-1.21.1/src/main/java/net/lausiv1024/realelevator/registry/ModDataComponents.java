package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents DATA_COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, RealElevator.MODID);

    /** 階床登録ツールがつながっている制御盤の場所。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> LINKED_INSTALLER =
            DATA_COMPONENTS.registerComponentType("linked_installer",
                    builder -> builder.persistent(GlobalPos.CODEC).networkSynchronized(GlobalPos.STREAM_CODEC));

    private ModDataComponents() {
    }
}

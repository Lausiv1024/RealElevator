package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import java.util.UUID;

import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
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

    /** 保守用レンチで、運転切替盤をつなぐために選んだエレベーター（かごの UUID）。運転切替盤をつなぐと消える。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<UUID>> SELECTED_CAR =
            DATA_COMPONENTS.registerComponentType("selected_car",
                    builder -> builder.persistent(UUIDUtil.CODEC).networkSynchronized(UUIDUtil.STREAM_CODEC));

    private ModDataComponents() {
    }
}

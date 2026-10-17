package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControlPanelBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RealElevator.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControlPanelBlockEntity>> CONTROL_PANEL =
            BLOCK_ENTITY_TYPES.register("control_panel",
                    () -> BlockEntityType.Builder.of(ControlPanelBlockEntity::new, ModBlocks.CONTROL_PANEL.get()).build(null));

    private ModBlockEntities() {
    }
}

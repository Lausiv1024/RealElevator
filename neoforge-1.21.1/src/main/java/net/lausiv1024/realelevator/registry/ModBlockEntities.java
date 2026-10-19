package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControllerBlockEntity;
import net.lausiv1024.realelevator.block.InstallerBlockEntity;
import net.lausiv1024.realelevator.block.PowerReceiverBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RealElevator.MODID);

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InstallerBlockEntity>> INSTALLER =
            BLOCK_ENTITY_TYPES.register("installer",
                    () -> BlockEntityType.Builder.of(InstallerBlockEntity::new, ModBlocks.INSTALLER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ControllerBlockEntity>> CONTROLLER =
            BLOCK_ENTITY_TYPES.register("controller",
                    () -> BlockEntityType.Builder.of(ControllerBlockEntity::new, ModBlocks.CONTROLLER.get()).build(null));

    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerReceiverBlockEntity>> POWER_RECEIVER =
            BLOCK_ENTITY_TYPES.register("power_receiver",
                    () -> BlockEntityType.Builder.of(PowerReceiverBlockEntity::new, ModBlocks.POWER_RECEIVER.get()).build(null));

    private ModBlockEntities() {
    }
}

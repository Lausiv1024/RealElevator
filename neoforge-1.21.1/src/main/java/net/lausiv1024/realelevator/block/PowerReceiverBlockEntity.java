package net.lausiv1024.realelevator.block;

import java.util.UUID;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 運転切替盤がつないでいるエレベーター（かごの UUID）を覚える。 */
public class PowerReceiverBlockEntity extends BlockEntity {
    @Nullable
    private UUID car;

    public PowerReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_RECEIVER.get(), pos, state);
    }

    /** つないでいるかご。つないでいなければ null。 */
    @Nullable
    public UUID car() {
        return car;
    }

    public void setCar(@Nullable UUID car) {
        this.car = car;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (car != null) {
            tag.putUUID("Car", car);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        car = tag.hasUUID("Car") ? tag.getUUID("Car") : null;
    }
}

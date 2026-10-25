package net.lausiv1024.realelevator.block;

import java.util.List;
import java.util.UUID;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.registry.ModBlockEntities;
import net.lausiv1024.realelevator.registry.ModBlocks;
import net.lausiv1024.realelevator.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 頂部の制御盤。設置したエレベーターの計画（{@link ElevatorPlan}）とネザースターを持ち、かごと紐付く。
 *
 * <p>リニューアル（配置・アナウンス・階名の変更）と解体は、乗場の操作盤を保守用レンチで右クリックして開くメニューから行う。
 */
public class ControllerBlockEntity extends BlockEntity {
    private ElevatorPlan plan;
    private ItemStack star = ItemStack.EMPTY;
    @Nullable
    private UUID carUuid;
    /** 解体の途中か。解体で制御盤のブロックを消したときに、もう一度解体しないようにする。 */
    private boolean dismantling;

    public ControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROLLER.get(), pos, state);
        this.plan = new ElevatorPlan(pos, state.getValue(HorizontalDirectionalBlock.FACING));
    }

    /** 設置したときに、設置用ブロックから計画とネザースターを受け取る。 */
    public void setup(ElevatorPlan plan, ItemStack star, UUID carUuid) {
        // 設置用ブロックとは別のものにしておく
        this.plan = ElevatorPlan.load(plan.save(), plan.origin(), plan.facing());
        this.star = star.copy();
        this.carUuid = carUuid;
        setChanged();
    }

    public ElevatorPlan plan() {
        return plan;
    }

    @Nullable
    public UUID carUuid() {
        return carUuid;
    }

    public ItemStack star() {
        return star;
    }

    @Nullable
    private ElevatorCarEntity car() {
        if (carUuid == null || level == null) {
            return null;
        }
        return ElevatorTracker.findCar(level, carUuid);
    }

    /** リニューアルの画面へ渡す設定（停止階を含む）。 */
    public CompoundTag renewalTag() {
        return plan.settingsTag();
    }

    /** リニューアル（配置・アナウンス・階名の変更）を反映し、かごにも反映する。 */
    public void applyRenewal(CompoundTag tag) {
        plan.applyRenewal(tag);
        ElevatorCarEntity car = car();
        if (car != null) {
            car.setFixtures(plan.fixtures());
            car.setAnnouncements(plan.announcements());
            car.setLandingOpen(plan.landingOpen());
            car.setInterior(plan.interior());
            car.setLabels(plan.labelArray());
        }
        setChanged();
    }

    /**
     * エレベーターを解体する。かご・乗場の扉・ガイドレール・頂部の制御盤を撤去し、設置用ブロックとネザースターを返す。
     *
     * @param player 返す相手。null なら、設置用ブロックを置いていた場所（最下階の乗場）に落とす
     */
    public void dismantle(@Nullable Player player) {
        if (dismantling || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        dismantling = true;
        if (carUuid != null) {
            for (LandingDoorEntity door : ElevatorTracker.landingDoors(serverLevel)) {
                if (door.carUuid().filter(carUuid::equals).isPresent()) {
                    door.discard();
                }
            }
            Entity car = serverLevel.getEntity(carUuid);
            if (car != null) {
                car.discard();
            }
        }
        ShaftLayout layout = plan.layout();
        for (ShaftLayout.Rail rail : layout.rails()) {
            BlockPos pos = plan.cellPos(rail.cell());
            if (serverLevel.getBlockState(pos).is(ModBlocks.GUIDE_RAIL.get())) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }

        List<ItemStack> returned = star.isEmpty()
                ? List.of(new ItemStack(ModItems.INSTALLER.get()))
                : List.of(new ItemStack(ModItems.INSTALLER.get()), star.copy());
        star = ItemStack.EMPTY;
        for (ItemStack stack : returned) {
            if (player == null || !player.getInventory().add(stack)) {
                BlockPos drop = player != null ? player.blockPosition() : plan.origin();
                Containers.dropItemStack(serverLevel, drop.getX() + 0.5, drop.getY() + 0.5, drop.getZ() + 0.5, stack);
            }
        }

        // 制御盤の2つのブロックを消す
        BlockPos lower = worldPosition;
        for (BlockPos pos : new BlockPos[] {lower.above(), lower}) {
            if (serverLevel.getBlockState(pos).is(ModBlocks.CONTROLLER.get())) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
    }

    // ---- 保存 ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Plan", plan.save());
        if (!star.isEmpty()) {
            tag.put("Star", star.save(registries));
        }
        if (carUuid != null) {
            tag.putUUID("Car", carUuid);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        plan = ElevatorPlan.load(tag.getCompound("Plan"), worldPosition, facing);
        star = ItemStack.parseOptional(registries, tag.getCompound("Star"));
        carUuid = tag.hasUUID("Car") ? tag.getUUID("Car") : null;
    }
}

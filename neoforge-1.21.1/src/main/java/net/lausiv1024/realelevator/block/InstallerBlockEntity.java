package net.lausiv1024.realelevator.block;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.install.ShaftLayout.Cell;
import net.lausiv1024.realelevator.core.install.ShaftLayout.FloorProblem;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.design.ElevatorProfile;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.menu.InstallerMenu;
import net.lausiv1024.realelevator.registry.ModBlockEntities;
import net.lausiv1024.realelevator.registry.ModBlocks;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

/**
 * 設置用ブロック。停止階と設定を決めて、昇降路を設置する。
 *
 * <p>停止階は階床登録ツールで登録し、設置用ブロックの高さ（最下階）は常に含む。設置すると、設置用ブロックは消え、
 * 計画（{@link ElevatorPlan}）とネザースターは昇降路の頂部の制御盤（{@link ControllerBlockEntity}）に移る。
 *
 * <p>高速仕様（高速・超高速）は、ネザースターを1個はめると選べるようになる。ネザースターは消費しない。
 */
public class InstallerBlockEntity extends BlockEntity implements MenuProvider {
    /** 設定画面から操作できる距離。 */
    public static final double MAX_USE_DISTANCE = 8.0;

    private ElevatorPlan plan;
    private boolean preview;
    /** ネザースターをはめるスロット。 */
    private final SimpleContainer star = new SimpleContainer(1) {
        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public boolean canPlaceItem(int slot, ItemStack stack) {
            return stack.is(Items.NETHER_STAR);
        }
    };

    public InstallerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INSTALLER.get(), pos, state);
        this.plan = new ElevatorPlan(pos, state.getValue(HorizontalDirectionalBlock.FACING));
        star.addListener(container -> onStarChanged());
    }

    // ---- 設定 ----

    public ElevatorPlan plan() {
        return plan;
    }

    public Direction facing() {
        return plan.facing();
    }

    public boolean isPreviewing() {
        return preview;
    }

    public int[] floorYs() {
        return plan.floorYs();
    }

    public String label(int floorY) {
        return plan.label(floorY);
    }

    public CarModel model() {
        return plan.model();
    }

    public SpeedSetting speed() {
        return plan.speed();
    }

    public PitMode pitMode() {
        return plan.pitMode();
    }

    public FixtureConfig fixtures() {
        return plan.fixtures();
    }

    public AnnouncementConfig announcements() {
        return plan.announcements();
    }

    public ShaftLayout layout() {
        return plan.layout();
    }

    public boolean allowsOverDoorHallFixtures() {
        return plan.allowsOverDoorHallFixtures();
    }

    public FloorProblem floorProblem() {
        return ShaftLayout.check(layout().floors());
    }

    /** ネザースターのスロット。 */
    public Container starContainer() {
        return star;
    }

    /** ネザースターがはまっているか（高速仕様を選べるか）。 */
    public boolean hasNetherStar() {
        return star.getItem(0).is(Items.NETHER_STAR);
    }

    /** ネザースターを取り出したら、高速仕様の設定は普通（高）に戻す。 */
    private void onStarChanged() {
        if (!hasNetherStar() && plan.speed().spec().isHighSpeed()) {
            plan.setSpeed(SpeedSetting.standard(SpeedSpec.NORMAL_HIGH));
        }
        sync();
    }

    /** 設定画面で変えた設定を反映する。 */
    public void applySettings(CompoundTag tag) {
        plan.applyInstallSettings(tag, hasNetherStar());
        plan.applyRenewal(tag);
        preview = tag.getBoolean("Preview");
        sync();
    }

    /** 設定画面へ渡す設定。{@link #applySettings} と同じ形。 */
    public CompoundTag settingsTag() {
        CompoundTag tag = plan.settingsTag();
        tag.putBoolean("Preview", preview);
        return tag;
    }

    /**
     * 仕様書のプロファイル profile を反映する（停止階・階名・読み上げ名・出入口はそのまま）。高速仕様は、ネザースターがはまっていなければ
     * 速度を変えない。結果を伝える文を返す。
     */
    public Component applyProfile(CompoundTag profile) {
        SpeedSpec requested = ConfigTags.parseEnum(SpeedSpec.class, profile.getString("Speed"), plan.speed().spec());
        applySettings(ElevatorProfile.mergeInstall(settingsTag(), profile));
        return Component.translatable(requested.isHighSpeed() && !hasNetherStar()
                ? "message.realelevator.profile.applied_no_star" : "message.realelevator.profile.applied");
    }

    /** 階床登録ツールで y の階を登録または解除する。結果を伝える文を返す。 */
    public Component toggleFloor(int y) {
        ElevatorPlan.FloorChange change = plan.toggleFloor(y);
        if (change.changed()) {
            sync();
        }
        return change.message();
    }

    // ---- ワールドの座標 ----

    /** 設置用ブロックからの相対位置 cell のワールドの位置。 */
    public BlockPos cellPos(Cell cell) {
        return plan.cellPos(cell);
    }

    /** かごの原点（かご内の床面の中心）のワールドの位置。y は最下階の床面。 */
    public Vec3 carOrigin() {
        return plan.carOrigin();
    }

    /** 設置の邪魔になるブロック（置き換えられないブロック）の位置。 */
    public List<BlockPos> obstructions(Level level) {
        List<BlockPos> result = new ArrayList<>();
        for (Cell cell : layout().requiredClearCells()) {
            BlockPos pos = cellPos(cell);
            if (!pos.equals(worldPosition) && !level.getBlockState(pos).canBeReplaced()) {
                result.add(pos);
            }
        }
        return result;
    }

    // ---- 設置 ----

    /**
     * 昇降路を設置する。ガイドレール・かご・各階の乗場の扉・頂部の制御盤を置き、設置用ブロックは消す。結果を伝える文を返す。
     */
    public Component install() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Component.empty();
        }
        FloorProblem problem = floorProblem();
        if (problem != FloorProblem.NONE) {
            return Component.translatable("message.realelevator.install.floors." + problem.name().toLowerCase(Locale.ROOT));
        }
        List<BlockPos> obstructions = obstructions(serverLevel);
        if (!obstructions.isEmpty()) {
            return Component.translatable("message.realelevator.install.obstructed", obstructions.size());
        }

        ShaftLayout layout = layout();
        // 置き換えられるもの（草や水など）は取り除く
        for (Cell cell : layout.requiredClearCells()) {
            BlockPos pos = cellPos(cell);
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        Direction toCar = facing().getCounterClockWise();
        for (ShaftLayout.Rail rail : layout.rails()) {
            Direction railFacing = rail.left() ? toCar : toCar.getOpposite();
            // 壁への取り付け金具は、ピットの底から数ブロックおきに付ける
            boolean bracket = Math.floorMod(rail.cell().y() - layout.bottom(), GuideRailBlock.BRACKET_SPACING) == 1;
            level.setBlock(cellPos(rail.cell()), ModBlocks.GUIDE_RAIL.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, railFacing).setValue(GuideRailBlock.BRACKET, bracket), Block.UPDATE_ALL);
        }

        List<Cell> controllerCells = layout.controllerCells();
        BlockPos controllerPos = cellPos(controllerCells.get(0));
        Vec3 origin = carOrigin();
        ElevatorCarEntity car = ModEntities.ELEVATOR_CAR.get().create(serverLevel);
        if (car == null) {
            return Component.empty();
        }
        car.moveTo(origin.x, origin.y, origin.z, 0, 0);
        car.setup(plan.spec(), facing(), floorYs(), plan.speed());
        car.setHoistway(plan.pitMode(), plan.counterweight());
        car.setFixtures(plan.fixtures());
        car.setAnnouncements(plan.announcements());
        car.setLandingOpen(plan.landingOpen());
        car.setInterior(plan.interior());
        car.setLabels(plan.labelArray());
        car.setSpeechNames(plan.speechNameArray());
        car.setController(controllerPos);
        if (plan.isThrough()) {
            car.setThrough(plan.entranceArray());
        }
        car.setObservation(plan.isObservation());
        serverLevel.addFreshEntity(car);
        // 階ごとに、使う出入口の乗場の扉を出す（二方向出入口なら奥も）
        for (int floorY : floorYs()) {
            for (Entrance entrance : plan.entrances(floorY).entrances()) {
                LandingDoorEntity door = ModEntities.LANDING_DOOR.get().create(serverLevel);
                if (door != null) {
                    door.moveTo(origin.x, floorY, origin.z, 0, 0);
                    door.setup(car, entrance);
                    serverLevel.addFreshEntity(door);
                }
            }
        }

        // 頂部の制御盤を置き、計画とネザースターを移す
        // 制御盤の背面を、付ける壁に向ける（奥の壁なら乗場の向き、左の壁なら乗場から見て右、乗場の壁の裏なら昇降路の奥の向き）
        Direction controllerFacing = switch (layout.controllerMount()) {
            case BACK_WALL -> facing();
            case LEFT_WALL -> facing().getCounterClockWise();
            case FRONT_WALL -> facing().getOpposite();
        };
        BlockState controller = ModBlocks.CONTROLLER.get().defaultBlockState()
                .setValue(HorizontalDirectionalBlock.FACING, controllerFacing)
                .setValue(ControllerBlock.CORNER, layout.controllerMount() == ShaftLayout.ControllerMount.LEFT_WALL);
        level.setBlock(controllerPos, controller.setValue(ControllerBlock.HALF, DoubleBlockHalf.LOWER), Block.UPDATE_ALL);
        level.setBlock(cellPos(controllerCells.get(1)), controller.setValue(ControllerBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        if (level.getBlockEntity(controllerPos) instanceof ControllerBlockEntity controllerEntity) {
            controllerEntity.setup(plan, star.removeItemNoUpdate(0), car.getUUID());
        }

        int floorCount = floorYs().length;
        // 設置用ブロックは消す（中身はもう制御盤に移したので、何も落とさない）
        level.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        return Component.translatable("message.realelevator.install.done", floorCount);
    }

    // ---- 保存と同期 ----

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Plan", plan.save());
        tag.putBoolean("Preview", preview);
        if (!star.getItem(0).isEmpty()) {
            tag.put("Star", star.getItem(0).save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // 計画の場所と向きは、いつもこのブロックのもの
        Direction facing = getBlockState().getValue(HorizontalDirectionalBlock.FACING);
        plan = ElevatorPlan.load(withoutPlace(tag.getCompound("Plan")), worldPosition, facing);
        preview = tag.getBoolean("Preview");
        // 読み込みでは変更を知らせない（同期や設定の戻しを起こさない）よう、中身を直接入れ替える
        star.getItems().set(0, ItemStack.parseOptional(registries, tag.getCompound("Star")));
    }

    private static CompoundTag withoutPlace(CompoundTag tag) {
        CompoundTag copy = tag.copy();
        copy.remove("Origin");
        copy.remove("Facing");
        return copy;
    }

    // ---- 設定画面 ----

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.realelevator.panel.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new InstallerMenu(containerId, inventory, this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}

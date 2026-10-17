package net.lausiv1024.realelevator.block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.install.ShaftLayout.Cell;
import net.lausiv1024.realelevator.core.install.ShaftLayout.FloorProblem;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.menu.ControlPanelMenu;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 制御盤の設定と、昇降路の設置・撤去。
 *
 * <p>停止階は階床登録ツールで登録し、制御盤の高さ（最下階）は常に含む。機種・速度・ピットは設置する前だけ変えられ、
 * 階名・配置・アナウンスの設定は設置したあとも変えられる（リニューアル）。
 *
 * <p>高速仕様（高速・超高速）は、ネザースターを1個はめると選べるようになる。ネザースターは消費しない。
 * 高速仕様で設置しているあいだは、ネザースターを取り出せない。
 */
public class ControlPanelBlockEntity extends BlockEntity implements MenuProvider {
    /** 階名の文字数の上限。階床表示器には先頭の2文字が出る。 */
    public static final int MAX_LABEL_LENGTH = 3;
    /** 設定画面から操作できる距離。 */
    public static final double MAX_USE_DISTANCE = 8.0;

    /** 停止階の床面のY座標。 */
    private final TreeSet<Integer> floors = new TreeSet<>();
    /** 停止階のY座標ごとの階名。空なら初期値（下から 1、2、3…）。 */
    private final Map<Integer, String> labels = new HashMap<>();
    private CarModel model = CarModel.P11;
    private SpeedSetting speed = SpeedSetting.standard(SpeedSpec.NORMAL_LOW);
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
    private PitMode pitMode = PitMode.REAL;
    private FixtureConfig fixtures = FixtureConfig.DEFAULT;
    private AnnouncementConfig announcements = AnnouncementConfig.DEFAULT;
    private boolean preview;
    /** 設置したかご。設置していなければ null。 */
    @Nullable
    private UUID carUuid;

    public ControlPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CONTROL_PANEL.get(), pos, state);
        floors.add(pos.getY());
        star.addListener(container -> onStarChanged());
    }

    // ---- 設定 ----

    public Direction facing() {
        return getBlockState().getValue(HorizontalDirectionalBlock.FACING);
    }

    public boolean isInstalled() {
        return carUuid != null;
    }

    public boolean isPreviewing() {
        return preview;
    }

    /** 停止階の床面のY座標（下から順）。 */
    public int[] floorYs() {
        return floors.stream().mapToInt(Integer::intValue).toArray();
    }

    public String label(int floorY) {
        return labels.getOrDefault(floorY, "");
    }

    public CarModel model() {
        return model;
    }

    public SpeedSetting speed() {
        return speed;
    }

    /** ネザースターのスロット。 */
    public Container starContainer() {
        return star;
    }

    /** ネザースターがはまっているか（高速仕様を選べるか）。 */
    public boolean hasNetherStar() {
        return star.getItem(0).is(Items.NETHER_STAR);
    }

    /** ネザースターを取り出せないか。高速仕様で設置しているあいだは取り出せない。 */
    public boolean isStarLocked() {
        return isInstalled() && speed.spec().isHighSpeed();
    }

    /** ネザースターを取り出したら、設置する前の高速仕様の設定は普通（高）に戻す。 */
    private void onStarChanged() {
        if (!hasNetherStar() && !isInstalled() && speed.spec().isHighSpeed()) {
            speed = SpeedSetting.standard(SpeedSpec.NORMAL_HIGH);
        }
        sync();
    }

    public PitMode pitMode() {
        return pitMode;
    }

    public FixtureConfig fixtures() {
        return fixtures;
    }

    public AnnouncementConfig announcements() {
        return announcements;
    }

    public ShaftLayout layout() {
        int[] relative = floors.stream().mapToInt(y -> y - worldPosition.getY()).toArray();
        return new ShaftLayout(model, pitMode, relative);
    }

    /** 乗場の器具を出入口の上に付けられるか（停止階の間隔が5ブロック以上か）。 */
    public boolean allowsOverDoorHallFixtures() {
        return ShaftLayout.allowsOverDoorHallFixtures(floorYs());
    }

    public FloorProblem floorProblem() {
        return ShaftLayout.check(layout().floors());
    }

    /** 設定画面で変えた設定を反映する。設置したあとは、かごにも反映する。 */
    public void applySettings(CompoundTag tag) {
        if (!isInstalled()) {
            model = ConfigTags.parseEnum(CarModel.class, tag.getString("Model"), model);
            SpeedSpec requested = ConfigTags.parseEnum(SpeedSpec.class, tag.getString("Speed"), speed.spec());
            // 高速仕様は、ネザースターがはまっているときだけ選べる
            if (!requested.isHighSpeed() || hasNetherStar()) {
                speed = readSpeed(requested, tag);
            }
            pitMode = ConfigTags.parseEnum(PitMode.class, tag.getString("Pit"), pitMode);
        }
        fixtures = ShaftLayout.fitFixtures(ConfigTags.readFixtures(tag.getCompound("Fixtures")), floorYs());
        announcements = ConfigTags.readAnnouncements(tag.getCompound("Announcements"));
        preview = !isInstalled() && tag.getBoolean("Preview");
        CompoundTag labelTag = tag.getCompound("Labels");
        labels.clear();
        for (int floorY : floors) {
            String label = labelTag.getString(Integer.toString(floorY)).trim();
            if (!label.isEmpty()) {
                labels.put(floorY, label.length() > MAX_LABEL_LENGTH ? label.substring(0, MAX_LABEL_LENGTH) : label);
            }
        }
        ElevatorCarEntity car = installedCar();
        if (car != null) {
            car.setFixtures(fixtures);
            car.setAnnouncements(announcements);
            car.setLabels(labelArray());
        }
        sync();
    }

    /** 設定画面へ渡す設定。{@link #applySettings} と同じ形。 */
    public CompoundTag settingsTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Model", model.name());
        tag.putString("Speed", speed.spec().name());
        tag.putDouble("Acceleration", speed.acceleration());
        tag.putDouble("Jerk", speed.jerk());
        tag.putString("Pit", pitMode.name());
        tag.put("Fixtures", ConfigTags.writeFixtures(fixtures));
        tag.put("Announcements", ConfigTags.writeAnnouncements(announcements));
        tag.putBoolean("Preview", preview);
        CompoundTag labelTag = new CompoundTag();
        labels.forEach((y, label) -> labelTag.putString(Integer.toString(y), label));
        tag.put("Labels", labelTag);
        return tag;
    }

    /** 速度の仕様 spec と、tag に書かれた加速度・ジャーク（なければ標準の値）。 */
    private static SpeedSetting readSpeed(SpeedSpec spec, CompoundTag tag) {
        SpeedSetting setting = SpeedSetting.standard(spec);
        if (tag.contains("Acceleration")) {
            setting = setting.withAcceleration(tag.getDouble("Acceleration")).withJerk(tag.getDouble("Jerk"));
        }
        return setting;
    }

    private String[] labelArray() {
        return floors.stream().map(y -> labels.getOrDefault(y, "")).toArray(String[]::new);
    }

    /** 階床登録ツールで y の階を登録または解除する。結果を伝える文を返す。 */
    public Component toggleFloor(int y) {
        if (isInstalled()) {
            return Component.translatable("message.realelevator.floor.installed");
        }
        if (y == worldPosition.getY()) {
            return Component.translatable("message.realelevator.floor.lowest");
        }
        if (y < worldPosition.getY()) {
            return Component.translatable("message.realelevator.floor.below");
        }
        if (floors.remove(y)) {
            labels.remove(y);
            sync();
            return Component.translatable("message.realelevator.floor.removed", y, floors.size());
        }
        for (int floor : floors) {
            if (Math.abs(floor - y) < ShaftLayout.MIN_FLOOR_SPACING) {
                return Component.translatable("message.realelevator.floor.too_close", ShaftLayout.MIN_FLOOR_SPACING, floor);
            }
        }
        floors.add(y);
        // 間隔が狭くなって付けられなくなった配置は直す
        fixtures = ShaftLayout.fitFixtures(fixtures, floorYs());
        sync();
        return Component.translatable("message.realelevator.floor.added", y, floors.size());
    }

    // ---- ワールドの座標 ----

    /** 制御盤からの相対位置 cell のワールドの位置。 */
    public BlockPos cellPos(Cell cell) {
        int[] xz = rotate(cell.x(), cell.z());
        return worldPosition.offset(xz[0], cell.y(), xz[1]);
    }

    /** かごの原点（かご内の床面の中心）のワールドの位置。y は最下階の床面。 */
    public Vec3 carOrigin() {
        ShaftLayout layout = layout();
        double[] xz = rotate(layout.carX(), layout.carZ());
        return new Vec3(worldPosition.getX() + 0.5 + xz[0], worldPosition.getY(), worldPosition.getZ() + 0.5 + xz[1]);
    }

    /** 制御盤の向きに合わせて (x, z) を回す。かごのローカル座標と同じ決まり（{@code Box#rotateQuarterTurns}）。 */
    private int[] rotate(int x, int z) {
        int rx = x;
        int rz = z;
        for (int i = 0; i < facing().get2DDataValue(); i++) {
            int t = rx;
            rx = -rz;
            rz = t;
        }
        return new int[] {rx, rz};
    }

    private double[] rotate(double x, double z) {
        double rx = x;
        double rz = z;
        for (int i = 0; i < facing().get2DDataValue(); i++) {
            double t = rx;
            rx = -rz;
            rz = t;
        }
        return new double[] {rx, rz};
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

    // ---- 設置と撤去 ----

    /** 昇降路を設置する。結果を伝える文を返す。 */
    public Component install() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return Component.empty();
        }
        if (isInstalled()) {
            return Component.translatable("message.realelevator.install.already");
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
            level.setBlock(cellPos(rail.cell()), ModBlocks.GUIDE_RAIL.get().defaultBlockState()
                    .setValue(HorizontalDirectionalBlock.FACING, railFacing), Block.UPDATE_ALL);
        }

        Vec3 origin = carOrigin();
        ElevatorCarEntity car = ModEntities.ELEVATOR_CAR.get().create(serverLevel);
        if (car == null) {
            return Component.empty();
        }
        car.moveTo(origin.x, origin.y, origin.z, 0, 0);
        car.setup(model, facing(), floorYs(), speed);
        car.setHoistway(pitMode);
        car.setFixtures(fixtures);
        car.setAnnouncements(announcements);
        car.setLabels(labelArray());
        serverLevel.addFreshEntity(car);
        for (int floorY : floorYs()) {
            LandingDoorEntity door = ModEntities.LANDING_DOOR.get().create(serverLevel);
            if (door != null) {
                door.moveTo(origin.x, floorY, origin.z, 0, 0);
                door.setup(car);
                serverLevel.addFreshEntity(door);
            }
        }
        carUuid = car.getUUID();
        preview = false;
        sync();
        return Component.translatable("message.realelevator.install.done", floors.size());
    }

    /** 設置したかご・乗場の扉・ガイドレールを撤去する。 */
    public void uninstall() {
        if (!(level instanceof ServerLevel serverLevel) || carUuid == null) {
            return;
        }
        for (LandingDoorEntity door : ElevatorTracker.landingDoors(serverLevel)) {
            if (door.carUuid().filter(carUuid::equals).isPresent()) {
                door.discard();
            }
        }
        Entity car = serverLevel.getEntity(carUuid);
        if (car != null) {
            car.discard();
        }
        for (ShaftLayout.Rail rail : layout().rails()) {
            BlockPos pos = cellPos(rail.cell());
            if (level.getBlockState(pos).is(ModBlocks.GUIDE_RAIL.get())) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        carUuid = null;
        sync();
    }

    @Nullable
    private ElevatorCarEntity installedCar() {
        if (carUuid == null || level == null) {
            return null;
        }
        return ElevatorTracker.findCar(level, carUuid);
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
        tag.putIntArray("Floors", floorYs());
        tag.put("Settings", settingsTag());
        if (!star.getItem(0).isEmpty()) {
            tag.put("Star", star.getItem(0).save(registries));
        }
        if (carUuid != null) {
            tag.putUUID("Car", carUuid);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        floors.clear();
        floors.add(worldPosition.getY());
        for (int y : tag.getIntArray("Floors")) {
            floors.add(y);
        }
        carUuid = tag.hasUUID("Car") ? tag.getUUID("Car") : null;
        CompoundTag settings = tag.getCompound("Settings");
        model = ConfigTags.parseEnum(CarModel.class, settings.getString("Model"), CarModel.P11);
        speed = readSpeed(ConfigTags.parseEnum(SpeedSpec.class, settings.getString("Speed"), SpeedSpec.NORMAL_LOW), settings);
        // 読み込みでは変更を知らせない（同期や設定の戻しを起こさない）よう、中身を直接入れ替える
        star.getItems().set(0, ItemStack.parseOptional(registries, tag.getCompound("Star")));
        pitMode = ConfigTags.parseEnum(PitMode.class, settings.getString("Pit"), PitMode.REAL);
        fixtures = ConfigTags.readFixtures(settings.getCompound("Fixtures"));
        announcements = ConfigTags.readAnnouncements(settings.getCompound("Announcements"));
        preview = settings.getBoolean("Preview");
        labels.clear();
        CompoundTag labelTag = settings.getCompound("Labels");
        for (String key : labelTag.getAllKeys()) {
            try {
                labels.put(Integer.parseInt(key), labelTag.getString(key));
            } catch (NumberFormatException ignored) {
                // 壊れた階名は読み飛ばす
            }
        }
    }

    // ---- 設定画面 ----

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.realelevator.panel.title");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ControlPanelMenu(containerId, inventory, this);
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

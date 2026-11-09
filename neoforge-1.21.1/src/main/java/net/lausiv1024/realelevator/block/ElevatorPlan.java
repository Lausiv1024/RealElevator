package net.lausiv1024.realelevator.block;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.design.Materials;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.car.FloorEntrances;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.install.ShaftLayout.Cell;
import net.lausiv1024.realelevator.core.install.ShaftLayout.CounterweightPosition;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * エレベーターの計画。設置用ブロックの場所と向き（昇降路の配置の基準）、停止階、階名、機種・速度・ピット・配置・アナウンスの設定。
 *
 * <p>設置用ブロックが持ち、設置すると頂部の制御盤へそのまま移す。
 */
public final class ElevatorPlan {
    /** 階名の文字数の上限。ドットの表示器にはドットにある文字が3文字まで出る（フォントで描くときは全部）。 */
    public static final int MAX_LABEL_LENGTH = 4;

    private final BlockPos origin;
    private final Direction facing;
    /** 停止階の床面のY座標。基準の高さ（最下階）は常に含む。 */
    private final TreeSet<Integer> floors = new TreeSet<>();
    /** 停止階のY座標ごとの階名。空なら初期値（下から 1、2、3…）。 */
    private final Map<Integer, String> labels = new HashMap<>();
    /** 停止階のY座標ごとの読み上げ名（音声合成で「（読み上げ名）です。」と読む）。 */
    private final Map<Integer, String> speechNames = new HashMap<>();
    private CarModel model = CarModel.P11;
    /** 扉の方式。機種で選べない方式は、使うときに CO になる（{@link CarSpec}）。 */
    private DoorType door = DoorType.CENTER;
    private SpeedSetting speed = SpeedSetting.standard(SpeedSpec.NORMAL_LOW);
    private PitMode pitMode = PitMode.REAL;
    private CounterweightPosition counterweight = CounterweightPosition.REAR;
    /** 二方向出入口（前後）か。 */
    private boolean through;
    /** 展望仕様（奥壁が腰壁とガラス）。二方向出入口（前後）とは組み合わせない。 */
    private boolean observation;
    /** 二方向出入口のときの、停止階のY座標ごとの出入口。書かれていない階は前だけ。 */
    private final Map<Integer, FloorEntrances> entrances = new HashMap<>();
    private FixtureConfig fixtures = FixtureConfig.DEFAULT;
    private AnnouncementConfig announcements = AnnouncementConfig.DEFAULT;
    private InteriorConfig interior = InteriorConfig.DEFAULT;
    /** ランディングオープン（着床の直前に戸を開き始める）か。 */
    private boolean landingOpen;

    /**
     * @param origin 設置用ブロックの場所（最下階の乗場の壁）
     * @param facing 設置用ブロックの正面（乗場側）の向き
     */
    public ElevatorPlan(BlockPos origin, Direction facing) {
        this.origin = origin.immutable();
        this.facing = facing;
        floors.add(origin.getY());
    }

    public BlockPos origin() {
        return origin;
    }

    public Direction facing() {
        return facing;
    }

    // ---- 設定 ----

    /** 停止階の床面のY座標（下から順）。 */
    public int[] floorYs() {
        return floors.stream().mapToInt(Integer::intValue).toArray();
    }

    public String label(int floorY) {
        return labels.getOrDefault(floorY, "");
    }

    /** 停止階の階名（下から順、空なら初期値）。 */
    public String[] labelArray() {
        return floors.stream().map(y -> labels.getOrDefault(y, "")).toArray(String[]::new);
    }

    /** 停止階の読み上げ名（下から順、空なら階名から読む）。 */
    public String[] speechNameArray() {
        return floors.stream().map(y -> speechNames.getOrDefault(y, "")).toArray(String[]::new);
    }

    public CarModel model() {
        return model;
    }

    /** 機種と扉の方式。 */
    public CarSpec spec() {
        return new CarSpec(model, door);
    }

    public SpeedSetting speed() {
        return speed;
    }

    public void setSpeed(SpeedSetting speed) {
        this.speed = speed;
    }

    public PitMode pitMode() {
        return pitMode;
    }

    /** 釣合い重りの位置（後ろ落とし／横落とし）。二方向出入口と展望仕様では横落としに決まる。 */
    public CounterweightPosition counterweight() {
        return through || observation ? CounterweightPosition.SIDE : counterweight;
    }

    /** 展望仕様か。 */
    public boolean isObservation() {
        return observation && !through;
    }

    /** 二方向出入口（前後）か。 */
    public boolean isThrough() {
        return through;
    }

    /** 停止階 floorY で使う出入口。一方向出入口なら前だけ。最下階（設置用ブロックの階）は前を必ず使う。 */
    public FloorEntrances entrances(int floorY) {
        if (!through) {
            return FloorEntrances.FRONT;
        }
        FloorEntrances value = entrances.getOrDefault(floorY, FloorEntrances.FRONT);
        return floorY == origin.getY() && !value.allowedOnLowestFloor() ? FloorEntrances.FRONT : value;
    }

    /** 停止階ごとの出入口（下から順）。 */
    public FloorEntrances[] entranceArray() {
        return floors.stream().map(this::entrances).toArray(FloorEntrances[]::new);
    }

    public FixtureConfig fixtures() {
        return fixtures;
    }

    public AnnouncementConfig announcements() {
        return announcements;
    }

    public void setAnnouncements(AnnouncementConfig announcements) {
        this.announcements = announcements;
    }

    public boolean landingOpen() {
        return landingOpen;
    }

    public InteriorConfig interior() {
        return interior;
    }

    /** 乗場の器具を出入口の上に付けられるか（停止階の間隔が5ブロック以上か）。 */
    public boolean allowsOverDoorHallFixtures() {
        return ShaftLayout.allowsOverDoorHallFixtures(floorYs());
    }

    /**
     * 停止階 y を登録または解除する。結果を伝える文を返す。
     *
     * @return 変えたかどうかと、伝える文
     */
    public FloorChange toggleFloor(int y) {
        if (y == origin.getY()) {
            return new FloorChange(false, Component.translatable("message.realelevator.floor.lowest"));
        }
        if (y < origin.getY()) {
            return new FloorChange(false, Component.translatable("message.realelevator.floor.below"));
        }
        int[] before = floorYs();
        if (floors.remove(y)) {
            labels.remove(y);
            speechNames.remove(y);
            entrances.remove(y);
            // 階ごとの乗場の意匠の階の番号を、停止階を変えたあとの番号に直す
            interior = interior.remapLandingFloors(before, floorYs());
            return new FloorChange(true, Component.translatable("message.realelevator.floor.removed", y, floors.size()));
        }
        if (floors.size() >= ShaftLayout.MAX_FLOORS) {
            return new FloorChange(false, Component.translatable("message.realelevator.floor.too_many", ShaftLayout.MAX_FLOORS));
        }
        for (int floor : floors) {
            if (Math.abs(floor - y) < ShaftLayout.MIN_FLOOR_SPACING) {
                return new FloorChange(false, Component.translatable("message.realelevator.floor.too_close", ShaftLayout.MIN_FLOOR_SPACING, floor));
            }
        }
        floors.add(y);
        interior = interior.remapLandingFloors(before, floorYs());
        // 間隔が狭くなって付けられなくなった配置は直す
        fixtures = ShaftLayout.fitFixtures(fixtures, floorYs());
        return new FloorChange(true, Component.translatable("message.realelevator.floor.added", y, floors.size()));
    }

    public record FloorChange(boolean changed, Component message) {
    }

    /**
     * 設置する前だけ変えられる設定（機種・速度・ピット）を反映する。高速仕様は、ネザースターがはまっているときだけ選べる。
     */
    public void applyInstallSettings(CompoundTag tag, boolean hasNetherStar) {
        model = ConfigTags.parseEnum(CarModel.class, tag.getString("Model"), model);
        door = new CarSpec(model, ConfigTags.parseEnum(DoorType.class, tag.getString("Door"), door)).door();
        SpeedSpec requested = ConfigTags.parseEnum(SpeedSpec.class, tag.getString("Speed"), speed.spec());
        if (!requested.isHighSpeed() || hasNetherStar) {
            speed = readSpeed(requested, tag);
        }
        pitMode = ConfigTags.parseEnum(PitMode.class, tag.getString("Pit"), pitMode);
        counterweight = ConfigTags.parseEnum(CounterweightPosition.class, tag.getString("Counterweight"), counterweight);
        through = tag.getBoolean("Through");
        observation = tag.getBoolean("Observation") && !through;
        readEntrances(tag.getCompound("Entrances"));
    }

    /** 設置したあとも変えられる設定（配置・アナウンス・階名）を反映する。 */
    public void applyRenewal(CompoundTag tag) {
        fixtures = ShaftLayout.fitFixtures(ConfigTags.readFixtures(tag.getCompound("Fixtures")), floorYs());
        announcements = ConfigTags.readAnnouncements(tag.getCompound("Announcements"));
        landingOpen = tag.getBoolean("LandingOpen");
        interior = Materials.sanitize(ConfigTags.readInterior(tag.getCompound("Interior")));
        CompoundTag labelTag = tag.getCompound("Labels");
        labels.clear();
        for (int floorY : floors) {
            String label = labelTag.getString(Integer.toString(floorY)).trim();
            if (!label.isEmpty()) {
                labels.put(floorY, label.length() > MAX_LABEL_LENGTH ? label.substring(0, MAX_LABEL_LENGTH) : label);
            }
        }
        CompoundTag speechTag = tag.getCompound("SpeechNames");
        speechNames.clear();
        for (int floorY : floors) {
            String name = speechTag.getString(Integer.toString(floorY)).trim();
            if (!name.isEmpty()) {
                speechNames.put(floorY, name.length() > Announcement.MAX_SPEECH_NAME_LENGTH
                        ? name.substring(0, Announcement.MAX_SPEECH_NAME_LENGTH) : name);
            }
        }
    }

    /** 設定画面へ渡す設定。{@link #applyInstallSettings} と {@link #applyRenewal} と同じ形に、停止階を足したもの。 */
    public CompoundTag settingsTag() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Model", model.name());
        tag.putString("Door", spec().door().name());
        tag.putString("Speed", speed.spec().name());
        tag.putDouble("Acceleration", speed.acceleration());
        tag.putDouble("Jerk", speed.jerk());
        tag.putString("Pit", pitMode.name());
        tag.putString("Counterweight", counterweight.name());
        tag.putBoolean("Through", through);
        tag.putBoolean("Observation", observation);
        CompoundTag entranceTag = new CompoundTag();
        entrances.forEach((y, value) -> entranceTag.putString(Integer.toString(y), value.name()));
        tag.put("Entrances", entranceTag);
        tag.put("Fixtures", ConfigTags.writeFixtures(fixtures));
        tag.put("Announcements", ConfigTags.writeAnnouncements(announcements));
        tag.putBoolean("LandingOpen", landingOpen);
        tag.put("Interior", ConfigTags.writeInterior(interior));
        CompoundTag labelTag = new CompoundTag();
        labels.forEach((y, label) -> labelTag.putString(Integer.toString(y), label));
        tag.put("Labels", labelTag);
        CompoundTag speechTag = new CompoundTag();
        speechNames.forEach((y, name) -> speechTag.putString(Integer.toString(y), name));
        tag.put("SpeechNames", speechTag);
        tag.putIntArray("Floors", floorYs());
        return tag;
    }

    private void readEntrances(CompoundTag tag) {
        entrances.clear();
        for (String key : tag.getAllKeys()) {
            try {
                int y = Integer.parseInt(key);
                if (floors.contains(y)) {
                    entrances.put(y, ConfigTags.parseEnum(FloorEntrances.class, tag.getString(key), FloorEntrances.FRONT));
                }
            } catch (NumberFormatException ignored) {
                // 壊れた階は読み飛ばす
            }
        }
    }

    /** 速度の仕様 spec と、tag に書かれた加速度・ジャーク（なければ標準の値）。 */
    public static SpeedSetting readSpeed(SpeedSpec spec, CompoundTag tag) {
        SpeedSetting setting = SpeedSetting.standard(spec);
        if (tag.contains("Acceleration")) {
            setting = setting.withAcceleration(tag.getDouble("Acceleration")).withJerk(tag.getDouble("Jerk"));
        }
        return setting;
    }

    // ---- 昇降路の配置 ----

    public ShaftLayout layout() {
        int[] relative = floors.stream().mapToInt(y -> y - origin.getY()).toArray();
        return new ShaftLayout(spec(), pitMode, counterweight(), relative, through ? entranceArray() : null);
    }

    /** 設置用ブロックからの相対位置 cell のワールドの位置。 */
    public BlockPos cellPos(Cell cell) {
        int x = cell.x();
        int z = cell.z();
        for (int i = 0; i < facing.get2DDataValue(); i++) {
            int t = x;
            x = -z;
            z = t;
        }
        return origin.offset(x, cell.y(), z);
    }

    /** かごの原点（かご内の床面の中心）のワールドの位置。y は最下階の床面。 */
    public Vec3 carOrigin() {
        ShaftLayout layout = layout();
        double x = layout.carX();
        double z = layout.carZ();
        // 設置用ブロックの向きに合わせて回す。かごのローカル座標と同じ決まり（{@code Box#rotateQuarterTurns}）
        for (int i = 0; i < facing.get2DDataValue(); i++) {
            double t = x;
            x = -z;
            z = t;
        }
        return new Vec3(origin.getX() + 0.5 + x, origin.getY(), origin.getZ() + 0.5 + z);
    }

    // ---- 保存 ----

    public CompoundTag save() {
        CompoundTag tag = settingsTag();
        tag.put("Origin", NbtUtils.writeBlockPos(origin));
        tag.putString("Facing", facing.getSerializedName());
        return tag;
    }

    /** {@link #save} で保存したもの。場所や向きが読めなければ、fallback の場所と向きを使う。 */
    public static ElevatorPlan load(CompoundTag tag, BlockPos fallbackOrigin, Direction fallbackFacing) {
        BlockPos origin = NbtUtils.readBlockPos(tag, "Origin").orElse(fallbackOrigin);
        Direction facing = Direction.byName(tag.getString("Facing"));
        if (facing == null || facing.getAxis().isVertical()) {
            facing = fallbackFacing;
        }
        ElevatorPlan plan = new ElevatorPlan(origin, facing);
        for (int y : tag.getIntArray("Floors")) {
            if (y > origin.getY()) {
                plan.floors.add(y);
            }
        }
        plan.model = ConfigTags.parseEnum(CarModel.class, tag.getString("Model"), CarModel.P11);
        plan.door = ConfigTags.parseEnum(DoorType.class, tag.getString("Door"), DoorType.CENTER);
        plan.speed = readSpeed(ConfigTags.parseEnum(SpeedSpec.class, tag.getString("Speed"), SpeedSpec.NORMAL_LOW), tag);
        plan.pitMode = ConfigTags.parseEnum(PitMode.class, tag.getString("Pit"), PitMode.REAL);
        plan.counterweight = ConfigTags.parseEnum(CounterweightPosition.class, tag.getString("Counterweight"), CounterweightPosition.REAR);
        plan.through = tag.getBoolean("Through");
        plan.observation = tag.getBoolean("Observation");
        plan.fixtures = ConfigTags.readFixtures(tag.getCompound("Fixtures"));
        plan.announcements = ConfigTags.readAnnouncements(tag.getCompound("Announcements"));
        plan.landingOpen = tag.getBoolean("LandingOpen");
        plan.interior = ConfigTags.readInterior(tag.getCompound("Interior"));
        plan.readEntrances(tag.getCompound("Entrances"));
        CompoundTag labelTag = tag.getCompound("Labels");
        for (String key : labelTag.getAllKeys()) {
            try {
                plan.labels.put(Integer.parseInt(key), labelTag.getString(key));
            } catch (NumberFormatException ignored) {
                // 壊れた階名は読み飛ばす
            }
        }
        CompoundTag speechTag = tag.getCompound("SpeechNames");
        for (String key : speechTag.getAllKeys()) {
            try {
                plan.speechNames.put(Integer.parseInt(key), speechTag.getString(key));
            } catch (NumberFormatException ignored) {
                // 壊れた読み上げ名は読み飛ばす
            }
        }
        return plan;
    }
}

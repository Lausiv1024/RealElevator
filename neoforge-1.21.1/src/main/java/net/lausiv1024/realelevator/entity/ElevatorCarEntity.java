package net.lausiv1024.realelevator.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.announce.AnnouncementScript;
import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.car.FloorEntrances;
import net.lausiv1024.realelevator.core.control.CallType;
import net.lausiv1024.realelevator.core.control.CollectiveControl;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.control.EntranceCalls;
import net.lausiv1024.realelevator.core.control.OperationTiming;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.display.HallLantern;
import net.lausiv1024.realelevator.core.door.DoorController;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.door.DoorState;
import net.lausiv1024.realelevator.core.door.DoorTiming;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.install.ShaftLayout.CounterweightPosition;
import net.lausiv1024.realelevator.core.install.ShaftLayout.PitMode;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.motion.TravelLimits;
import net.lausiv1024.realelevator.core.motion.Trip;
import net.lausiv1024.realelevator.network.AnnouncePayload;
import net.lausiv1024.realelevator.registry.ModSounds;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * エレベーターのかご。
 *
 * <p>位置は発車情報（{@link Trip}）とゲーム時刻から、戸の開き具合は戸の状態とゲーム時刻から求め、
 * サーバーとクライアントで同じ計算をする。サーバーから送るのは機種・向き・停止階・発車情報・戸の状態・登録中の呼びだけで、
 * 毎tickの位置や開き具合は同期しない。エンティティの位置はかご内の床面の中心。
 *
 * <p>運行は乗合全自動（{@link CollectiveControl}）で、戸が全閉になるまで発車せず、着床すると戸を開く。
 * 乗場の扉（{@link LandingDoorEntity}）は、かごが着床している階のものだけが、かごの戸と同じ開き具合で動く。
 */
public class ElevatorCarEntity extends Entity implements ButtonHost {
    private static final EntityDataAccessor<Byte> DATA_MODEL = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<CompoundTag> DATA_FLOORS = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DATA_TRIP = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DATA_DOOR = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DATA_CALLS = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DATA_FIXTURES = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** セーフティシューの検知に使う、戸板のまわりのわずかな余裕。 */
    private static final double SAFETY_SHOE_MARGIN = 0.01;
    /** 床や屋根に入り込んだとみなす深さの下限。衝突判定が面の手前に残すすき間より大きくする。 */
    private static final double SINK_TOLERANCE = 1e-4;
    /** 床や屋根に入り込んだとみなす深さの上限。これより深いものは、かごの下にいるものとして押し上げない。 */
    private static final double MAX_SINK = 1.0;

    /** 停止階の床面のY座標（下から順）。 */
    private int[] floors = new int[0];
    /** 二方向出入口（前後）か。 */
    private boolean through;
    /** 二方向出入口のときの、階ごとの出入口（下から順）。 */
    private FloorEntrances[] floorEntrances = new FloorEntrances[0];
    /** 乗場呼びがどちらの側で押されたか（二方向出入口）。 */
    private final EntranceCalls entranceCalls = new EntranceCalls();
    /** 今（または最後に）開いた扉の側。戸の開き具合は、この側の扉にだけ効く。 */
    private final Set<Entrance> openSides = EnumSet.of(Entrance.FRONT);
    /** 扉が開いているあいだに反対側で呼ばれた側。今の扉が閉まりきったら、発車せずにこの側を開く。サーバーだけで使う。 */
    private final Set<Entrance> pendingSides = EnumSet.noneOf(Entrance.class);
    /** 到着したときに応えた呼び（あとから開く側の応答灯を、その扉を開くときに消すのに使う）。サーバーだけで使う。 */
    private boolean pendingCarCall;
    private TravelDirection pendingHallDirection = TravelDirection.NONE;
    /** ボタンの並びのうち、正面の操作盤のボタンの数（そのあとが奥の操作盤）。 */
    private int frontButtonCount;
    /** 頂部の制御盤（下のブロック）の位置。ウィザードで設置したかごだけが持つ。サーバーだけで使う。 */
    @Nullable
    private BlockPos controllerPos;
    /** 昇降路のピットの方式。ウィザードで設置したかごだけが持ち、昇降路の機構を描く。 */
    @Nullable
    private PitMode pitMode;
    /** 釣合い重りの位置（ウィザードで設置したかごだけ）。 */
    private CounterweightPosition counterweight = CounterweightPosition.REAR;
    /** pitMode と停止階から作った昇降路の機構。停止階などが変わったら作り直す。 */
    @Nullable
    private HoistMechanism hoist;
    /** 停止階の階名（下から順）。空文字なら初期値（下から 1、2、3…）を使う。 */
    private String[] labels = new String[0];
    private List<Button> buttons = List.of();
    private FixtureConfig fixtures = FixtureConfig.DEFAULT;
    /** アナウンスの設定。サーバーだけが使う。 */
    private AnnouncementConfig announcements = AnnouncementConfig.DEFAULT;
    /** 戸が開ききったら行先方向のアナウンスを流すか。サーバーだけが使う。 */
    private boolean directionAnnouncementPending;
    /** この停止で最後に流した行先方向。同じ方向を繰り返さないために使う。サーバーだけが使う。 */
    private TravelDirection spokenDirection = TravelDirection.NONE;
    /** 行先方向のアナウンスが流れ終わるゲーム時刻。サーバーだけが使う。 */
    private long directionSpeakingUntil = Long.MIN_VALUE;
    /** 最近流したアナウンス（新しい順に最大8件）。動作の確認用。サーバーだけが使う。 */
    private final Deque<Announcement> recentAnnouncements = new ArrayDeque<>();
    /** 停止階が2つ以上あるときだけ作る。 */
    @Nullable
    private CollectiveControl control;
    private SpeedSetting speed = SpeedSetting.standard(SpeedSpec.NORMAL_LOW);
    @Nullable
    private Trip trip;
    private final DoorController door = new DoorController(DoorTiming.STANDARD);
    /** 今いる階（走行中は最後に止まった階）。サーバーだけが使う。 */
    private int currentFloor;
    /** 走行中の行先の階。サーバーだけが使う。 */
    private int targetFloor;
    /** 最後に位置を計算したゲーム時刻。描画の補間に使う。 */
    private long lastTick;
    /** この走行の到着チャイムをもう鳴らしたか。サーバーだけが使う。 */
    private boolean arrivalAnnounced;
    /** ホールランタンの点滅を始めたゲーム時刻（到着チャイムを鳴らした時刻）。点滅の位相の起点。 */
    private long lanternTick;
    /** 到着チャイムを鳴らした行先の階。走行中、この階のホールランタンを点滅させる。なければ −1。 */
    private int announcedFloor = -1;
    /** 到着チャイムを鳴らしたときに先読みした、到着後の進行方向。 */
    private TravelDirection announcedDirection = TravelDirection.NONE;
    /** 発車を決めたあと、実際に動き始めるゲーム時刻。発車待ちでなければ −1。サーバーだけが使う。 */
    private long departureTick = -1;
    /** 運搬の移動量を最後に評価したゲーム時刻と、そのときと前のtickのかごの位置。 */
    private long carryTick = Long.MIN_VALUE;
    private double carryFromY;
    private double carryToY;

    public ElevatorCarEntity(EntityType<? extends ElevatorCarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        setAnnouncements(AnnouncementConfig.DEFAULT);
    }

    /** 速度の仕様の標準の加速度とジャークで設定する。 */
    public void setup(CarModel model, Direction facing, int[] floors, SpeedSpec speedSpec) {
        setup(CarSpec.of(model), facing, floors, SpeedSetting.standard(speedSpec));
    }

    public void setup(CarModel model, Direction facing, int[] floors, SpeedSetting speed) {
        setup(CarSpec.of(model), facing, floors, speed);
    }

    public void setup(CarSpec spec, Direction facing, int[] floors, SpeedSetting speed) {
        this.entityData.set(DATA_MODEL, CarShapes.encode(spec));
        this.entityData.set(DATA_FACING, facing);
        int[] sorted = floors.clone();
        Arrays.sort(sorted);
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Floors", sorted);
        if (pitMode != null) {
            tag.putString("Pit", pitMode.name());
            tag.putString("Counterweight", counterweight.name());
        }
        writeEntrances(tag);
        this.entityData.set(DATA_FLOORS, tag);
        applyFloors(sorted);
        this.speed = speed;
        this.currentFloor = nearestFloor(getY());
        this.targetFloor = currentFloor;
        // 停止階が変わったら、付けられない配置を直す
        setFixtures(fixtures);
        setBoundingBox(makeBoundingBox());
    }

    /**
     * 二方向出入口（前後）にする（設置したとき）。サーバーで呼ぶ。
     *
     * @param entrances 階ごとの出入口（下から順）
     */
    public void setThrough(FloorEntrances[] entrances) {
        this.through = true;
        this.floorEntrances = entrances.clone();
        this.hoist = null;
        CompoundTag tag = this.entityData.get(DATA_FLOORS).copy();
        writeEntrances(tag);
        this.entityData.set(DATA_FLOORS, tag);
        rebuildButtons();
    }

    private void writeEntrances(CompoundTag tag) {
        if (!through) {
            tag.remove("Entrances");
            return;
        }
        byte[] bytes = new byte[floorEntrances.length];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) floorEntrances[i].ordinal();
        }
        tag.putByteArray("Entrances", bytes);
    }

    private void readEntrances(CompoundTag tag) {
        through = tag.contains("Entrances");
        byte[] bytes = tag.getByteArray("Entrances");
        FloorEntrances[] values = FloorEntrances.values();
        floorEntrances = new FloorEntrances[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            floorEntrances[i] = values[Math.clamp(bytes[i], 0, values.length - 1)];
        }
    }

    /** 二方向出入口（前後）か。 */
    public boolean isThrough() {
        return through;
    }

    /** 停止階 floor で使う出入口。一方向出入口なら正面だけ。 */
    public Set<Entrance> entrancesAt(int floor) {
        if (!through || floor < 0 || floor >= floorEntrances.length) {
            return EnumSet.of(Entrance.FRONT);
        }
        return floorEntrances[floor].entrances();
    }

    /** 頂部の制御盤の位置を覚える（設置したとき）。 */
    public void setController(BlockPos pos) {
        this.controllerPos = pos.immutable();
    }

    /** 頂部の制御盤（下のブロック）の位置。ウィザードで設置していないかごは null。 */
    @Nullable
    public BlockPos controllerPos() {
        return controllerPos;
    }

    /** 速度の設定。 */
    public SpeedSetting speed() {
        return speed;
    }

    /** 昇降路の機構を付ける（ウィザードで設置したとき）。サーバーで呼ぶ。 */
    public void setHoistway(PitMode pitMode) {
        setHoistway(pitMode, CounterweightPosition.REAR);
    }

    /** 昇降路の機構を付ける（ウィザードで設置したとき）。サーバーで呼ぶ。 */
    public void setHoistway(PitMode pitMode, CounterweightPosition counterweight) {
        this.pitMode = pitMode;
        this.counterweight = counterweight;
        this.hoist = null;
        CompoundTag tag = this.entityData.get(DATA_FLOORS).copy();
        tag.putString("Pit", pitMode.name());
        tag.putString("Counterweight", counterweight.name());
        this.entityData.set(DATA_FLOORS, tag);
    }

    /** 昇降路の機構。ウィザードで設置していないかごは null。 */
    @Nullable
    public HoistMechanism hoist() {
        if (hoist == null && pitMode != null && floors.length >= 2) {
            hoist = new HoistMechanism(spec(), pitMode, counterweight, through, floors);
        }
        return hoist;
    }

    /** ゲーム時刻 tick での釣合い重りの当たり判定の箱（ワールド座標）。機構がなければ null。 */
    @Nullable
    public AABB counterweightAt(double tick) {
        HoistMechanism hoist = hoist();
        if (hoist == null) {
            return null;
        }
        Box box = hoist.counterweight(yAt(tick)).rotateQuarterTurns(facing().get2DDataValue());
        return new AABB(box.minX() + getX(), box.minY(), box.minZ() + getZ(), box.maxX() + getX(), box.maxY(), box.maxZ() + getZ());
    }

    /** 昇降路の全体（ワールド座標）。機構がなければ null。 */
    @Nullable
    public AABB hoistwayBounds() {
        HoistMechanism hoist = hoist();
        if (hoist == null) {
            return null;
        }
        Box box = hoist.shaftBounds().rotateQuarterTurns(facing().get2DDataValue());
        return new AABB(box.minX() + getX(), box.minY(), box.minZ() + getZ(), box.maxX() + getX(), box.maxY(), box.maxZ() + getZ());
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        // 昇降路の機構は、かごが見えていなくても見えることがある
        AABB shaft = hoistwayBounds();
        return shaft != null ? shaft.minmax(super.getBoundingBoxForCulling()) : super.getBoundingBoxForCulling();
    }

    private void applyFloors(int[] floors) {
        this.floors = floors;
        this.hoist = null;
        this.control = floors.length >= 2 ? new CollectiveControl(floors.length) : null;
        rebuildButtons();
    }

    private void rebuildButtons() {
        List<Button> front = ButtonLayout.carPanel(spec(), floors.length, fixtures.panelSides());
        frontButtonCount = front.size();
        if (!through) {
            this.buttons = front;
            return;
        }
        // 奥の出入口の袖壁にも、同じ操作盤を180°回して置く
        List<Button> both = new ArrayList<>(front);
        for (Button button : front) {
            both.add(new Button(button.kind(), button.floor(), button.box().rotateQuarterTurns(Entrance.REAR.quarterTurns())));
        }
        this.buttons = List.copyOf(both);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODEL, CarShapes.encode(CarSpec.of(CarModel.P11)));
        builder.define(DATA_FACING, Direction.SOUTH);
        builder.define(DATA_FLOORS, new CompoundTag());
        builder.define(DATA_TRIP, new CompoundTag());
        builder.define(DATA_DOOR, new CompoundTag());
        builder.define(DATA_CALLS, new CompoundTag());
        builder.define(DATA_FIXTURES, new CompoundTag());
    }

    public CarModel model() {
        return spec().model();
    }

    /** 機種と扉の方式。 */
    public CarSpec spec() {
        return CarShapes.decode(this.entityData.get(DATA_MODEL));
    }

    /** 出入口のある正面の向き。 */
    public Direction facing() {
        return this.entityData.get(DATA_FACING);
    }

    public int floorCount() {
        return floors.length;
    }

    /** 停止階の床面のY座標（下から順）。 */
    public int[] floors() {
        return floors.clone();
    }

    /** 床面のY座標が y の停止階の番号。停止階でなければ −1。 */
    public int floorIndexAt(double y) {
        for (int i = 0; i < floors.length; i++) {
            if (floors[i] == y) {
                return i;
            }
        }
        return -1;
    }

    /** 操作盤と階床表示器の配置。 */
    public FixtureConfig fixtures() {
        return fixtures;
    }

    /** アナウンスの設定。 */
    public AnnouncementConfig announcements() {
        return announcements;
    }

    /** アナウンスの設定を変える。サーバーで呼ぶ。 */
    public void setAnnouncements(AnnouncementConfig announcements) {
        this.announcements = announcements;
        door.setCloseAnnouncementTicks(announcements.doorClose() ? AnnouncementScript.DOOR_CLOSE_WAIT_TICKS : 0);
    }

    /** 操作盤と階床表示器の配置を変える。停止階の間隔で付けられない配置は直す。サーバーで呼ぶ。 */
    public void setFixtures(FixtureConfig fixtures) {
        this.fixtures = ShaftLayout.fitFixtures(fixtures, floors);
        this.entityData.set(DATA_FIXTURES, ConfigTags.writeFixtures(this.fixtures));
        rebuildButtons();
    }

    public boolean isMoving() {
        return trip != null;
    }

    public DoorState doorState() {
        return door.state();
    }

    // ---- 呼びとボタン ----

    /** 正面の操作盤のかご呼び。 */
    public PressResult pressCarCall(int floor) {
        return pressCarCall(floor, Entrance.FRONT);
    }

    /** かご呼び（行先ボタン）。side は押した操作盤の側。かごがその階に止まっていれば、その側の戸を開く。 */
    public PressResult pressCarCall(int floor, Entrance side) {
        if (level().isClientSide || control == null) {
            return PressResult.INVALID;
        }
        PressResult result = control.pressCar(floor, currentFloor, trip == null);
        if (result == PressResult.REGISTERED || result == PressResult.ALREADY_REGISTERED) {
            entranceCalls.pressCar(floor, side);
            if (result == PressResult.ALREADY_REGISTERED) {
                // 反対側の操作盤ですでに登録されていても、この側の応答灯のために送り直す
                syncCalls();
            }
        }
        handlePress(result, CallType.CAR, side);
        return result;
    }

    /** 正面の乗場の乗場呼び。 */
    public PressResult pressHallCall(int floor, TravelDirection direction) {
        return pressHallCall(floor, direction, Entrance.FRONT);
    }

    /** 乗場呼び（▲▼ボタン）。side はボタンを押した乗場の側。かごがその階に止まっていて、その方向へ進めるなら、その側の戸を開く。 */
    public PressResult pressHallCall(int floor, TravelDirection direction, Entrance side) {
        if (level().isClientSide || control == null) {
            return PressResult.INVALID;
        }
        PressResult result = control.pressHall(floor, direction, currentFloor, trip == null);
        if (result == PressResult.REGISTERED || result == PressResult.ALREADY_REGISTERED) {
            entranceCalls.press(floor, direction, side);
            if (result == PressResult.ALREADY_REGISTERED) {
                // 反対側の乗場ですでに登録されていても、この側の応答灯のために送り直す
                syncCalls();
            }
        }
        handlePress(result, CallType.HALL, side);
        return result;
    }

    /** 今いる階で、side の側から開けと言われたときに開く扉。その階にその側の出入口がなければ、その階にある扉。 */
    private Entrance doorFor(Entrance side) {
        Set<Entrance> available = entrancesAt(currentFloor);
        return available.contains(side) ? side : available.iterator().next();
    }

    /**
     * 今いる階で side の側の扉を開く。両側の扉が同時に開くことはなく、反対側の扉が開いているあいだは、
     * それが閉まりきってから開く。
     *
     * @return すぐに開いた（開き直した）か。反対側を待つなら false
     */
    private boolean requestOpen(Entrance side, CallType callType) {
        Entrance target = doorFor(side);
        if (!door.isClosed() && !openSides.contains(target)) {
            pendingSides.add(target);
            return false;
        }
        if (door.isClosed()) {
            openSides.clear();
            openSides.add(target);
        }
        openDoor(callType);
        return true;
    }

    /** @param side その場で応えるときに開く扉の側（押した操作盤や乗場の側） */
    private void handlePress(PressResult result, CallType callType, Entrance side) {
        if (result == PressResult.SERVED_HERE) {
            if (callType == CallType.HALL) {
                lanternTick = level().getGameTime();
                playArrivalChime(currentFloor, control != null ? control.direction() : TravelDirection.NONE);
            }
            boolean alreadyOpen = door.state() == DoorState.OPEN;
            if (!requestOpen(side, callType)) {
                syncCalls();
                return;
            }
            // かごがいる階の乗場呼びでは、階の案内はせず、行先方向だけを流す（戸がもう開いていればすぐに）
            if (callType == CallType.HALL && announcements.arrival()) {
                if (alreadyOpen) {
                    announceDirection();
                } else {
                    directionAnnouncementPending = true;
                }
            }
        }
        if (result == PressResult.SERVED_HERE || result == PressResult.REGISTERED) {
            // その場で応えた乗場呼びでも進行方向が決まるので、階床表示器の矢印のために送る
            syncCalls();
        }
    }

    /** 側 side の操作盤の開ボタン。その側の扉を開く（反対側が開いていれば、閉まってから）。走行中は効かない。 */
    public boolean openDoor(Entrance side) {
        if (level().isClientSide || trip != null) {
            return false;
        }
        requestOpen(side, CallType.CAR);
        return true;
    }

    /** 開ボタン。走行中は効かない。閉まっているときは、最後に開いた側の扉を開く。 */
    public boolean openDoor(CallType callType) {
        if (level().isClientSide || trip != null) {
            return false;
        }
        Set<Entrance> available = entrancesAt(currentFloor);
        if (openSides.isEmpty() || !available.containsAll(openSides)) {
            openSides.clear();
            openSides.add(available.iterator().next());
        }
        door.open(level().getGameTime(), door.timing().dwellTicks(callType));
        syncDoor();
        return true;
    }

    /** 閉ボタン。開放待機中なら、すぐに閉じ始める。 */
    public boolean closeDoor() {
        if (level().isClientSide || door.state() != DoorState.OPEN) {
            return false;
        }
        // 閉ボタンでは、戸閉アナウンスを流しながらすぐに閉め始める。行先方向を流している間は戸閉アナウンスを流さない
        if (announcements.doorClose() && !isSpeakingDirection(level().getGameTime())) {
            announce(Announcement.doorClose());
        }
        door.close(level().getGameTime());
        syncDoor();
        return true;
    }

    public boolean isCarCallLit(int floor) {
        return control != null && floor >= 0 && floor < floors.length && control.hasCarCall(floor);
    }

    public boolean isHallCallLit(int floor, TravelDirection direction) {
        return control != null && floor >= 0 && floor < floors.length && control.hasHallCall(floor, direction);
    }

    /** 側 side の乗場の、乗場呼びの応答灯。二方向出入口では、その側で押された呼びだけ点く。 */
    public boolean isHallCallLit(int floor, TravelDirection direction, Entrance side) {
        if (!through) {
            return isHallCallLit(floor, direction);
        }
        // 運行制御の呼びは到着で消えるが、あとから開く側は扉を開くまで点けておく
        return floor >= 0 && floor < floors.length && entranceCalls.isRegistered(floor, direction, side);
    }

    @Override
    public List<Button> buttons() {
        return buttons;
    }

    @Override
    public AABB buttonBox(int index) {
        Box box = buttons.get(index).box().rotateQuarterTurns(facing().get2DDataValue());
        return CarShapes.toAabb(box, getX(), getY(), getZ());
    }

    /**
     * クライアントへ送っている呼びのデータで、側 side の操作盤の floor へのかご呼びが登録されているか。
     * クライアントの応答灯はこれで決まる（動作確認のテスト用）。
     */
    public boolean isCarCallSynced(int floor, Entrance side) {
        CompoundTag tag = this.entityData.get(DATA_CALLS);
        List<long[]> sideCalls = new ArrayList<>();
        for (int i = 0; tag.contains("Side" + i); i++) {
            sideCalls.add(tag.getLongArray("Side" + i));
        }
        EntranceCalls synced = new EntranceCalls();
        synced.restore(sideCalls.toArray(long[][]::new));
        return synced.isCarRegistered(floor, side);
    }

    /** ボタン index がどちらの出入口の操作盤のものか。並びは正面の操作盤、奥の操作盤の順。 */
    private Entrance buttonSide(int index) {
        return index < frontButtonCount ? Entrance.FRONT : Entrance.REAR;
    }

    @Override
    public boolean isButtonLit(int index) {
        Button button = buttons.get(index);
        if (button.kind() != ButtonLayout.Kind.CAR_CALL) {
            return false;
        }
        // 二方向出入口では、押した側の操作盤の行先ボタンだけ点く
        return through ? entranceCalls.isCarRegistered(button.floor(), buttonSide(index)) : isCarCallLit(button.floor());
    }

    @Override
    public void pressButton(int index) {
        Button button = buttons.get(index);
        Entrance side = buttonSide(index);
        switch (button.kind()) {
            case OPEN -> openDoor(side);
            case CLOSE -> closeDoor();
            case CAR_CALL -> pressCarCall(button.floor(), side);
            case HALL_UP, HALL_DOWN -> {
            }
        }
    }

    // ---- tick ----

    @Override
    public void tick() {
        super.tick();
        long now = level().getGameTime();
        lastTick = now;
        if (trip != null) {
            setPos(getX(), trip.positionAt(now), getZ());
        }
        // 誰も乗っていないtickでも、運搬の基準の位置を進めておく
        carryDelta(now);
        if (!level().isClientSide) {
            serverTick(now);
        }
    }

    private void serverTick(long now) {
        // 到着したtickの最後の移動量も乗っているエンティティに加えられるよう、走行情報は1tick遅れて消す
        if (trip != null && trip.isArrivedAt(now - 1)) {
            setTrip(null);
            currentFloor = targetFloor;
            serveCurrentFloor(now);
        } else if (trip != null) {
            stopOnTheWayIfPossible(now);
            announceArrivalIfCertain(now);
        }

        // 行先方向を流している間に自動で閉めるときは、戸閉アナウンスを流さず（待たず）に閉める
        door.setCloseAnnouncementTicks(announcements.doorClose() && !isSpeakingDirection(now)
                ? AnnouncementScript.DOOR_CLOSE_WAIT_TICKS : 0);
        DoorController.Event doorEvent = door.tick(now);
        if (doorEvent == DoorController.Event.CLOSE_ANNOUNCED) {
            // 開放待機が終わった。戸閉アナウンスを流し終わってから閉め始める
            announce(Announcement.doorClose());
        }
        boolean doorChanged = doorEvent == DoorController.Event.STATE_CHANGED;
        if (doorChanged && directionAnnouncementPending) {
            if (door.state() == DoorState.OPEN) {
                // 戸が開ききったら行先方向を流す
                directionAnnouncementPending = false;
                announceDirection();
            } else if (door.state() != DoorState.OPENING) {
                directionAnnouncementPending = false;
            }
        }
        announceDirectionIfChanged();
        if (door.state() == DoorState.CLOSING && isDoorObstructed(now)) {
            door.obstruct(now);
            doorChanged = true;
        }
        if (doorChanged) {
            syncDoor();
        }

        if (trip == null && door.isClosed() && !pendingSides.isEmpty()) {
            // 扉が開いているあいだに反対側で呼ばれていたら、発車せずにその側を開く
            Entrance next = pendingSides.iterator().next();
            pendingSides.remove(next);
            openSides.clear();
            openSides.add(next);
            // 到着したときに応えた呼びのうち、この側のものの応答灯を消す
            entranceCalls.served(currentFloor, next, pendingCarCall, pendingHallDirection, entrancesAt(currentFloor));
            if (pendingSides.isEmpty()) {
                pendingCarCall = false;
                pendingHallDirection = TravelDirection.NONE;
            }
            syncCalls();
            // 反対側の扉を開くときも、到着チャイムを鳴らしてランタンを点滅させ直す
            lanternTick = now;
            playArrivalChime(currentFloor, control != null ? control.direction() : TravelDirection.NONE);
            door.open(now, door.timing().dwellTicks(CallType.HALL));
            syncDoor();
            departureTick = -1;
        } else if (trip == null && door.isClosed() && control != null) {
            TravelDirection before = control.direction();
            int next = control.nextStop(currentFloor);
            if (control.direction() != before) {
                // 階床表示器の矢印のため、進行方向の変化も送る
                syncCalls();
            }
            if (next == currentFloor) {
                departureTick = -1;
                serveCurrentFloor(now);
            } else if (next >= 0) {
                // 発車を決めてから、少し遅れて動き始める
                if (departureTick < 0) {
                    departureTick = now + OperationTiming.START_DELAY_TICKS;
                }
                if (now >= departureTick) {
                    departureTick = -1;
                    targetFloor = next;
                    setTrip(new Trip(now, getY(), floors[next], speed.limits()));
                }
            } else {
                departureTick = -1;
            }
        } else if (trip == null) {
            // 戸が開き直したら発車を取りやめる
            departureTick = -1;
        }
    }

    /**
     * 走行中、行先より手前に止まるべき階があれば、近い順に、減速が間に合う最初の階へ行先を変える。
     * 間に合わない階は通過し、その呼びは残して後で応える。
     */
    private void stopOnTheWayIfPossible(long now) {
        if (control == null || trip == null) {
            return;
        }
        int step = trip.to() > trip.from() ? 1 : -1;
        for (int floor = currentFloor + step; floor != targetFloor; floor += step) {
            if (!control.isStopOnTheWay(floor)) {
                continue;
            }
            Trip shorter = trip.retarget(now, floors[floor]);
            if (shorter != null) {
                targetFloor = floor;
                setTrip(shorter);
                return;
            }
        }
    }

    /** 今いる階の呼びに応えて戸を開く。走行中に到着チャイムを鳴らしていなければ、ここで鳴らす。 */
    private void serveCurrentFloor(long now) {
        boolean carCall = control != null && control.hasCarCall(currentFloor);
        boolean upBefore = control != null && control.hasHallCall(currentFloor, TravelDirection.UP);
        boolean downBefore = control != null && control.hasHallCall(currentFloor, TravelDirection.DOWN);
        CallType served = control != null ? control.arrive(currentFloor) : CallType.CAR;
        // 応えた乗場呼びの方向（arrive で消えたほう）
        TravelDirection hallDirection = upBefore && !control.hasHallCall(currentFloor, TravelDirection.UP) ? TravelDirection.UP
                : downBefore && control != null && !control.hasHallCall(currentFloor, TravelDirection.DOWN) ? TravelDirection.DOWN
                : TravelDirection.NONE;
        // 両側を開くときは、先に呼ばれた側から片側ずつ
        List<Entrance> sides = entranceCalls.arrive(currentFloor, carCall, hallDirection, entrancesAt(currentFloor));
        openSides.clear();
        openSides.add(sides.get(0));
        pendingSides.clear();
        pendingSides.addAll(sides.subList(1, sides.size()));
        pendingCarCall = carCall;
        pendingHallDirection = hallDirection;
        if (!arrivalAnnounced) {
            lanternTick = now;
            playArrivalChime(currentFloor, control != null ? control.direction() : TravelDirection.NONE);
        }
        arrivalAnnounced = false;
        announcedFloor = -1;
        door.open(now, door.timing().dwellTicks(served));
        syncDoor();
        syncCalls();
        spokenDirection = TravelDirection.NONE;
        // 戸を開き始めるときに「◯階です。」を流し（戸はアナウンスを待たない）、開ききったら行先方向を流す
        if (announcements.arrival()) {
            announce(Announcement.arrival(floorLabel(currentFloor)));
            directionAnnouncementPending = true;
        }
    }

    /** 行先方向のアナウンス（「上へ参ります。」「下へ参ります。」）を流す。方向が決まっていなければ流さない。 */
    private void announceDirection() {
        TravelDirection direction = control != null ? control.direction() : TravelDirection.NONE;
        if (direction != TravelDirection.NONE) {
            announce(Announcement.direction(direction));
            spokenDirection = direction;
            directionSpeakingUntil = level().getGameTime() + AnnouncementScript.DIRECTION_TICKS;
        }
    }

    /** 行先方向のアナウンスを流しているところか。 */
    private boolean isSpeakingDirection(long now) {
        return now < directionSpeakingUntil;
    }

    /**
     * 戸が開ききっている間に、呼びの登録などで進行方向が決まったり変わったりしたら、行先方向を流す。
     * 戸閉アナウンスを流して閉め始めるのを待っている間は、そちらを途中で切らないよう流さない。
     */
    private void announceDirectionIfChanged() {
        if (!announcements.arrival() || control == null || trip != null || directionAnnouncementPending
                || door.state() != DoorState.OPEN || door.isWaitingToClose()) {
            return;
        }
        TravelDirection direction = control.direction();
        if (direction != TravelDirection.NONE && direction != spokenDirection) {
            announceDirection();
        }
    }

    /** このかごが見えているクライアントへアナウンスを送る。鳴らし方はクライアントに任せる。 */
    private void announce(Announcement announcement) {
        PacketDistributor.sendToPlayersTrackingEntity(this, new AnnouncePayload(getId(), announcement));
        recentAnnouncements.addFirst(announcement);
        while (recentAnnouncements.size() > 8) {
            recentAnnouncements.removeLast();
        }
    }

    /** 最近流したアナウンス（新しい順）。動作の確認用。 */
    public List<Announcement> recentAnnouncements() {
        return List.copyOf(recentAnnouncements);
    }

    /**
     * 行先への到着が確定し、到着が近づいたら、行先階の乗場で到着チャイムを鳴らす。
     *
     * <p>到着が確定するのは、行先の1つ手前の階に減速しても止まれなくなったとき（途中階で呼ばれても止まれなくなったとき）。
     * 行先が隣の階なら発車した時点で確定する。長い走行で早く鳴りすぎないよう、到着の6秒前までは待つ。
     * チャイムの向きは、到着したあとの進行方向を先読みして決める。
     */
    private void announceArrivalIfCertain(long now) {
        if (arrivalAnnounced || trip == null || control == null) {
            return;
        }
        int step = trip.to() > trip.from() ? 1 : -1;
        int beforeTarget = targetFloor - step;
        boolean certain = beforeTarget == currentFloor || trip.retarget(now, floors[beforeTarget]) == null;
        if (HallLantern.shouldChime(certain, trip.arrivalTick() - now)) {
            arrivalAnnounced = true;
            announcedFloor = targetFloor;
            announcedDirection = directionAfterArrivingAt(targetFloor);
            // ホールランタンもチャイムと同時に点滅を始める
            lanternTick = now;
            playArrivalChime(targetFloor, announcedDirection);
            syncDoor();
        }
    }

    /** floor に着いて呼びに応えたあとの進行方向。今の呼びのまま着いたとして先読みする。 */
    private TravelDirection directionAfterArrivingAt(int floor) {
        long[][] calls = control.snapshot();
        CollectiveControl probe = new CollectiveControl(floors.length);
        probe.restore(calls[0], calls[1], calls[2], control.direction());
        probe.arrive(floor);
        return probe.direction();
    }

    /** 設定で有効なら、停止階 floor の乗場で、ホールランタンの位置から到着チャイムを鳴らす。下りは C6→G#5、それ以外は G#5→C6。 */
    private void playArrivalChime(int floor, TravelDirection direction) {
        if (!announcements.chime()) {
            return;
        }
        Vec3 position = lanternCenter(floor);
        SoundEvent sound = (direction == TravelDirection.DOWN ? ModSounds.CHIME_DOWN : ModSounds.CHIME_UP).get();
        level().playSound(null, position.x, position.y, position.z, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** 停止階 floor のホールランタンの中心（ワールド座標）。 */
    private Vec3 lanternCenter(int floor) {
        AABB bounds = null;
        for (HallLantern.Section section : HallLantern.sections(spec(), fixtures, isTerminalFloor(floor))) {
            AABB box = CarShapes.toAabb(section.box().rotateQuarterTurns(facing().get2DDataValue()), getX(), floors[floor], getZ());
            bounds = bounds == null ? box : bounds.minmax(box);
        }
        if (bounds != null) {
            return bounds.getCenter();
        }
        // ホールランタンを付けないときは、出入口の上の中央から鳴らす
        Box entrance = CarGeometry.entrance(spec());
        Box above = new Box(0, entrance.maxY(), DoorGeometry.landingFace(spec()), 0, entrance.maxY(), DoorGeometry.landingFace(spec()))
                .rotateQuarterTurns(facing().get2DDataValue());
        return new Vec3(getX() + above.minX(), floors[floor] + above.minY(), getZ() + above.minZ());
    }

    /** 最上階または最下階か。 */
    public boolean isTerminalFloor(int floor) {
        return floor == 0 || floor == floors.length - 1;
    }

    /** 到着チャイムを鳴らした行先の階。なければ −1。 */
    public int announcedFloor() {
        return announcedFloor;
    }

    /** この走行の到着チャイムをもう鳴らしたか。 */
    public boolean isArrivalAnnounced() {
        return arrivalAnnounced;
    }

    /** ホールランタンが点滅を始めてからの経過時間 [tick]。点滅の位相に使う。 */
    public double lanternTicks(double tick) {
        return tick - lanternTick;
    }

    /**
     * 停止階 floor のホールランタンの点灯する段（点滅の消灯中かどうかは含めない）。
     *
     * <p>到着チャイムを鳴らしてから到着するまでは、行先階のランタンを先読みした方向で点け、
     * 到着して扉が開いている間は、その階のランタンを今の進行方向で点ける。
     */
    public Set<TravelDirection> lanternSections(int floor) {
        return lanternSections(floor, Entrance.FRONT);
    }

    /** 側 side の乗場のホールランタン。二方向出入口では、開く（開いている）側の乗場だけ点ける。 */
    public Set<TravelDirection> lanternSections(int floor, Entrance side) {
        if (trip != null) {
            if (floor != announcedFloor || !lanternSides(floor).contains(side)) {
                return Set.of();
            }
            return HallLantern.litSections(floor, floors.length, announcedDirection, true);
        }
        boolean doorOpenHere = floorIndexAt(getY()) == floor && door.state() != DoorState.CLOSED && openSides.contains(side);
        return HallLantern.litSections(floor, floors.length, control != null ? control.direction() : TravelDirection.NONE, doorOpenHere);
    }

    /** 到着する階 floor で、到着チャイムのあとランタンを点ける側（そこで開く扉の側）。 */
    private Set<Entrance> lanternSides(int floor) {
        if (!through || control == null) {
            return entrancesAt(floor);
        }
        // 先に開く側
        List<Entrance> sides = entranceCalls.wouldOpen(floor, control.hasCarCall(floor), announcedDirection, entrancesAt(floor));
        return EnumSet.of(sides.get(0));
    }

    /** セーフティシュー：閉まりかけの戸（かごと乗場の両方）に生き物が触れているかどうか。 */
    private boolean isDoorObstructed(long now) {
        double fraction = door.fractionAt(now);
        List<AABB> panels = new ArrayList<>(8);
        for (Entrance entrance : openSides) {
            for (DoorGeometry.Side side : DoorGeometry.Side.values()) {
                for (Box box : CarShapes.doorPanels(spec(), facing(), side, fraction, entrance)) {
                    panels.add(CarShapes.toAabb(box, getX(), getY(), getZ()).inflate(SAFETY_SHOE_MARGIN));
                }
            }
        }
        if (panels.isEmpty()) {
            return false;
        }
        AABB search = panels.stream().reduce(AABB::minmax).orElseThrow();
        return !level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> !entity.isSpectator() && panels.stream().anyMatch(entity.getBoundingBox()::intersects)).isEmpty();
    }

    // ---- 階床表示 ----

    /** 停止階 floor の階名。 */
    public String floorLabel(int floor) {
        return floor >= 0 && floor < labels.length && !labels[floor].isEmpty() ? labels[floor] : FloorIndicator.defaultLabel(floor);
    }

    /** 停止階の階名を変える（下から順）。サーバーで呼ぶ。 */
    public void setLabels(String[] labels) {
        this.labels = Arrays.copyOf(labels, floors.length);
        for (int i = 0; i < this.labels.length; i++) {
            if (this.labels[i] == null) {
                this.labels[i] = "";
            }
        }
        CompoundTag tag = this.entityData.get(DATA_FLOORS).copy();
        tag.put("Labels", writeLabels(this.labels));
        this.entityData.set(DATA_FLOORS, tag);
    }

    private static ListTag writeLabels(String[] labels) {
        ListTag list = new ListTag();
        for (String label : labels) {
            list.add(StringTag.valueOf(label));
        }
        return list;
    }

    private static String[] readLabels(CompoundTag tag) {
        ListTag list = tag.getList("Labels", Tag.TAG_STRING);
        String[] labels = new String[list.size()];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = list.getString(i);
        }
        return labels;
    }

    /** ゲーム時刻 tick に階床表示器に出す階。かごの位置に最も近い階。 */
    public int indicatorFloor(double tick) {
        return floors.length > 0 ? FloorIndicator.displayedFloor(yAt(tick), floors) : 0;
    }

    /** ゲーム時刻 tick に階床表示器に出す階名。長い階間の途中ではハイフン。 */
    public String indicatorLabel(double tick) {
        return floors.length > 0 ? FloorIndicator.displayedLabel(yAt(tick), floors, this::floorLabel) : floorLabel(0);
    }

    /** 階床表示器に出す矢印の向き。走行中はその向き、停止中は次に進む方向。 */
    public TravelDirection indicatorDirection() {
        if (trip != null) {
            return trip.to() > trip.from() ? TravelDirection.UP : TravelDirection.DOWN;
        }
        return control != null ? control.direction() : TravelDirection.NONE;
    }

    /** クライアントへ送っている進行方向。クライアントの矢印はこれで決まる。 */
    public TravelDirection syncedIndicatorDirection() {
        return parseEnum(TravelDirection.class, this.entityData.get(DATA_CALLS).getString("Direction"), TravelDirection.NONE);
    }

    // ---- 位置と形状 ----

    /**
     * ゲーム時刻 now のtickに、かごが前のtickから実際に動いた量。乗っているエンティティの運搬に使う。
     *
     * <p>クライアントのゲーム時刻はサーバーの時刻に合わせて数tick飛んだり戻ったりすることがあるので、
     * 「1tick分の移動量」ではなく、前に評価したtickの位置からの差を返す。同じtickに何度呼んでも同じ値になり、
     * かごとエンティティのどちらが先にtickしても変わらない。
     */
    public double carryDelta(long now) {
        if (now != carryTick) {
            carryFromY = carryTick == Long.MIN_VALUE ? yAt(now) : carryToY;
            carryToY = yAt(now);
            carryTick = now;
        }
        return carryToY - carryFromY;
    }

    /** {@link #carryDelta} の基準にした、前のtickでのかごの床面のY座標。 */
    public double carryFromY(long now) {
        carryDelta(now);
        return carryFromY;
    }

    /** ゲーム時刻 tick（partialTickを含めてよい）でのかごの床面のY座標。 */
    public double yAt(double tick) {
        return trip != null ? trip.positionAt(tick) : getY();
    }

    /** ゲーム時刻 tick での戸の開き具合（0 が全閉、1 が全開）。 */
    public double doorFractionAt(double tick) {
        return door.fractionAt(tick);
    }

    /** 出入口 entrance の戸の開き具合。今開いている側でなければ全閉。 */
    public double doorFractionAt(double tick, Entrance entrance) {
        return openSides.contains(entrance) ? door.fractionAt(tick) : 0;
    }

    /**
     * 描画に使うゲーム時刻。バニラの補間と同じく、直前のtickと最新のtickの間を partialTick で表す。
     */
    public double renderTick(float partialTick) {
        return lastTick - 1 + partialTick;
    }

    /** 描画用の位置。 */
    public double renderY(float partialTick) {
        return trip != null ? trip.positionAt(renderTick(partialTick)) : Mth.lerp(partialTick, yo, getY());
    }

    /** ゲーム時刻 tick での当たり判定の箱（ワールド座標）。床・壁・天井とかごの戸。 */
    public List<AABB> collisionBoxesAt(long tick) {
        double y = yAt(tick);
        List<AABB> boxes = new ArrayList<>();
        for (Part part : parts()) {
            boxes.add(CarShapes.toAabb(part.box(), getX(), y, getZ()));
        }
        for (Entrance entrance : entrances()) {
            for (Box panel : CarShapes.doorPanels(spec(), facing(), DoorGeometry.Side.CAR, doorFractionAt(tick, entrance), entrance)) {
                boxes.add(CarShapes.toAabb(panel, getX(), y, getZ()));
            }
        }
        return boxes;
    }

    public List<Part> parts() {
        return CarShapes.parts(spec(), facing(), through);
    }

    /** かごの出入口（二方向出入口なら正面と奥）。 */
    public List<Entrance> entrances() {
        return through ? List.of(Entrance.FRONT, Entrance.REAR) : List.of(Entrance.FRONT);
    }

    /** かご全体の外形（ワールド座標）。床面のY座標 y の位置に置いたもの。戸が袖壁の外へはみ出す分を含む。 */
    public AABB exteriorAt(double y) {
        AABB exterior = CarShapes.toAabb(CarShapes.exterior(spec(), facing()), getX(), y, getZ());
        for (Entrance entrance : entrances()) {
            exterior = exterior.minmax(CarShapes.toAabb(CarShapes.doorTravelBounds(spec(), facing(), DoorGeometry.Side.CAR, entrance), getX(), y, getZ()));
        }
        return exterior;
    }

    /**
     * entity がかごと一緒に動くべき位置にいるかどうか。かごの外形の真上または中にいて、
     * 足元が床面（少し下まで許す）から屋根の少し上までの間にあれば運ぶ。
     */
    public boolean carries(Entity entity, double carY) {
        Box exterior = CarShapes.exterior(spec(), facing());
        double localX = entity.getX() - getX();
        double localZ = entity.getZ() - getZ();
        double feet = entity.getY() - carY;
        return localX > exterior.minX() && localX < exterior.maxX()
                && localZ > exterior.minZ() && localZ < exterior.maxZ()
                && feet > exterior.minY() - 0.25 && feet < exterior.maxY() + 0.5;
    }

    /**
     * entity の足が、ゲーム時刻 tick でのかごの床または屋根の中に入り込んでいれば、その上面のY座標を返す。
     * 入り込んでいなければ NaN。
     *
     * <p>クライアントのゲーム時刻はサーバーの時刻に合わせて数tick飛ぶことがあり、そのときかごの床は1tick分より大きく動く。
     * 運搬では1tick分の移動量しか加えないので、上昇中は床の中に取り残されて下へ抜けてしまう。それを防ぐために使う。
     */
    public double sunkSurface(Entity entity, long tick) {
        double y = yAt(tick);
        double feet = entity.getY();
        for (Part part : parts()) {
            if (part.type() != CarGeometry.PartType.FLOOR && part.type() != CarGeometry.PartType.CEILING) {
                continue;
            }
            AABB box = CarShapes.toAabb(part.box(), getX(), y, getZ());
            boolean above = entity.getX() > box.minX && entity.getX() < box.maxX
                    && entity.getZ() > box.minZ && entity.getZ() < box.maxZ;
            if (above && feet < box.maxY - SINK_TOLERANCE && feet > box.maxY - MAX_SINK) {
                return box.maxY;
            }
        }
        return Double.NaN;
    }

    private int nearestFloor(double y) {
        int nearest = 0;
        for (int i = 1; i < floors.length; i++) {
            if (Math.abs(floors[i] - y) < Math.abs(floors[nearest] - y)) {
                nearest = i;
            }
        }
        return nearest;
    }

    // ---- 同期 ----

    private void setTrip(@Nullable Trip trip) {
        this.trip = trip;
        this.entityData.set(DATA_TRIP, trip != null ? writeTrip(trip) : new CompoundTag());
    }

    private void syncDoor() {
        this.entityData.set(DATA_DOOR, writeDoorState());
    }

    private void syncCalls() {
        this.entityData.set(DATA_CALLS, control != null ? writeCalls(control) : new CompoundTag());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_MODEL.equals(key) || DATA_FACING.equals(key)) {
            setBoundingBox(makeBoundingBox());
        }
        if (!level().isClientSide) {
            return;
        }
        if (DATA_MODEL.equals(key) || DATA_FLOORS.equals(key)) {
            CompoundTag floorTag = this.entityData.get(DATA_FLOORS);
            pitMode = floorTag.contains("Pit") ? parseEnum(PitMode.class, floorTag.getString("Pit"), PitMode.REAL) : null;
            counterweight = parseEnum(CounterweightPosition.class, floorTag.getString("Counterweight"), CounterweightPosition.REAR);
            readEntrances(floorTag);
            hoist = null;
            applyFloors(floorTag.getIntArray("Floors"));
            labels = readLabels(floorTag);
            readCalls(this.entityData.get(DATA_CALLS));
        } else if (DATA_TRIP.equals(key)) {
            Trip previous = trip;
            trip = readTrip(this.entityData.get(DATA_TRIP));
            if (trip == null && previous != null) {
                // 到着。クライアントの時刻が遅れていても、行先にぴったり着床させる
                setPos(getX(), previous.to(), getZ());
            }
        } else if (DATA_DOOR.equals(key)) {
            readDoorState(this.entityData.get(DATA_DOOR));
        } else if (DATA_CALLS.equals(key)) {
            readCalls(this.entityData.get(DATA_CALLS));
        } else if (DATA_FIXTURES.equals(key)) {
            fixtures = ConfigTags.readFixtures(this.entityData.get(DATA_FIXTURES));
            rebuildButtons();
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        return exteriorAt(getY());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        // 走行中の高さはクライアント側で運転曲線から求めるので、サーバーから届いた値は使わない
        setPos(x, trip != null ? getY() : y, z);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        ElevatorTracker.add(this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        ElevatorTracker.remove(this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    // ---- 保存 ----

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Model", model().name());
        // 戸の状態（"Door"）と別のキーにする
        tag.putString("DoorType", spec().door().name());
        tag.putString("Facing", facing().getSerializedName());
        tag.putIntArray("Floors", floors);
        tag.putString("Speed", speed.spec().name());
        if (controllerPos != null) {
            tag.put("Controller", NbtUtils.writeBlockPos(controllerPos));
        }
        tag.putDouble("Acceleration", speed.acceleration());
        tag.putDouble("Jerk", speed.jerk());
        if (pitMode != null) {
            tag.putString("Pit", pitMode.name());
            tag.putString("Counterweight", counterweight.name());
        }
        writeEntrances(tag);
        tag.putByte("PendingSides", sideMask(pendingSides));
        tag.putBoolean("PendingCarCall", pendingCarCall);
        tag.putString("PendingHallDirection", pendingHallDirection.name());
        if (trip != null) {
            tag.put("Trip", writeTrip(trip));
        }
        tag.put("Door", writeDoorState());
        tag.putBoolean("ArrivalAnnounced", arrivalAnnounced);
        if (control != null) {
            tag.put("Calls", writeCalls(control));
        }
        tag.put("Fixtures", ConfigTags.writeFixtures(fixtures));
        tag.put("Labels", writeLabels(labels));
        tag.put("Announcements", ConfigTags.writeAnnouncements(announcements));
        tag.putInt("CurrentFloor", currentFloor);
        tag.putInt("TargetFloor", targetFloor);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        CarModel model = parseEnum(CarModel.class, tag.getString("Model"), CarModel.P11);
        Direction facing = Direction.byName(tag.getString("Facing"));
        if (facing == null || facing.getAxis().isVertical()) {
            facing = Direction.SOUTH;
        }
        pitMode = tag.contains("Pit") ? parseEnum(PitMode.class, tag.getString("Pit"), PitMode.REAL) : null;
        counterweight = parseEnum(CounterweightPosition.class, tag.getString("Counterweight"), CounterweightPosition.REAR);
        controllerPos = NbtUtils.readBlockPos(tag, "Controller").orElse(null);
        SpeedSetting speed = SpeedSetting.standard(parseEnum(SpeedSpec.class, tag.getString("Speed"), SpeedSpec.NORMAL_LOW));
        if (tag.contains("Acceleration")) {
            speed = speed.withAcceleration(tag.getDouble("Acceleration")).withJerk(tag.getDouble("Jerk"));
        }
        DoorType door = parseEnum(DoorType.class, tag.getString("DoorType"), DoorType.CENTER);
        readEntrances(tag);
        pendingSides.clear();
        pendingSides.addAll(sides(tag.getByte("PendingSides")));
        pendingCarCall = tag.getBoolean("PendingCarCall");
        pendingHallDirection = parseEnum(TravelDirection.class, tag.getString("PendingHallDirection"), TravelDirection.NONE);
        setup(new CarSpec(model, door), facing, tag.getIntArray("Floors"), speed);
        setFixtures(ConfigTags.readFixtures(tag.getCompound("Fixtures")));
        setLabels(readLabels(tag));
        setAnnouncements(ConfigTags.readAnnouncements(tag.getCompound("Announcements")));
        // 保存時に走行中・開閉中だった場合は、ゲーム時刻に合わせてそのまま続きから動く
        setTrip(tag.contains("Trip") ? readTrip(tag.getCompound("Trip")) : null);
        readDoorState(tag.getCompound("Door"));
        arrivalAnnounced = tag.getBoolean("ArrivalAnnounced");
        syncDoor();
        readCalls(tag.getCompound("Calls"));
        syncCalls();
        if (tag.contains("CurrentFloor")) {
            currentFloor = Mth.clamp(tag.getInt("CurrentFloor"), 0, Math.max(0, floors.length - 1));
            targetFloor = Mth.clamp(tag.getInt("TargetFloor"), 0, Math.max(0, floors.length - 1));
        }
    }

    private static CompoundTag writeTrip(Trip trip) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("StartTick", trip.startTick());
        tag.putDouble("From", trip.from());
        tag.putDouble("To", trip.to());
        TravelLimits limits = trip.limits();
        tag.putDouble("MaxVelocity", limits.maxVelocity());
        tag.putDouble("MaxAcceleration", limits.maxAcceleration());
        tag.putDouble("MaxJerk", limits.maxJerk());
        return tag;
    }

    @Nullable
    private static Trip readTrip(CompoundTag tag) {
        if (!tag.contains("StartTick")) {
            return null;
        }
        // 上限値を持たない古い保存データは、速度の仕様の標準の値で走る
        TravelLimits limits = tag.contains("MaxVelocity")
                ? new TravelLimits(tag.getDouble("MaxVelocity"), tag.getDouble("MaxAcceleration"), tag.getDouble("MaxJerk"))
                : parseEnum(SpeedSpec.class, tag.getString("Speed"), SpeedSpec.NORMAL_LOW).limits();
        return new Trip(tag.getLong("StartTick"), tag.getDouble("From"), tag.getDouble("To"), limits);
    }

    private static CompoundTag writeDoor(DoorController door) {
        CompoundTag tag = new CompoundTag();
        tag.putString("State", door.state().name());
        tag.putLong("StartTick", door.startTick());
        tag.putDouble("StartFraction", door.startFraction());
        tag.putInt("Dwell", door.dwellTicks());
        return tag;
    }

    private void readDoorState(CompoundTag tag) {
        readDoor(tag, door);
        openSides.clear();
        openSides.addAll(tag.contains("Sides") ? sides(tag.getByte("Sides")) : EnumSet.of(Entrance.FRONT));
        lanternTick = tag.getLong("LanternTick");
        announcedFloor = tag.contains("AnnouncedFloor") ? tag.getInt("AnnouncedFloor") : -1;
        announcedDirection = parseEnum(TravelDirection.class, tag.getString("AnnouncedDirection"), TravelDirection.NONE);
    }

    private CompoundTag writeDoorState() {
        CompoundTag tag = writeDoor(door);
        tag.putByte("Sides", sideMask(openSides));
        tag.putLong("LanternTick", lanternTick);
        tag.putInt("AnnouncedFloor", announcedFloor);
        tag.putString("AnnouncedDirection", announcedDirection.name());
        return tag;
    }

    private static void readDoor(CompoundTag tag, DoorController door) {
        door.restore(parseEnum(DoorState.class, tag.getString("State"), DoorState.CLOSED),
                tag.getLong("StartTick"), tag.getDouble("StartFraction"), tag.getInt("Dwell"));
    }

    private static byte sideMask(Set<Entrance> sides) {
        int mask = 0;
        for (Entrance side : sides) {
            mask |= 1 << side.ordinal();
        }
        return (byte) mask;
    }

    private static Set<Entrance> sides(int mask) {
        Set<Entrance> sides = EnumSet.noneOf(Entrance.class);
        for (Entrance side : Entrance.values()) {
            if ((mask & 1 << side.ordinal()) != 0) {
                sides.add(side);
            }
        }
        return sides;
    }

    private CompoundTag writeCalls(CollectiveControl control) {
        CompoundTag tag = writeCallsOnly(control);
        long[][] sideCalls = entranceCalls.snapshot();
        for (int i = 0; i < sideCalls.length; i++) {
            tag.putLongArray("Side" + i, sideCalls[i]);
        }
        return tag;
    }

    private static CompoundTag writeCallsOnly(CollectiveControl control) {
        long[][] snapshot = control.snapshot();
        CompoundTag tag = new CompoundTag();
        tag.putLongArray("Car", snapshot[0]);
        tag.putLongArray("Up", snapshot[1]);
        tag.putLongArray("Down", snapshot[2]);
        tag.putString("Direction", control.direction().name());
        return tag;
    }

    private void readCalls(CompoundTag tag) {
        if (control != null) {
            control.restore(tag.getLongArray("Car"), tag.getLongArray("Up"), tag.getLongArray("Down"),
                    parseEnum(TravelDirection.class, tag.getString("Direction"), TravelDirection.NONE));
        }
        // 書かれている分だけ読む（並びの長さは EntranceCalls#snapshot が決める）
        List<long[]> sideCalls = new ArrayList<>();
        for (int i = 0; tag.contains("Side" + i); i++) {
            sideCalls.add(tag.getLongArray("Side" + i));
        }
        entranceCalls.restore(sideCalls.toArray(long[][]::new));
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

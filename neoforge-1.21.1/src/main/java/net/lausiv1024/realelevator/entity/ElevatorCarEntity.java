package net.lausiv1024.realelevator.entity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.announce.Announcement;
import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.control.GroupDispatcher;
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
import net.lausiv1024.realelevator.core.control.CancelReplan;
import net.lausiv1024.realelevator.core.control.CollectiveControl;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.control.EntranceCalls;
import net.lausiv1024.realelevator.core.control.OperationTiming;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.control.WheelchairCalls;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.design.InteriorGeometry;
import net.lausiv1024.realelevator.core.design.LandingLayout;
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
import net.lausiv1024.realelevator.block.PowerReceiverBlock;
import net.lausiv1024.realelevator.block.PowerReceiverBlockEntity;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final EntityDataAccessor<CompoundTag> DATA_INTERIOR = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    /** 群管理の群（同じ群のかごで乗場呼びを分け合う）。群に入っていなければ空。 */
    private static final EntityDataAccessor<Optional<UUID>> DATA_GROUP = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    /** 群管理の見積もりで、開いている戸が閉まるまでの時間とみなす長さ [秒]。 */
    private static final double OPEN_DOOR_SECONDS = 6;

    /** セーフティシューの検知に使う、戸板のまわりのわずかな余裕。 */
    private static final double SAFETY_SHOE_MARGIN = 0.01;
    /** ナッジング中にブザーを鳴らす間隔 [tick]。ブザーの音は0.5秒なので、鳴る・止むを繰り返す断続音になる。 */
    private static final int BUZZER_INTERVAL_TICKS = 20;
    /** ブザーを鳴らす高さ（かごの床から）。アナウンスと同じかごの中のスピーカー。 */
    private static final double BUZZER_HEIGHT = 2.5;
    /** 床や屋根に入り込んだとみなす深さの下限。衝突判定が面の手前に残すすき間より大きくする。 */
    private static final double SINK_TOLERANCE = 1e-4;
    /** 床や屋根に入り込んだとみなす深さの上限。これより深いものは、かごの下にいるものとして押し上げない。 */
    private static final double MAX_SINK = 1.0;
    /** 止まっているかごで、床に入り込んだものを押し上げる深さの下限。サーバーから届く位置の丸めの分でも衝突判定をすり抜けるので、ごく小さくする。 */
    private static final double RESTING_SINK_TOLERANCE = 1e-7;
    /** 止まっているかごで、床に入り込んだものを押し上げる深さの上限。かごの下にいるものを持ち上げないよう、浅くする。 */
    private static final double RESTING_MAX_SINK = 0.05;
    /** 到着チャイムの長さ [tick]（2音の間隔0.45秒＋余韻1.6秒）。 */
    private static final int CHIME_TICKS = 41;
    /** かご内でしゃがんでいるあいだ、開ボタンを押したものとして扱う間隔 [tick]（使用キーの押したままの繰り返しと同じ）。 */
    private static final int SNEAK_OPEN_INTERVAL_TICKS = 4;
    /** クライアントで、ほかのエンティティをかごの床に合わせるときに見込む、位置の遅れ [tick]。 */
    private static final int REMOTE_LAG_TICKS = 20;
    /** かごが止まったあとも、ほかのエンティティをかごの床に合わせ続ける時間 [tick]（遅れて届く位置で沈まないように）。 */
    private static final int REMOTE_SETTLE_TICKS = 10;

    /** 停止階の床面のY座標（下から順）。 */
    private int[] floors = new int[0];
    /** 二方向出入口（前後）か。 */
    private boolean through;
    /** 展望仕様（奥壁が腰壁とガラス）か。 */
    private boolean observation;
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
    /** あとから開く側も、車椅子用のボタンで呼ばれた扱い（開放待機を延ばし、ゆっくり閉じる）で開くか。サーバーだけで使う。 */
    private boolean pendingWheelchair;
    /** 応答灯の点いた行先ボタン（ボタンの番号）を最後に押したゲーム時刻。2度押しキャンセルの判定に使う。サーバーだけで使う。 */
    private final Map<Integer, Long> litPressTicks = new HashMap<>();
    /** ランディングオープン（着床の直前に戸を開き始める）か。サーバーだけで使う。 */
    private boolean landingOpen;
    /** ランディングオープンで、着床する前にこの走行の行先に着いた扱いにしたか（戸が開き始めている）。 */
    private boolean landingOpened;
    /** つないでいる運転切替盤の位置。つないでいなければ null で、常に通常運転の扱い。サーバーだけで使う。 */
    @Nullable
    private BlockPos powerReceiverPos;
    /** 休止中か（運転切替盤に信号がない）。表示器をハイフンにするため、呼びと一緒に同期する。 */
    private boolean resting;
    /** ナッジング中に、次にブザーを鳴らすゲーム時刻。サーバーだけで使う。 */
    private long nextBuzzTick = Long.MIN_VALUE;
    /** 今向かっている階の呼びが取り消されたので、着いても戸を開けないか。サーバーだけで使う。 */
    private boolean skipDoorsOnArrival;
    /** 車椅子用操作盤・乗場の車椅子用ボタンで登録された呼び。 */
    private final WheelchairCalls wheelchairCalls = new WheelchairCalls();
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
    /** 停止階の読み上げ名（下から順。空なら階名から読む）。 */
    private String[] speechNames = new String[0];
    private List<Button> buttons = List.of();
    private FixtureConfig fixtures = FixtureConfig.DEFAULT;
    /** かご内装。描画にだけ使う。 */
    private InteriorConfig interior = InteriorConfig.DEFAULT;
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
    /** 最後に位置を計算した時刻（{@link ElevatorClock}）。描画の補間に使う。 */
    private double lastTick;
    /** 設定画面のプレビューで描くだけのかご（ワールドには加えない）か。表示器の矢印とホールランタンを上向きで点けて見せる。 */
    private boolean preview;
    /** 最後に走っていたときの時刻。クライアントだけが使う。 */
    private double lastTravelClock = Double.NEGATIVE_INFINITY;
    /** 最後に到着チャイムを鳴らした階と、鳴り終わるゲーム時刻。 */
    private int chimeFloor = -1;
    private long chimeEndTick = Long.MIN_VALUE;
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
    private double carryTick = Double.NaN;
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

    /** 展望仕様にする（設置したとき）。サーバーで呼ぶ。二方向出入口（前後）とは組み合わせない。 */
    public void setObservation(boolean observation) {
        this.observation = observation && !through;
        CompoundTag tag = this.entityData.get(DATA_FLOORS).copy();
        writeEntrances(tag);
        this.entityData.set(DATA_FLOORS, tag);
    }

    /** 展望仕様（奥壁が腰壁とガラス）か。 */
    public boolean isObservation() {
        return observation;
    }

    private void writeEntrances(CompoundTag tag) {
        tag.putBoolean("Observation", observation);
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
        observation = tag.getBoolean("Observation") && !through;
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
    /** 群管理の群。群に入っていなければ空。 */
    public Optional<UUID> groupId() {
        return this.entityData.get(DATA_GROUP);
    }

    /** 群管理の群を変える（null なら群から外す）。サーバーで呼ぶ。 */
    public void setGroupId(@Nullable UUID id) {
        this.entityData.set(DATA_GROUP, Optional.ofNullable(id));
    }

    /**
     * 群管理で、位置 floorY の乗場呼びの割り当てを考えるときの様子。止まる予定の階は、かご呼びと乗場呼びのある階。
     * その階に止まらないか、休止中なら、応えられない。
     */
    public GroupDispatcher.CarState groupState(double floorY) {
        List<Double> stops = new ArrayList<>();
        if (control != null) {
            for (int i = 0; i < floors.length; i++) {
                if (control.hasCarCall(i) || control.hasHallCall(i, TravelDirection.UP) || control.hasHallCall(i, TravelDirection.DOWN)) {
                    stops.add((double) floors[i]);
                }
            }
        }
        boolean available = control != null && !resting && floorIndexAt(floorY) >= 0;
        TravelDirection direction = control != null ? control.direction() : TravelDirection.NONE;
        return new GroupDispatcher.CarState(yAt(level().getGameTime()), direction, speed.limits().maxVelocity(),
                door.isClosed() ? 0 : OPEN_DOOR_SECONDS, stops, available);
    }

    /**
     * 群管理で、停止階 floor の乗場呼びをこのかごに割り当てたことを、その階の乗場で、ホールランタンの位置から知らせる（即時予報）。
     * 音は上りも下りも同じ G#5 の単音。到着チャイムの設定がオンのときだけ鳴らす。
     */
    public void announceAssignment(int floor) {
        if (!announcements.chime() || resting) {
            return;
        }
        Vec3 position = lanternCenter(floor);
        level().playSound(null, position.x, position.y, position.z, ModSounds.CHIME_FORECAST.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

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

    /** ボタンの並びは、正面の操作盤、奥の操作盤（二方向出入口）、車椅子用操作盤の順。 */
    private void rebuildButtons() {
        List<Button> front = ButtonLayout.carPanel(spec(), floors.length, fixtures.panelSides(), fixtures.openExtension());
        frontButtonCount = front.size();
        List<Button> all = new ArrayList<>(front);
        if (through) {
            // 奥の出入口の袖壁にも、同じ操作盤を180°回して置く
            for (Button button : front) {
                all.add(button.rotateQuarterTurns(Entrance.REAR.quarterTurns()));
            }
        }
        if (fixtures.wheelchairPanel()) {
            all.addAll(ButtonLayout.wheelchairPanel(spec(), floors.length));
        }
        this.buttons = List.copyOf(all);
        litPressTicks.clear();
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
        builder.define(DATA_INTERIOR, new CompoundTag());
        builder.define(DATA_GROUP, Optional.empty());
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

    /** かご内装。 */
    public InteriorConfig interior() {
        return interior;
    }

    /** かご内装を変える。サーバーで呼ぶ。 */
    public void setInterior(InteriorConfig interior) {
        this.interior = interior;
        this.entityData.set(DATA_INTERIOR, ConfigTags.writeInterior(interior));
    }

    /** 操作盤と階床表示器の配置を変える。停止階の間隔で付けられない配置は直す。サーバーで呼ぶ。 */
    public void setFixtures(FixtureConfig fixtures) {
        this.fixtures = ShaftLayout.fitFixtures(fixtures, floors);
        this.entityData.set(DATA_FIXTURES, ConfigTags.writeFixtures(this.fixtures));
        rebuildButtons();
        if (!this.fixtures.openExtension() && door.isExtended()) {
            // 延長中に開延長ボタンを外したら、延長をやめる
            door.cancelExtension();
            syncDoor();
        }
    }

    public boolean isMoving() {
        return trip != null;
    }

    /** ランディングオープンにするか。サーバーで呼ぶ。 */
    public void setLandingOpen(boolean landingOpen) {
        this.landingOpen = landingOpen;
    }

    public boolean landingOpen() {
        return landingOpen;
    }

    /**
     * 呼びや開ボタンについて、止まっているとみなすか。着床しているときと、ランディングオープンで行先に着いた扱いになった
     * （戸が開き始めた）あとの着床までのあいだ。
     */
    private boolean isStoppedForCalls() {
        return trip == null || landingOpened;
    }

    /**
     * 乗場の扉が、床面のY座標 y の階でかごの戸と一緒に動くか。着床しているとき（ランディングオープンでは、
     * 着床の直前に戸が開き始めたときから）その階の扉だけが動く。クライアントでも、同期した走行情報と戸の状態から決まる。
     */
    public boolean isAtLanding(double y) {
        if (trip == null) {
            return getY() == y;
        }
        return trip.to() == y && door.state() != DoorState.CLOSED;
    }

    public DoorState doorState() {
        return door.state();
    }

    /** 今の開放待機の長さ [tick]。 */
    public int doorDwellTicks() {
        return door.dwellTicks();
    }

    /** 車椅子用のボタンで開いたので、ゆっくり閉じるか。 */
    public boolean isDoorSlowClose() {
        return door.isSlowClose();
    }

    // ---- 呼びとボタン ----

    /** 正面の操作盤のかご呼び。 */
    public PressResult pressCarCall(int floor) {
        return pressCarCall(floor, Entrance.FRONT);
    }

    /** かご呼び（行先ボタン）。side は押した操作盤の側。かごがその階に止まっていれば、その側の戸を開く。 */
    public PressResult pressCarCall(int floor, Entrance side) {
        return pressCarCall(floor, side, false);
    }

    /** 車椅子用操作盤のかご呼び。二方向出入口では、最後に開いた扉の反対側の扉を開く（車椅子はかごの中で向きを変えられない）。 */
    public PressResult pressWheelchairCarCall(int floor) {
        return pressCarCall(floor, wheelchairSide(), true);
    }

    /** 車椅子用操作盤から呼んだときに開く扉の側。最後に開いた扉の反対側。 */
    private Entrance wheelchairSide() {
        if (!through) {
            return Entrance.FRONT;
        }
        Entrance last = openSides.isEmpty() ? Entrance.FRONT : openSides.iterator().next();
        return last == Entrance.FRONT ? Entrance.REAR : Entrance.FRONT;
    }

    /**
     * かご呼び。side は押した操作盤の側。かごがその階に止まっていれば、その側の戸を開く。
     *
     * @param wheelchair 車椅子用操作盤で押したか（着いたら開放待機を延ばし、ゆっくり閉じる）
     */
    private PressResult pressCarCall(int floor, Entrance side, boolean wheelchair) {
        if (level().isClientSide || control == null || resting) {
            return PressResult.INVALID;
        }
        PressResult result = control.pressCar(floor, currentFloor, isStoppedForCalls());
        if (result == PressResult.REGISTERED || result == PressResult.ALREADY_REGISTERED) {
            entranceCalls.pressCar(floor, side);
            if (wheelchair) {
                wheelchairCalls.pressCar(floor);
            }
            if (result == PressResult.ALREADY_REGISTERED) {
                // 反対側の操作盤ですでに登録されていても、この側の応答灯のために送り直す
                syncCalls();
            }
        }
        handlePress(result, CallType.CAR, side, wheelchair);
        return result;
    }

    /**
     * 行先ボタンの2度押しキャンセル。floor のかご呼びを取り消す。
     *
     * <p>二方向出入口で side を指定したときは、その側の操作盤の呼びだけを取り消し、反対側の操作盤でも登録されていれば、
     * かごはその階に止まる。走行中に今向かっている階の呼びがなくなったら、行先を決め直す（{@link CancelReplan}）。
     *
     * @param side 押した操作盤の側。null ならどちらの側の呼びも取り消す（一方向出入口、車椅子用操作盤）
     * @return 取り消したか
     */
    public boolean cancelCarCall(int floor, @Nullable Entrance side) {
        if (level().isClientSide || control == null || !control.hasCarCall(floor)) {
            return false;
        }
        if (side != null) {
            entranceCalls.cancelCar(floor, side);
            Entrance other = side == Entrance.FRONT ? Entrance.REAR : Entrance.FRONT;
            if (entranceCalls.isCarRegistered(floor, other)) {
                syncCalls();
                return true;
            }
        } else {
            for (Entrance each : Entrance.values()) {
                entranceCalls.cancelCar(floor, each);
            }
        }
        wheelchairCalls.cancelCar(floor);
        control.cancelCar(floor, isStoppedForCalls());
        if (!isStoppedForCalls() && floor == targetFloor) {
            replanAfterCancel(level().getGameTime());
        }
        syncCalls();
        return true;
    }

    /** 走行中に、今向かっている階の呼びが取り消された。延ばせれば先の呼びへ延ばし、だめならどこかに止まって戸を開けない。 */
    private void replanAfterCancel(long now) {
        CancelReplan.Outcome outcome = CancelReplan.replan(control, trip, now, floors, currentFloor, targetFloor);
        boolean changed = outcome.targetFloor() != targetFloor || !outcome.openDoors();
        if (outcome.trip() != trip) {
            setTrip(outcome.trip());
        }
        targetFloor = outcome.targetFloor();
        skipDoorsOnArrival = !outcome.openDoors();
        if (changed && arrivalAnnounced) {
            // 取り消した階の到着チャイムとランタンはやめる（行先を延ばしたら、その階で鳴らし直す）
            arrivalAnnounced = false;
            announcedFloor = -1;
            syncDoor();
        }
    }

    /**
     * 応答灯の点いた行先ボタン index が、素早く2回続けて押されたかを調べる。2回目なら呼びを取り消して true を返す。
     */
    private boolean cancelIfDoublePressed(int index) {
        if (level().isClientSide) {
            return false;
        }
        if (!isButtonLit(index)) {
            litPressTicks.remove(index);
            return false;
        }
        long now = level().getGameTime();
        Long last = litPressTicks.get(index);
        if (last != null && now - last <= OperationTiming.DOUBLE_PRESS_TICKS) {
            litPressTicks.remove(index);
            Button button = buttons.get(index);
            cancelCarCall(button.floor(), through && !button.wheelchair() ? buttonSide(index) : null);
            return true;
        }
        litPressTicks.put(index, now);
        return false;
    }

    /** 正面の乗場の乗場呼び。 */
    public PressResult pressHallCall(int floor, TravelDirection direction) {
        return pressHallCall(floor, direction, Entrance.FRONT);
    }

    /** 乗場呼び（▲▼ボタン）。side はボタンを押した乗場の側。かごがその階に止まっていて、その方向へ進めるなら、その側の戸を開く。 */
    public PressResult pressHallCall(int floor, TravelDirection direction, Entrance side) {
        return pressHallCall(floor, direction, side, false);
    }

    /**
     * 乗場呼び。
     *
     * @param wheelchair 乗場の車椅子用ボタンで押したか（応えたときに開放待機を延ばし、ゆっくり閉じる）
     */
    public PressResult pressHallCall(int floor, TravelDirection direction, Entrance side, boolean wheelchair) {
        if (level().isClientSide || control == null || resting) {
            return PressResult.INVALID;
        }
        PressResult result = control.pressHall(floor, direction, currentFloor, isStoppedForCalls());
        if (result == PressResult.REGISTERED || result == PressResult.ALREADY_REGISTERED) {
            entranceCalls.press(floor, direction, side);
            if (wheelchair) {
                wheelchairCalls.press(floor, direction);
            }
            if (result == PressResult.ALREADY_REGISTERED) {
                // 反対側の乗場や普通のボタンですでに登録されていても、この側・車椅子用の応答灯のために送り直す
                syncCalls();
            }
        }
        handlePress(result, CallType.HALL, side, wheelchair);
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
    private boolean requestOpen(Entrance side, CallType callType, boolean wheelchair) {
        Entrance target = doorFor(side);
        if (door.isNudging() && openSides.contains(target)) {
            // ナッジング中は、閉めている扉を開く操作を受け付けない
            return false;
        }
        if (!door.isClosed() && !openSides.contains(target)) {
            pendingSides.add(target);
            pendingWheelchair |= wheelchair;
            return false;
        }
        if (door.isClosed()) {
            openSides.clear();
            openSides.add(target);
        }
        openDoor(callType, wheelchair);
        return true;
    }

    /**
     * @param side       その場で応えるときに開く扉の側（押した操作盤や乗場の側）
     * @param wheelchair 車椅子用のボタンで押したか
     */
    private void handlePress(PressResult result, CallType callType, Entrance side, boolean wheelchair) {
        if (result == PressResult.SERVED_HERE && door.isNudging() && openSides.contains(doorFor(side))) {
            // ナッジングで閉めている扉は開かないので、チャイムも鳴らさない
            syncCalls();
            return;
        }
        if (result == PressResult.SERVED_HERE) {
            if (callType == CallType.HALL) {
                lanternTick = level().getGameTime();
                playArrivalChime(currentFloor, control != null ? control.direction() : TravelDirection.NONE);
            }
            boolean alreadyOpen = door.state() == DoorState.OPEN;
            if (!requestOpen(side, callType, wheelchair)) {
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
        if (level().isClientSide || !isStoppedForCalls()) {
            return false;
        }
        requestOpen(side, CallType.CAR, false);
        return true;
    }

    /** 車椅子用操作盤の開ボタン。今（または最後に）開いた側の扉を開き、開放待機を延ばしてゆっくり閉じる。走行中は効かない。 */
    public boolean openDoorForWheelchair() {
        if (level().isClientSide || !isStoppedForCalls()) {
            return false;
        }
        requestOpen(openSides.isEmpty() ? Entrance.FRONT : openSides.iterator().next(), CallType.CAR, true);
        return true;
    }

    /** 開ボタン。走行中は効かない。閉まっているときは、最後に開いた側の扉を開く。 */
    public boolean openDoor(CallType callType) {
        return openDoor(callType, false);
    }

    /**
     * 戸を開く。走行中は効かない。閉まっているときは、最後に開いた側の扉を開く。
     *
     * @param wheelchair 車椅子用のボタンで開いたか（開放待機を延ばし、ゆっくり閉じる）
     */
    private boolean openDoor(CallType callType, boolean wheelchair) {
        if (level().isClientSide || !isStoppedForCalls()) {
            return false;
        }
        Set<Entrance> available = entrancesAt(currentFloor);
        if (openSides.isEmpty() || !available.containsAll(openSides)) {
            openSides.clear();
            openSides.add(available.iterator().next());
        }
        door.open(level().getGameTime(), door.timing().dwellTicks(callType, wheelchair), wheelchair);
        syncDoor();
        return true;
    }

    /** 側 side の操作盤の開延長ボタン。戸を開き、全開になってから60秒開けておく。走行中は効かない。 */
    public boolean extendDoor(Entrance side) {
        if (level().isClientSide || !isStoppedForCalls() || !requestOpen(side, CallType.CAR, false)) {
            return false;
        }
        door.extend(level().getGameTime());
        syncDoor();
        return door.isExtended();
    }

    /** 到着チャイムが鳴り終わるゲーム時刻（動作確認のテスト用）。 */
    public long chimeEndTick() {
        return chimeEndTick;
    }

    public boolean isDoorExtended() {
        return door.isExtended();
    }

    /** 閉ボタン。開放待機中なら、すぐに閉じ始める。開延長は解除する。 */
    public boolean closeDoor() {
        if (level().isClientSide) {
            return false;
        }
        if (door.state() != DoorState.OPEN) {
            if (door.isExtended()) {
                door.cancelExtension();
                syncDoor();
            }
            return false;
        }
        // 閉ボタンでは、戸閉アナウンスを流しながらすぐに閉め始める。行先方向や、自動で閉めるときの戸閉アナウンスを
        // 流している間は、新しく流さない（別のアナウンスが流れていれば流し直さない）
        if (announcements.doorClose() && !isSpeakingDirection(level().getGameTime()) && !door.isWaitingToClose()) {
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

    /** 側 side の乗場の、車椅子用ボタンの応答灯。車椅子用ボタンで押された呼びだけ点く。 */
    public boolean isWheelchairHallCallLit(int floor, TravelDirection direction, Entrance side) {
        return floor >= 0 && floor < floors.length && wheelchairCalls.isRegistered(floor, direction) && isHallCallLit(floor, direction, side);
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
        if (button.kind() == ButtonLayout.Kind.OPEN_EXTEND) {
            return door.isExtended();
        }
        if (button.kind() != ButtonLayout.Kind.CAR_CALL) {
            return false;
        }
        if (button.wheelchair()) {
            // 車椅子用操作盤は、車椅子用操作盤で押した呼びだけ点く（普通の操作盤は、どちらで押しても点く）
            return isCarCallLit(button.floor()) && wheelchairCalls.isCarRegistered(button.floor());
        }
        // 二方向出入口では、押した側の操作盤の行先ボタンだけ点く
        return through ? entranceCalls.isCarRegistered(button.floor(), buttonSide(index)) : isCarCallLit(button.floor());
    }

    @Override
    public String buttonLabel(int index) {
        Button button = buttons.get(index);
        return button.kind() == ButtonLayout.Kind.CAR_CALL ? floorLabel(button.floor()) : "";
    }

    /** 非常呼びボタン。飾りで、押すと短い音が鳴るだけ。 */
    private void pressEmergency(Button button) {
        Box box = button.box().rotateQuarterTurns(facing().get2DDataValue());
        level().playSound(null, getX() + (box.minX() + box.maxX()) / 2, getY() + (box.minY() + box.maxY()) / 2,
                getZ() + (box.minZ() + box.maxZ()) / 2, ModSounds.EMERGENCY_BEEP.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    public void pressButton(int index) {
        Button button = buttons.get(index);
        if (button.wheelchair()) {
            switch (button.kind()) {
                case OPEN -> openDoorForWheelchair();
                case CLOSE -> closeDoor();
                case CAR_CALL -> {
                    if (!cancelIfDoublePressed(index)) {
                        pressWheelchairCarCall(button.floor());
                    }
                }
                case EMERGENCY -> pressEmergency(button);
                case OPEN_EXTEND, HALL_UP, HALL_DOWN -> {
                }
            }
            return;
        }
        Entrance side = buttonSide(index);
        switch (button.kind()) {
            case OPEN -> openDoor(side);
            case CLOSE -> closeDoor();
            case OPEN_EXTEND -> extendDoor(side);
            case CAR_CALL -> {
                if (!cancelIfDoublePressed(index)) {
                    pressCarCall(button.floor(), side);
                }
            }
            case EMERGENCY -> pressEmergency(button);
            case HALL_UP, HALL_DOWN -> {
            }
        }
    }

    // ---- tick ----

    @Override
    public void tick() {
        super.tick();
        // クライアントでは、ゲーム時刻の巻き戻りで跳ねないよう、なめらかに進む時計で位置を求める
        double clock = ElevatorClock.now(level());
        lastTick = clock;
        if (trip != null) {
            setPos(getX(), trip.positionAt(clock), getZ());
            lastTravelClock = clock;
        }
        // 誰も乗っていないtickでも、運搬の基準の位置を進めておく
        carryDelta(clock);
        if (!level().isClientSide) {
            serverTick(level().getGameTime());
        }
    }

    private void serverTick(long now) {
        updateOperation(now);
        // 到着したtickの最後の移動量も乗っているエンティティに加えられるよう、走行情報は1tick遅れて消す
        if (trip != null && trip.isArrivedAt(now - 1)) {
            setTrip(null);
            currentFloor = targetFloor;
            if (landingOpened) {
                // ランディングオープンで、着いた扱いはもう済んでいる
                landingOpened = false;
            } else if (resting) {
                // 休止時の自動着床：最寄りの階に着いたら、乗客が降りるまで戸を開け、そのあと閉めて休止する
                openForRest(now);
            } else if (skipDoorsOnArrival && (control == null || !control.hasAnyCallAt(currentFloor))) {
                // 取り消された行先に止まった。戸は開けずに、次の呼びへ向かう（なければここで待つ）
                arrivalAnnounced = false;
                announcedFloor = -1;
                syncDoor();
                syncCalls();
            } else {
                serveCurrentFloor(now);
            }
            skipDoorsOnArrival = false;
        } else if (trip != null && !landingOpened) {
            stopOnTheWayIfPossible(now);
            announceArrivalIfCertain(now);
            openBeforeLandingIfEnabled(now);
        }

        // 行先方向を流している間に自動で閉めるときは、戸閉アナウンスを流さず（待たず）に閉める
        door.setCloseAnnouncementTicks(announcements.doorClose() && !isSpeakingDirection(now) && !resting
                ? AnnouncementScript.DOOR_CLOSE_WAIT_TICKS : 0);
        if (now % SNEAK_OPEN_INTERVAL_TICKS == 0 && isStoppedForCalls() && hasSneakingRider()) {
            // かご内でしゃがんでいるあいだは、開ボタンを押しているものとして扱う
            openDoor(CallType.CAR);
        }
        if (door.state() == DoorState.OPEN && isBeamBlocked()) {
            // 開ききっているあいだにビームが人を検知していたら、待機時間を数え直す（戸閉アナウンスも流さない）
            door.hold(now);
        }
        DoorController.Event doorEvent = door.tick(now);
        if (doorEvent == DoorController.Event.CLOSE_ANNOUNCED) {
            // 開放待機が終わった。戸閉アナウンスを流し終わってから閉め始める
            announce(Announcement.doorClose());
        }
        boolean doorChanged = doorEvent == DoorController.Event.STATE_CHANGED || doorEvent == DoorController.Event.NUDGING_STARTED;
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
        soundNudgingBuzzer(now);

        if (trip == null && door.isClosed() && !pendingSides.isEmpty()) {
            // 扉が開いているあいだに反対側で呼ばれていたら、発車せずにその側を開く
            Entrance next = pendingSides.iterator().next();
            pendingSides.remove(next);
            openSides.clear();
            openSides.add(next);
            // 到着したときに応えた呼びのうち、この側のものの応答灯を消す
            entranceCalls.served(currentFloor, next, pendingCarCall, pendingHallDirection, entrancesAt(currentFloor));
            boolean wheelchair = pendingWheelchair;
            if (pendingSides.isEmpty()) {
                pendingCarCall = false;
                pendingHallDirection = TravelDirection.NONE;
                pendingWheelchair = false;
            }
            syncCalls();
            // 反対側の扉を開くときも、到着チャイムを鳴らしてランタンを点滅させ直す
            lanternTick = now;
            playArrivalChime(currentFloor, control != null ? control.direction() : TravelDirection.NONE);
            door.open(now, door.timing().dwellTicks(CallType.HALL, wheelchair), wheelchair);
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
     * ランディングオープン：着床の {@value OperationTiming#LANDING_OPEN_TICKS} tick前になったら、その時点で行先に着いた扱いにする
     * （呼びの記録を消し、チャイムを鳴らし、戸を開き始める）。かごは運転曲線どおりに着床まで動き続ける。
     * 休止時の自動着床と、取り消した行先に戸を開けずに止まるときは使わない。戸を開き始める時刻は到着tickから決まるので、同期は増やさない。
     */
    private void openBeforeLandingIfEnabled(long now) {
        if (!landingOpen || trip == null || resting || now < trip.arrivalTick() - OperationTiming.LANDING_OPEN_TICKS
                || (skipDoorsOnArrival && (control == null || !control.hasAnyCallAt(targetFloor)))) {
            return;
        }
        landingOpened = true;
        currentFloor = targetFloor;
        skipDoorsOnArrival = false;
        serveCurrentFloor(now);
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
                skipDoorsOnArrival = false;
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
        // 車椅子用のボタンで登録された呼びに応えたら、開放待機を延ばし、ゆっくり閉じる
        boolean wheelchair = wheelchairCalls.arrive(currentFloor, carCall, hallDirection);
        pendingWheelchair = wheelchair && !pendingSides.isEmpty();
        if (!arrivalAnnounced) {
            lanternTick = now;
            playArrivalChime(currentFloor, control != null ? control.direction() : TravelDirection.NONE);
        }
        arrivalAnnounced = false;
        announcedFloor = -1;
        door.open(now, door.timing().dwellTicks(served, wheelchair), wheelchair);
        syncDoor();
        syncCalls();
        spokenDirection = TravelDirection.NONE;
        // 戸を開き始めるときに「◯階です。」を流し（戸はアナウンスを待たない）、開ききったら行先方向を流す
        if (announcements.arrival()) {
            announce(Announcement.arrival(floorLabel(currentFloor), floorSpeechName(currentFloor)));
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
        if (!announcements.arrival() || control == null || !isStoppedForCalls() || directionAnnouncementPending
                || door.state() != DoorState.OPEN || door.isWaitingToClose()) {
            return;
        }
        TravelDirection direction = control.direction();
        if (direction != TravelDirection.NONE && direction != spokenDirection) {
            announceDirection();
        }
    }

    /** このかごが見えているクライアントへアナウンスを送る。鳴らし方はクライアントに任せる。休止中は流さない。 */
    private void announce(Announcement announcement) {
        if (resting) {
            return;
        }
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
        if (arrivalAnnounced || trip == null || control == null || resting
                || (skipDoorsOnArrival && !control.hasAnyCallAt(targetFloor))) {
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

    /** かごの中（床から天井までの内側）に、しゃがんでいるプレイヤーがいるか。 */
    private boolean hasSneakingRider() {
        AABB inside = CarShapes.toAabb(CarGeometry.interior(spec()).rotateQuarterTurns(facing().get2DDataValue()), getX(), getY(), getZ());
        return !level().getEntitiesOfClass(Player.class, inside,
                player -> player.isShiftKeyDown() && !player.isSpectator() && inside.contains(player.position().add(0, 0.01, 0))).isEmpty();
    }

    /**
     * 設定で有効なら、停止階 floor の乗場で、ホールランタンの位置から到着チャイムを鳴らす。下りは C6→G#5、それ以外は G#5→C6。
     * 同じ階のチャイムが鳴っているあいだは、重ねて鳴らさない。
     */
    private void playArrivalChime(int floor, TravelDirection direction) {
        if (!announcements.chime() || resting) {
            return;
        }
        long now = level().getGameTime();
        if (floor == chimeFloor && now < chimeEndTick) {
            return;
        }
        chimeFloor = floor;
        chimeEndTick = now + CHIME_TICKS;
        Vec3 position = lanternCenter(floor);
        SoundEvent sound = (direction == TravelDirection.DOWN ? ModSounds.CHIME_DOWN : ModSounds.CHIME_UP).get();
        level().playSound(null, position.x, position.y, position.z, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** 停止階 floor のホールランタンの中心（ワールド座標）。 */
    private Vec3 lanternCenter(int floor) {
        AABB bounds = null;
        for (HallLantern.Section section : landingLayout(0).lantern(isTerminalFloor(floor))) {
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
        // プレビューは点滅の点灯中のまま見せる
        return preview ? 0 : tick - lanternTick;
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
        if (preview) {
            return HallLantern.litSections(floor, floors.length, TravelDirection.UP, true);
        }
        if (resting) {
            return Set.of();
        }
        if (trip != null && !isAtLanding(trip.to())) {
            if (floor != announcedFloor || !lanternSides(floor).contains(side)) {
                return Set.of();
            }
            return HallLantern.litSections(floor, floors.length, announcedDirection, true);
        }
        // 着床している（ランディングオープンで戸が開き始めた）階
        double here = trip != null ? trip.to() : getY();
        boolean doorOpenHere = floorIndexAt(here) == floor && door.state() != DoorState.CLOSED && openSides.contains(side);
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

    /**
     * 閉まりかけの戸を止めるものがあるかどうか。セーフティシュー（閉まりかけの戸板に生き物が触れている）か、
     * 多光軸ビームセンサー（かごの戸と乗場の戸の間の検知領域に生き物が入っている）。どちらも開いている側の出入口だけを見る。
     */
    private boolean isDoorObstructed(long now) {
        double fraction = door.fractionAt(now);
        List<AABB> panels = new ArrayList<>(10);
        for (Entrance entrance : openSides) {
            for (DoorGeometry.Side side : DoorGeometry.Side.values()) {
                for (Box box : CarShapes.doorPanels(spec(), facing(), side, fraction, entrance)) {
                    panels.add(CarShapes.toAabb(box, getX(), getY(), getZ()).inflate(SAFETY_SHOE_MARGIN));
                }
            }
            if (!door.isNudging()) {
                // ナッジング中は多光軸ビームを使わず、セーフティシューだけで止める
                panels.add(CarShapes.toAabb(CarShapes.beamZone(spec(), facing(), entrance), getX(), getY(), getZ()));
            }
        }
        if (panels.isEmpty()) {
            return false;
        }
        AABB search = panels.stream().reduce(AABB::minmax).orElseThrow();
        return !level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> !entity.isSpectator() && panels.stream().anyMatch(entity.getBoundingBox()::intersects)).isEmpty();
    }

    // ---- 運転と休止 ----

    /** 運転切替盤 pos につなぐ。サーバーで呼ぶ。 */
    public void linkPowerReceiver(BlockPos pos) {
        this.powerReceiverPos = pos.immutable();
    }

    /** 運転切替盤とのつなぎを外す。常に通常運転の扱いに戻る。サーバーで呼ぶ。 */
    public void unlinkPowerReceiver() {
        this.powerReceiverPos = null;
    }

    /** つないでいる運転切替盤の位置。つないでいなければ null。 */
    @Nullable
    public BlockPos powerReceiverPos() {
        return powerReceiverPos;
    }

    /** 休止中か。 */
    public boolean isResting() {
        return resting;
    }

    /**
     * 運転切替盤の信号を見て、通常運転と休止を切り替える。運転切替盤が読み込まれていなければ今のまま。運転切替盤がなくなっていたり、
     * ほかのエレベーターにつなぎ直されていたりしたら、つなぎを外して通常運転の扱いに戻す。
     */
    private void updateOperation(long now) {
        boolean powered = true;
        if (powerReceiverPos != null) {
            if (!level().isLoaded(powerReceiverPos)) {
                return;
            }
            BlockState state = level().getBlockState(powerReceiverPos);
            if (state.getBlock() instanceof PowerReceiverBlock
                    && level().getBlockEntity(powerReceiverPos) instanceof PowerReceiverBlockEntity receiver
                    && getUUID().equals(receiver.car())) {
                powered = state.getValue(PowerReceiverBlock.POWERED);
            } else {
                powerReceiverPos = null;
            }
        }
        if (powered == resting) {
            if (powered) {
                // 通常運転に戻る。呼びは休止で消えているので、押し直すと動く
                resting = false;
                syncCalls();
            } else {
                enterRest(now);
            }
        }
    }

    /**
     * 休止。呼びをすべて消し、ボタンのランプとホールランタンを消し、表示器をハイフンにする。走行中なら減速が間に合う最寄りの階に止まり（休止時の自動着床）、
     * 止まっていればその階で、乗客が降りるまで戸を開けてから閉めて休止する。
     */
    private void enterRest(long now) {
        resting = true;
        if (control != null) {
            control.clearAll();
        }
        entranceCalls.restore(new long[0][]);
        wheelchairCalls.restore(new long[0], new long[0], new long[0]);
        pendingSides.clear();
        pendingCarCall = false;
        pendingHallDirection = TravelDirection.NONE;
        pendingWheelchair = false;
        litPressTicks.clear();
        departureTick = -1;
        skipDoorsOnArrival = false;
        directionAnnouncementPending = false;
        arrivalAnnounced = false;
        announcedFloor = -1;
        if (trip != null && !landingOpened) {
            CancelReplan.Outcome outcome = CancelReplan.nearestStop(trip, now, floors, currentFloor, targetFloor);
            if (outcome.trip() != trip) {
                setTrip(outcome.trip());
            }
            targetFloor = outcome.targetFloor();
        } else {
            openForRest(now);
        }
        syncDoor();
        syncCalls();
    }

    /** 休止で着床した（または止まっていた）階で、乗客が降りるまで戸を開ける。そのあとは普通に閉まって休止する。 */
    private void openForRest(long now) {
        door.cancelExtension();
        Set<Entrance> available = entrancesAt(currentFloor);
        if (openSides.isEmpty() || !available.containsAll(openSides)) {
            openSides.clear();
            openSides.add(available.iterator().next());
        }
        door.open(now, OperationTiming.REST_DOOR_OPEN_TICKS);
        syncDoor();
    }

    /** ナッジング中は、閉まりきるまでかごの中でブザーを断続的に鳴らす。 */
    private void soundNudgingBuzzer(long now) {
        if (!door.isNudging() || door.isClosed()) {
            nextBuzzTick = Long.MIN_VALUE;
            return;
        }
        if (now >= nextBuzzTick) {
            level().playSound(null, getX(), getY() + BUZZER_HEIGHT, getZ(), ModSounds.NUDGE_BUZZER.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            nextBuzzTick = now + BUZZER_INTERVAL_TICKS;
        }
    }

    /** ナッジング（強制戸閉）中か。 */
    public boolean isNudging() {
        return door.isNudging();
    }

    /** 多光軸ビームセンサー：開いている側の出入口の、かごの戸と乗場の戸の間に生き物がいるかどうか。 */
    private boolean isBeamBlocked() {
        List<AABB> zones = new ArrayList<>(2);
        for (Entrance entrance : openSides) {
            zones.add(CarShapes.toAabb(CarShapes.beamZone(spec(), facing(), entrance), getX(), getY(), getZ()));
        }
        if (zones.isEmpty()) {
            return false;
        }
        AABB search = zones.stream().reduce(AABB::minmax).orElseThrow();
        return !level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> !entity.isSpectator() && zones.stream().anyMatch(entity.getBoundingBox()::intersects)).isEmpty();
    }

    // ---- 階床表示 ----

    /** 設定画面のプレビューで描くだけのかごにする。 */
    public void setPreview(boolean preview) {
        this.preview = preview;
    }

    public boolean isPreview() {
        return preview;
    }

    /** 停止階に3文字の階名があるか（ドットで描くとき、表示器を3文字分に広げる。フォントで描くときは縮めるので広げない）。 */
    public boolean hasLongLabels() {
        if (fixtures.indicatorStyle() == FixtureConfig.IndicatorStyle.FONT) {
            return false;
        }
        for (int i = 0; i < floors.length; i++) {
            if (FloorIndicator.hasLongLabel(floorLabel(i))) {
                return true;
            }
        }
        return false;
    }

    /** 乗場の配置（乗場の面を faceOffset だけずらしたもの）。 */
    public LandingLayout landingLayout(double faceOffset) {
        return LandingLayout.of(spec(), fixtures, interior.landing(), faceOffset, hasLongLabels());
    }

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

    /** 停止階 floor の読み上げ名。なければ空。 */
    public String floorSpeechName(int floor) {
        return floor >= 0 && floor < speechNames.length ? speechNames[floor] : "";
    }

    /** 停止階の読み上げ名を変える（下から順）。サーバーで呼ぶ。 */
    public void setSpeechNames(String[] names) {
        this.speechNames = Arrays.copyOf(names, floors.length);
        for (int i = 0; i < this.speechNames.length; i++) {
            if (this.speechNames[i] == null) {
                this.speechNames[i] = "";
            }
        }
        CompoundTag tag = this.entityData.get(DATA_FLOORS).copy();
        tag.put("SpeechNames", writeLabels(this.speechNames));
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
        return readStrings(tag, "Labels");
    }

    private static String[] readStrings(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, Tag.TAG_STRING);
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
        if (resting) {
            // 休止中は表示器をハイフンにする
            return FloorIndicator.BETWEEN_FLOORS;
        }
        return floors.length > 0 ? FloorIndicator.displayedLabel(yAt(tick), floors, this::floorLabel) : floorLabel(0);
    }

    /** 階床表示器に出す矢印の向き。走行中はその向き、停止中は次に進む方向。 */
    public TravelDirection indicatorDirection() {
        if (preview) {
            return TravelDirection.UP;
        }
        if (resting) {
            return TravelDirection.NONE;
        }
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
     * 時刻 now（{@link ElevatorClock}）のtickに、かごが前のtickから実際に動いた量。乗っているエンティティの運搬に使う。
     *
     * <p>時刻が飛んだり戻ったりしても合うよう、「1tick分の移動量」ではなく、前に評価したtickの位置からの差を返す。
     * 同じtickに何度呼んでも同じ値になり、かごとエンティティのどちらが先にtickしても変わらない。
     */
    public double carryDelta(double now) {
        if (now != carryTick) {
            carryFromY = Double.isNaN(carryTick) ? yAt(now) : carryToY;
            carryToY = yAt(now);
            carryTick = now;
        }
        return carryToY - carryFromY;
    }

    /** {@link #carryDelta} の基準にした、前のtickでのかごの床面のY座標。 */
    public double carryFromY(double now) {
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
    public List<AABB> collisionBoxesAt(double tick) {
        double y = yAt(tick);
        List<AABB> boxes = new ArrayList<>();
        for (Part part : parts()) {
            boxes.add(CarShapes.toAabb(part.box(), getX(), y, getZ()));
        }
        // かご上の安全柵（ウィザードで設置したかごだけ）
        HoistMechanism hoist = hoist();
        if (hoist != null) {
            for (Box fence : hoist.carFrame().fence()) {
                boxes.add(CarShapes.toAabb(fence.rotateQuarterTurns(facing().get2DDataValue()), getX(), y, getZ()));
            }
        }
        // カーペットはバニラと同じく1pxの段になる
        if (interior.carpet() != InteriorConfig.Carpet.NONE) {
            boxes.add(CarShapes.toAabb(InteriorGeometry.carpet(spec()).rotateQuarterTurns(facing().get2DDataValue()), getX(), y, getZ()));
        }
        for (Entrance entrance : entrances()) {
            for (Box panel : CarShapes.doorPanels(spec(), facing(), DoorGeometry.Side.CAR, doorFractionAt(tick, entrance), entrance)) {
                boxes.add(CarShapes.toAabb(panel, getX(), y, getZ()));
            }
        }
        return boxes;
    }

    public List<Part> parts() {
        return CarShapes.parts(spec(), facing(), through, observation);
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
        return carries(entity, carY, carY);
    }

    /** {@link #carries(Entity, double)} の、かごの床面が lowY から highY までのどこかにあればよいもの。 */
    public boolean carries(Entity entity, double lowY, double highY) {
        Box exterior = CarShapes.exterior(spec(), facing());
        double localX = entity.getX() - getX();
        double localZ = entity.getZ() - getZ();
        return localX > exterior.minX() && localX < exterior.maxX()
                && localZ > exterior.minZ() && localZ < exterior.maxZ()
                && entity.getY() > lowY + exterior.minY() - 0.25 && entity.getY() < highY + exterior.maxY() + 0.5;
    }

    /**
     * entity の足が、ゲーム時刻 tick でのかごの床または屋根の中に入り込んでいれば、その上面のY座標を返す。
     * 入り込んでいなければ NaN。
     *
     * <p>クライアントのゲーム時刻はサーバーの時刻に合わせて数tick飛ぶことがあり、そのときかごの床は1tick分より大きく動く。
     * 運搬では1tick分の移動量しか加えないので、上昇中は床の中に取り残されて下へ抜けてしまう。それを防ぐために使う。
     */
    public double sunkSurface(Entity entity, double tick) {
        return sunkSurface(entity, tick, SINK_TOLERANCE, MAX_SINK);
    }

    /**
     * 止まっているかごの床に、entity の足がごく浅く入り込んでいれば、その上面のY座標を返す。入り込んでいなければ NaN。
     *
     * <p>エンティティがサーバーからの位置で動いていたあと（プレイヤーがボートに乗ったときなど）は、位置の丸めの分だけ床より下にいることがある。
     * 衝突判定は重なっている箱を無視するので、そのまま動かすと床をすり抜ける。
     */
    public double restingSunkSurface(Entity entity, double tick) {
        return sunkSurface(entity, tick, RESTING_SINK_TOLERANCE, RESTING_MAX_SINK);
    }

    /**
     * クライアントで、サーバーからの位置で動くエンティティ entity を、走っているかごの床に合わせるときの面のY座標。
     * かごの当たり判定の面のうち、足にいちばん近い面。遅れて見込まれる範囲に面がなければ NaN。
     *
     * <p>サーバーから届く位置は遅れて補間されるので、かごの位置を時刻から求めているクライアントでは、
     * そのままだと上昇中に沈み、下降中に浮いて見える。
     */
    public double restingSurface(Entity entity, double tick) {
        double feet = entity.getY();
        double reach = Math.abs(yAt(tick) - yAt(tick - REMOTE_LAG_TICKS)) + 0.25;
        double best = Double.NaN;
        for (AABB box : collisionBoxesAt(tick)) {
            boolean above = entity.getX() > box.minX && entity.getX() < box.maxX && entity.getZ() > box.minZ && entity.getZ() < box.maxZ;
            double distance = Math.abs(box.maxY - feet);
            if (above && distance <= reach && (Double.isNaN(best) || distance < Math.abs(best - feet))) {
                best = box.maxY;
            }
        }
        return best;
    }

    /** クライアントで、ほかのエンティティをかごの床に合わせる必要があるか（走っている、または止まった直後）。 */
    public boolean isSettling(double clock) {
        return trip != null || clock - lastTravelClock < REMOTE_SETTLE_TICKS;
    }

    private double sunkSurface(Entity entity, double tick, double minDepth, double maxDepth) {
        double y = yAt(tick);
        double feet = entity.getY();
        for (Part part : parts()) {
            if (part.type() != CarGeometry.PartType.FLOOR && part.type() != CarGeometry.PartType.CEILING) {
                continue;
            }
            AABB box = CarShapes.toAabb(part.box(), getX(), y, getZ());
            boolean above = entity.getX() > box.minX && entity.getX() < box.maxX
                    && entity.getZ() > box.minZ && entity.getZ() < box.maxZ;
            if (above && feet < box.maxY - minDepth && feet > box.maxY - maxDepth) {
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
            speechNames = readStrings(floorTag, "SpeechNames");
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
        } else if (DATA_INTERIOR.equals(key)) {
            interior = ConfigTags.readInterior(this.entityData.get(DATA_INTERIOR));
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
        if (powerReceiverPos != null) {
            tag.put("PowerReceiver", NbtUtils.writeBlockPos(powerReceiverPos));
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
        tag.putBoolean("PendingWheelchair", pendingWheelchair);
        tag.putBoolean("SkipDoors", skipDoorsOnArrival);
        tag.putBoolean("LandingOpen", landingOpen);
        tag.putBoolean("LandingOpened", landingOpened);
        if (trip != null) {
            tag.put("Trip", writeTrip(trip));
        }
        tag.put("Door", writeDoorState());
        tag.putBoolean("ArrivalAnnounced", arrivalAnnounced);
        if (control != null) {
            tag.put("Calls", writeCalls(control));
        }
        tag.put("Fixtures", ConfigTags.writeFixtures(fixtures));
        tag.put("Interior", ConfigTags.writeInterior(interior));
        groupId().ifPresent(id -> tag.putUUID("Group", id));
        tag.put("Labels", writeLabels(labels));
        tag.put("SpeechNames", writeLabels(speechNames));
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
        powerReceiverPos = NbtUtils.readBlockPos(tag, "PowerReceiver").orElse(null);
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
        pendingWheelchair = tag.getBoolean("PendingWheelchair");
        skipDoorsOnArrival = tag.getBoolean("SkipDoors");
        landingOpen = tag.getBoolean("LandingOpen");
        landingOpened = tag.getBoolean("LandingOpened");
        setup(new CarSpec(model, door), facing, tag.getIntArray("Floors"), speed);
        setFixtures(ConfigTags.readFixtures(tag.getCompound("Fixtures")));
        setInterior(ConfigTags.readInterior(tag.getCompound("Interior")));
        setGroupId(tag.hasUUID("Group") ? tag.getUUID("Group") : null);
        setLabels(readLabels(tag));
        setSpeechNames(readStrings(tag, "SpeechNames"));
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
        tag.putBoolean("Slow", door.isSlowClose());
        tag.putBoolean("Nudge", door.isNudging());
        tag.putBoolean("Extend", door.isExtended());
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
                tag.getLong("StartTick"), tag.getDouble("StartFraction"), tag.getInt("Dwell"), tag.getBoolean("Slow"),
                tag.getBoolean("Nudge"), tag.getBoolean("Extend"));
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
        long[][] wheelchair = wheelchairCalls.snapshot();
        tag.putLongArray("WheelchairCar", wheelchair[0]);
        tag.putLongArray("WheelchairUp", wheelchair[1]);
        tag.putLongArray("WheelchairDown", wheelchair[2]);
        tag.putBoolean("Resting", resting);
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
        wheelchairCalls.restore(tag.getLongArray("WheelchairCar"), tag.getLongArray("WheelchairUp"), tag.getLongArray("WheelchairDown"));
        resting = tag.getBoolean("Resting");
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

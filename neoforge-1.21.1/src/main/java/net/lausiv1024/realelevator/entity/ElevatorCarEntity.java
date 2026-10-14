package net.lausiv1024.realelevator.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.CallType;
import net.lausiv1024.realelevator.core.control.CollectiveControl;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.display.FloorIndicator;
import net.lausiv1024.realelevator.core.door.DoorController;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.door.DoorState;
import net.lausiv1024.realelevator.core.door.DoorTiming;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.motion.Trip;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

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
    private List<Button> buttons = List.of();
    private FixtureConfig fixtures = FixtureConfig.DEFAULT;
    /** 停止階が2つ以上あるときだけ作る。 */
    @Nullable
    private CollectiveControl control;
    private SpeedSpec speedSpec = SpeedSpec.NORMAL_LOW;
    @Nullable
    private Trip trip;
    private final DoorController door = new DoorController(DoorTiming.STANDARD);
    /** 今いる階（走行中は最後に止まった階）。サーバーだけが使う。 */
    private int currentFloor;
    /** 走行中の行先の階。サーバーだけが使う。 */
    private int targetFloor;
    /** 最後に位置を計算したゲーム時刻。描画の補間に使う。 */
    private long lastTick;
    /** 運搬の移動量を最後に評価したゲーム時刻と、そのときと前のtickのかごの位置。 */
    private long carryTick = Long.MIN_VALUE;
    private double carryFromY;
    private double carryToY;

    public ElevatorCarEntity(EntityType<? extends ElevatorCarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(CarModel model, Direction facing, int[] floors, SpeedSpec speedSpec) {
        this.entityData.set(DATA_MODEL, (byte) model.ordinal());
        this.entityData.set(DATA_FACING, facing);
        int[] sorted = floors.clone();
        Arrays.sort(sorted);
        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Floors", sorted);
        this.entityData.set(DATA_FLOORS, tag);
        applyFloors(sorted);
        this.speedSpec = speedSpec;
        this.currentFloor = nearestFloor(getY());
        this.targetFloor = currentFloor;
        setBoundingBox(makeBoundingBox());
    }

    private void applyFloors(int[] floors) {
        this.floors = floors;
        this.control = floors.length >= 2 ? new CollectiveControl(floors.length) : null;
        rebuildButtons();
    }

    private void rebuildButtons() {
        this.buttons = ButtonLayout.carPanel(model(), floors.length, fixtures.panelSides());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODEL, (byte) CarModel.P11.ordinal());
        builder.define(DATA_FACING, Direction.SOUTH);
        builder.define(DATA_FLOORS, new CompoundTag());
        builder.define(DATA_TRIP, new CompoundTag());
        builder.define(DATA_DOOR, new CompoundTag());
        builder.define(DATA_CALLS, new CompoundTag());
        builder.define(DATA_FIXTURES, new CompoundTag());
    }

    public CarModel model() {
        CarModel[] models = CarModel.values();
        return models[Mth.clamp(this.entityData.get(DATA_MODEL), 0, models.length - 1)];
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

    /** 操作盤と階床表示器の配置を変える。サーバーで呼ぶ。 */
    public void setFixtures(FixtureConfig fixtures) {
        this.fixtures = fixtures;
        this.entityData.set(DATA_FIXTURES, writeFixtures(fixtures));
        rebuildButtons();
    }

    public boolean isMoving() {
        return trip != null;
    }

    public DoorState doorState() {
        return door.state();
    }

    // ---- 呼びとボタン ----

    /** かご呼び（行先ボタン）。かごがその階に止まっていれば戸を開く。 */
    public PressResult pressCarCall(int floor) {
        if (level().isClientSide || control == null) {
            return PressResult.INVALID;
        }
        PressResult result = control.pressCar(floor, currentFloor, trip == null);
        handlePress(result, CallType.CAR);
        return result;
    }

    /** 乗場呼び（▲▼ボタン）。かごがその階に止まっていて、その方向へ進めるなら戸を開く。 */
    public PressResult pressHallCall(int floor, TravelDirection direction) {
        if (level().isClientSide || control == null) {
            return PressResult.INVALID;
        }
        PressResult result = control.pressHall(floor, direction, currentFloor, trip == null);
        handlePress(result, CallType.HALL);
        return result;
    }

    private void handlePress(PressResult result, CallType callType) {
        if (result == PressResult.SERVED_HERE) {
            openDoor(callType);
        }
        if (result == PressResult.SERVED_HERE || result == PressResult.REGISTERED) {
            // その場で応えた乗場呼びでも進行方向が決まるので、階床表示器の矢印のために送る
            syncCalls();
        }
    }

    /** 開ボタン。走行中は効かない。 */
    public boolean openDoor(CallType callType) {
        if (level().isClientSide || trip != null) {
            return false;
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

    @Override
    public List<Button> buttons() {
        return buttons;
    }

    @Override
    public AABB buttonBox(int index) {
        Box box = buttons.get(index).box().rotateQuarterTurns(facing().get2DDataValue());
        return CarShapes.toAabb(box, getX(), getY(), getZ());
    }

    @Override
    public boolean isButtonLit(int index) {
        Button button = buttons.get(index);
        return button.kind() == ButtonLayout.Kind.CAR_CALL && isCarCallLit(button.floor());
    }

    @Override
    public void pressButton(int index) {
        Button button = buttons.get(index);
        switch (button.kind()) {
            case OPEN -> openDoor(CallType.CAR);
            case CLOSE -> closeDoor();
            case CAR_CALL -> pressCarCall(button.floor());
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
        }

        boolean doorChanged = door.tick(now);
        if (door.state() == DoorState.CLOSING && isDoorObstructed(now)) {
            door.obstruct(now);
            doorChanged = true;
        }
        if (doorChanged) {
            syncDoor();
        }

        if (trip == null && door.isClosed() && control != null) {
            TravelDirection before = control.direction();
            int next = control.nextStop(currentFloor);
            if (control.direction() != before) {
                // 階床表示器の矢印のため、進行方向の変化も送る
                syncCalls();
            }
            if (next == currentFloor) {
                serveCurrentFloor(now);
            } else if (next >= 0) {
                targetFloor = next;
                setTrip(new Trip(now, getY(), floors[next], speedSpec));
            }
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

    /** 今いる階の呼びに応えて戸を開く。 */
    private void serveCurrentFloor(long now) {
        CallType served = control != null ? control.arrive(currentFloor) : CallType.CAR;
        door.open(now, door.timing().dwellTicks(served));
        syncDoor();
        syncCalls();
    }

    /** セーフティシュー：閉まりかけの戸（かごと乗場の両方）に生き物が触れているかどうか。 */
    private boolean isDoorObstructed(long now) {
        double fraction = door.fractionAt(now);
        List<AABB> panels = new ArrayList<>(4);
        for (DoorGeometry.Side side : DoorGeometry.Side.values()) {
            for (Box box : CarShapes.doorPanels(model(), facing(), side, fraction)) {
                panels.add(CarShapes.toAabb(box, getX(), getY(), getZ()).inflate(SAFETY_SHOE_MARGIN));
            }
        }
        AABB search = panels.stream().reduce(AABB::minmax).orElseThrow();
        return !level().getEntitiesOfClass(LivingEntity.class, search,
                entity -> !entity.isSpectator() && panels.stream().anyMatch(entity.getBoundingBox()::intersects)).isEmpty();
    }

    // ---- 階床表示 ----

    /** 停止階 floor の階名。 */
    public String floorLabel(int floor) {
        return FloorIndicator.defaultLabel(floor);
    }

    /** ゲーム時刻 tick に階床表示器に出す階。かごの位置に最も近い階。 */
    public int indicatorFloor(double tick) {
        return floors.length > 0 ? FloorIndicator.displayedFloor(yAt(tick), floors) : 0;
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
        for (Box panel : CarShapes.doorPanels(model(), facing(), DoorGeometry.Side.CAR, door.fractionAt(tick))) {
            boxes.add(CarShapes.toAabb(panel, getX(), y, getZ()));
        }
        return boxes;
    }

    public List<Part> parts() {
        return CarShapes.parts(model(), facing());
    }

    /** かご全体の外形（ワールド座標）。床面のY座標 y の位置に置いたもの。戸が袖壁の外へはみ出す分を含む。 */
    public AABB exteriorAt(double y) {
        AABB exterior = CarShapes.toAabb(CarShapes.exterior(model(), facing()), getX(), y, getZ());
        AABB door = CarShapes.toAabb(CarShapes.doorTravelBounds(model(), facing(), DoorGeometry.Side.CAR), getX(), y, getZ());
        return exterior.minmax(door);
    }

    /**
     * entity がかごと一緒に動くべき位置にいるかどうか。かごの外形の真上または中にいて、
     * 足元が床面（少し下まで許す）から屋根の少し上までの間にあれば運ぶ。
     */
    public boolean carries(Entity entity, double carY) {
        Box exterior = CarShapes.exterior(model(), facing());
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
        this.entityData.set(DATA_DOOR, writeDoor(door));
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
            applyFloors(this.entityData.get(DATA_FLOORS).getIntArray("Floors"));
            readCalls(this.entityData.get(DATA_CALLS));
        } else if (DATA_TRIP.equals(key)) {
            Trip previous = trip;
            trip = readTrip(this.entityData.get(DATA_TRIP));
            if (trip == null && previous != null) {
                // 到着。クライアントの時刻が遅れていても、行先にぴったり着床させる
                setPos(getX(), previous.to(), getZ());
            }
        } else if (DATA_DOOR.equals(key)) {
            readDoor(this.entityData.get(DATA_DOOR), door);
        } else if (DATA_CALLS.equals(key)) {
            readCalls(this.entityData.get(DATA_CALLS));
        } else if (DATA_FIXTURES.equals(key)) {
            fixtures = readFixtures(this.entityData.get(DATA_FIXTURES));
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
        tag.putString("Facing", facing().getSerializedName());
        tag.putIntArray("Floors", floors);
        tag.putString("Speed", speedSpec.name());
        if (trip != null) {
            tag.put("Trip", writeTrip(trip));
        }
        tag.put("Door", writeDoor(door));
        if (control != null) {
            tag.put("Calls", writeCalls(control));
        }
        tag.put("Fixtures", writeFixtures(fixtures));
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
        setup(model, facing, tag.getIntArray("Floors"), parseEnum(SpeedSpec.class, tag.getString("Speed"), SpeedSpec.NORMAL_LOW));
        setFixtures(readFixtures(tag.getCompound("Fixtures")));
        // 保存時に走行中・開閉中だった場合は、ゲーム時刻に合わせてそのまま続きから動く
        setTrip(tag.contains("Trip") ? readTrip(tag.getCompound("Trip")) : null);
        readDoor(tag.getCompound("Door"), door);
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
        tag.putString("Speed", trip.speedSpec().name());
        return tag;
    }

    @Nullable
    private static Trip readTrip(CompoundTag tag) {
        if (!tag.contains("StartTick")) {
            return null;
        }
        return new Trip(tag.getLong("StartTick"), tag.getDouble("From"), tag.getDouble("To"),
                parseEnum(SpeedSpec.class, tag.getString("Speed"), SpeedSpec.NORMAL_LOW));
    }

    private static CompoundTag writeDoor(DoorController door) {
        CompoundTag tag = new CompoundTag();
        tag.putString("State", door.state().name());
        tag.putLong("StartTick", door.startTick());
        tag.putDouble("StartFraction", door.startFraction());
        tag.putInt("Dwell", door.dwellTicks());
        return tag;
    }

    private static void readDoor(CompoundTag tag, DoorController door) {
        door.restore(parseEnum(DoorState.class, tag.getString("State"), DoorState.CLOSED),
                tag.getLong("StartTick"), tag.getDouble("StartFraction"), tag.getInt("Dwell"));
    }

    private static CompoundTag writeFixtures(FixtureConfig fixtures) {
        CompoundTag tag = new CompoundTag();
        tag.putString("PanelSides", fixtures.panelSides().name());
        tag.putString("CarIndicator", fixtures.carIndicator().name());
        tag.putBoolean("LeftPanelIndicator", fixtures.leftPanelIndicator());
        tag.putString("HallIndicator", fixtures.hallIndicator().name());
        return tag;
    }

    private static FixtureConfig readFixtures(CompoundTag tag) {
        FixtureConfig defaults = FixtureConfig.DEFAULT;
        return new FixtureConfig(
                parseEnum(FixtureConfig.PanelSides.class, tag.getString("PanelSides"), defaults.panelSides()),
                parseEnum(FixtureConfig.CarIndicator.class, tag.getString("CarIndicator"), defaults.carIndicator()),
                tag.getBoolean("LeftPanelIndicator"),
                parseEnum(FixtureConfig.HallIndicator.class, tag.getString("HallIndicator"), defaults.hallIndicator()));
    }

    private static CompoundTag writeCalls(CollectiveControl control) {
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
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

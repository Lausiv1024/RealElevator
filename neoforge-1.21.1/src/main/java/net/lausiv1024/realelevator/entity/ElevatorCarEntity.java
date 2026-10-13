package net.lausiv1024.realelevator.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.CallType;
import net.lausiv1024.realelevator.core.door.DoorController;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
import net.lausiv1024.realelevator.core.door.DoorState;
import net.lausiv1024.realelevator.core.door.DoorTiming;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.motion.Trip;
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
 * サーバーとクライアントで同じ計算をする。サーバーから送るのは機種・向き・発車情報・戸の状態だけで、
 * 毎tickの位置や開き具合は同期しない。エンティティの位置はかご内の床面の中心。
 *
 * <p>戸が全閉になるまで発車せず、着床すると戸を開く。乗場の扉（{@link LandingDoorEntity}）は、
 * かごが着床している階のものだけが、かごの戸と同じ開き具合で動く。
 */
public class ElevatorCarEntity extends Entity {
    private static final EntityDataAccessor<Byte> DATA_MODEL = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<CompoundTag> DATA_TRIP = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<CompoundTag> DATA_DOOR = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** セーフティシューの検知に使う、戸板のまわりのわずかな余裕。 */
    private static final double SAFETY_SHOE_MARGIN = 0.01;

    public enum CallResult {
        /** 受け付けた。戸が閉まりしだい発車する。 */
        ACCEPTED,
        /** かごがその階にいるので、戸を開いた。 */
        OPENED,
        /** 走行中、または別の呼びに応えている途中。 */
        BUSY,
        /** 存在しない階。 */
        INVALID
    }

    /** 停止階の床面のY座標（下から順）。サーバーだけが持つ。 */
    private int[] floors = new int[0];
    private SpeedSpec speedSpec = SpeedSpec.NORMAL_LOW;
    @Nullable
    private Trip trip;
    private final DoorController door = new DoorController(DoorTiming.STANDARD);
    /** 戸が閉まるのを待っている行先（停止階の番号）。なければ −1。サーバーだけが持つ。 */
    private int pendingFloor = -1;
    private CallType pendingCall = CallType.CAR;
    /** 今の走行がどの呼びに応えたものか。着床したときの開放待機時間を決める。 */
    private CallType tripCall = CallType.CAR;
    /** 最後に位置を計算したゲーム時刻。描画の補間に使う。 */
    private long lastTick;

    public ElevatorCarEntity(EntityType<? extends ElevatorCarEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(CarModel model, Direction facing, int[] floors, SpeedSpec speedSpec) {
        this.entityData.set(DATA_MODEL, (byte) model.ordinal());
        this.entityData.set(DATA_FACING, facing);
        this.floors = floors.clone();
        Arrays.sort(this.floors);
        this.speedSpec = speedSpec;
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_MODEL, (byte) CarModel.P11.ordinal());
        builder.define(DATA_FACING, Direction.SOUTH);
        builder.define(DATA_TRIP, new CompoundTag());
        builder.define(DATA_DOOR, new CompoundTag());
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

    public boolean isMoving() {
        return trip != null;
    }

    public DoorState doorState() {
        return door.state();
    }

    /**
     * 停止階 floorIndex（0始まり、下から）への呼び。
     *
     * <p>フェーズ1では呼びを1つずつ処理する。かごがその階にいれば戸を開き、
     * そうでなければ戸が全閉になってから発車し、着いたら戸を開く。
     */
    public CallResult call(int floorIndex, CallType callType) {
        if (level().isClientSide || floorIndex < 0 || floorIndex >= floors.length) {
            return CallResult.INVALID;
        }
        if (trip != null || pendingFloor >= 0) {
            return CallResult.BUSY;
        }
        if (floors[floorIndex] == getY()) {
            openDoor(callType);
            return CallResult.OPENED;
        }
        pendingFloor = floorIndex;
        pendingCall = callType;
        return CallResult.ACCEPTED;
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

    @Override
    public void tick() {
        super.tick();
        long now = level().getGameTime();
        lastTick = now;
        if (trip != null) {
            setPos(getX(), trip.positionAt(now), getZ());
        }
        if (!level().isClientSide) {
            serverTick(now);
        }
    }

    private void serverTick(long now) {
        // 到着したtickの最後の移動量も乗っているエンティティに加えられるよう、走行情報は1tick遅れて消す
        if (trip != null && trip.isArrivedAt(now - 1)) {
            setTrip(null);
            door.open(now, door.timing().dwellTicks(tripCall));
            syncDoor();
        }

        boolean doorChanged = door.tick(now);
        if (door.state() == DoorState.CLOSING && isDoorObstructed(now)) {
            door.obstruct(now);
            doorChanged = true;
        }
        if (doorChanged) {
            syncDoor();
        }

        if (trip == null && pendingFloor >= 0 && door.isClosed()) {
            setTrip(new Trip(now, getY(), floors[pendingFloor], speedSpec));
            tripCall = pendingCall;
            pendingFloor = -1;
        }
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

    private void setTrip(@Nullable Trip trip) {
        this.trip = trip;
        this.entityData.set(DATA_TRIP, trip != null ? writeTrip(trip) : new CompoundTag());
    }

    private void syncDoor() {
        this.entityData.set(DATA_DOOR, writeDoor(door));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_MODEL.equals(key) || DATA_FACING.equals(key)) {
            setBoundingBox(makeBoundingBox());
        } else if (DATA_TRIP.equals(key) && level().isClientSide) {
            Trip previous = trip;
            trip = readTrip(this.entityData.get(DATA_TRIP));
            if (trip == null && previous != null) {
                // 到着。クライアントの時刻が遅れていても、行先にぴったり着床させる
                setPos(getX(), previous.to(), getZ());
            }
        } else if (DATA_DOOR.equals(key) && level().isClientSide) {
            readDoor(this.entityData.get(DATA_DOOR), door);
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

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putString("Model", model().name());
        tag.putString("Facing", facing().getSerializedName());
        tag.putIntArray("Floors", floors);
        tag.putString("Speed", speedSpec.name());
        if (trip != null) {
            tag.put("Trip", writeTrip(trip));
        }
        tag.putString("TripCall", tripCall.name());
        tag.put("Door", writeDoor(door));
        tag.putInt("PendingFloor", pendingFloor);
        tag.putString("PendingCall", pendingCall.name());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        CarModel model = parseEnum(CarModel.class, tag.getString("Model"), CarModel.P11);
        Direction facing = Direction.byName(tag.getString("Facing"));
        if (facing == null || facing.getAxis().isVertical()) {
            facing = Direction.SOUTH;
        }
        setup(model, facing, tag.getIntArray("Floors"), parseEnum(SpeedSpec.class, tag.getString("Speed"), SpeedSpec.NORMAL_LOW));
        // 保存時に走行中・開閉中だった場合は、ゲーム時刻に合わせてそのまま続きから動く
        setTrip(tag.contains("Trip") ? readTrip(tag.getCompound("Trip")) : null);
        tripCall = parseEnum(CallType.class, tag.getString("TripCall"), CallType.CAR);
        readDoor(tag.getCompound("Door"), door);
        syncDoor();
        pendingFloor = tag.contains("PendingFloor") && tag.getInt("PendingFloor") < floors.length ? tag.getInt("PendingFloor") : -1;
        pendingCall = parseEnum(CallType.class, tag.getString("PendingCall"), CallType.CAR);
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

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

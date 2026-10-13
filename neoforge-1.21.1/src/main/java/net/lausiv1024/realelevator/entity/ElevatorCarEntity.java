package net.lausiv1024.realelevator.entity;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarGeometry.Part;
import net.lausiv1024.realelevator.core.car.CarModel;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * エレベーターのかご。
 *
 * <p>位置は発車情報（{@link Trip}）とゲーム時刻から求め、サーバーとクライアントで同じ計算をする。
 * サーバーから送るのは機種・向き・発車情報だけで、毎tickの位置は同期しない。
 * エンティティの位置はかご内の床面の中心。
 */
public class ElevatorCarEntity extends Entity {
    private static final EntityDataAccessor<Byte> DATA_MODEL = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.DIRECTION);
    private static final EntityDataAccessor<CompoundTag> DATA_TRIP = SynchedEntityData.defineId(ElevatorCarEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** 停止階の床面のY座標（下から順）。サーバーだけが持つ。 */
    private int[] floors = new int[0];
    private SpeedSpec speedSpec = SpeedSpec.NORMAL_LOW;
    @Nullable
    private Trip trip;
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

    public boolean isMoving() {
        return trip != null;
    }

    /**
     * 停止階 floorIndex（0始まり、下から）へ発車する。
     *
     * @return 発車したかどうか。走行中、または既にその階にいるときは発車しない
     */
    public boolean departTo(int floorIndex) {
        if (level().isClientSide || trip != null || floorIndex < 0 || floorIndex >= floors.length) {
            return false;
        }
        int target = floors[floorIndex];
        if (target == getY()) {
            return false;
        }
        setTrip(new Trip(level().getGameTime(), getY(), target, speedSpec));
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        long now = level().getGameTime();
        lastTick = now;
        if (trip != null) {
            setPos(getX(), trip.positionAt(now), getZ());
            // 到着したtickの最後の移動量も乗っているエンティティに加えられるよう、走行情報は1tick遅れて消す
            if (!level().isClientSide && trip.isArrivedAt(now - 1)) {
                setTrip(null);
            }
        }
    }

    /** ゲーム時刻 tick（partialTickを含めてよい）でのかごの床面のY座標。 */
    public double yAt(double tick) {
        return trip != null ? trip.positionAt(tick) : getY();
    }

    /** 描画用の位置。バニラの補間と同じく、直前のtickと最新のtickの間を partialTick で表す。 */
    public double renderY(float partialTick) {
        return trip != null ? trip.positionAt(lastTick - 1 + partialTick) : Mth.lerp(partialTick, yo, getY());
    }

    /** 部位の箱（ワールド座標）。床面のY座標 y の位置に置いたもの。 */
    public List<AABB> partBoxesAt(double y) {
        return CarShapes.parts(model(), facing()).stream()
                .map(p -> CarShapes.toAabb(p.box(), getX(), y, getZ()))
                .toList();
    }

    public List<Part> parts() {
        return CarShapes.parts(model(), facing());
    }

    /** かご全体の外形（ワールド座標）。床面のY座標 y の位置に置いたもの。 */
    public AABB exteriorAt(double y) {
        return CarShapes.toAabb(CarShapes.exterior(model(), facing()), getX(), y, getZ());
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
        CarTracker.add(this);
    }

    @Override
    public void onRemovedFromLevel() {
        super.onRemovedFromLevel();
        CarTracker.remove(this);
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
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        CarModel model = parseEnum(CarModel.class, tag.getString("Model"), CarModel.P11);
        Direction facing = Direction.byName(tag.getString("Facing"));
        if (facing == null || facing.getAxis().isVertical()) {
            facing = Direction.SOUTH;
        }
        setup(model, facing, tag.getIntArray("Floors"), parseEnum(SpeedSpec.class, tag.getString("Speed"), SpeedSpec.NORMAL_LOW));
        // 保存時に走行中だった場合は、ゲーム時刻に合わせてそのまま続きから走る
        setTrip(tag.contains("Trip") ? readTrip(tag.getCompound("Trip")) : null);
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

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String name, E fallback) {
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}

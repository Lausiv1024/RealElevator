package net.lausiv1024.realelevator.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.door.DoorGeometry;
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
 * 乗場の扉。
 *
 * <p>位置（x, z）と向きはかごと同じで、y はその階の床面。座標はかごがこの階に着床したときのかごのローカル座標で表す。
 * 開き具合は自分では持たず、かごがこの階に着床しているときだけ、かごの戸と同じ開き具合で動く。
 * そのため、かごとの間で追加の同期はいらない。
 */
public class LandingDoorEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> DATA_CAR = SynchedEntityData.defineId(LandingDoorEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> DATA_MODEL = SynchedEntityData.defineId(LandingDoorEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(LandingDoorEntity.class, EntityDataSerializers.DIRECTION);

    @Nullable
    private ElevatorCarEntity cachedCar;

    public LandingDoorEntity(EntityType<? extends LandingDoorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(ElevatorCarEntity car) {
        this.entityData.set(DATA_CAR, Optional.of(car.getUUID()));
        this.entityData.set(DATA_MODEL, (byte) car.model().ordinal());
        this.entityData.set(DATA_FACING, car.facing());
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CAR, Optional.empty());
        builder.define(DATA_MODEL, (byte) CarModel.P11.ordinal());
        builder.define(DATA_FACING, Direction.SOUTH);
    }

    public CarModel model() {
        CarModel[] models = CarModel.values();
        return models[Mth.clamp(this.entityData.get(DATA_MODEL), 0, models.length - 1)];
    }

    public Direction facing() {
        return this.entityData.get(DATA_FACING);
    }

    public Optional<UUID> carUuid() {
        return this.entityData.get(DATA_CAR);
    }

    /** つながっているかご。読み込まれていなければ null。 */
    @Nullable
    public ElevatorCarEntity car() {
        if (cachedCar != null && !cachedCar.isRemoved()) {
            return cachedCar;
        }
        cachedCar = carUuid().map(uuid -> ElevatorTracker.findCar(level(), uuid)).orElse(null);
        return cachedCar;
    }

    /** かごがこの階に着床しているかどうか。 */
    public boolean isCarHere() {
        ElevatorCarEntity car = car();
        return car != null && !car.isMoving() && car.getY() == getY();
    }

    /** ゲーム時刻 tick での開き具合。かごがこの階にいなければ全閉。 */
    public double fractionAt(double tick) {
        ElevatorCarEntity car = car();
        return isCarHere() && car != null ? car.doorFractionAt(tick) : 0;
    }

    /** 描画用の開き具合。かごと同じ時刻の基準を使う。 */
    public double renderFraction(float partialTick) {
        ElevatorCarEntity car = car();
        return isCarHere() && car != null ? car.doorFractionAt(car.renderTick(partialTick)) : 0;
    }

    public List<AABB> collisionBoxesAt(long tick) {
        return CarShapes.doorPanels(model(), facing(), DoorGeometry.Side.LANDING, fractionAt(tick)).stream()
                .map(box -> CarShapes.toAabb(box, getX(), getY(), getZ()))
                .toList();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_MODEL.equals(key) || DATA_FACING.equals(key)) {
            setBoundingBox(makeBoundingBox());
        } else if (DATA_CAR.equals(key)) {
            cachedCar = null;
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        Box bounds = CarShapes.doorTravelBounds(model(), facing(), DoorGeometry.Side.LANDING);
        return CarShapes.toAabb(bounds, getX(), getY(), getZ());
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
        carUuid().ifPresent(uuid -> tag.putUUID("Car", uuid));
        tag.putString("Model", model().name());
        tag.putString("Facing", facing().getSerializedName());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.entityData.set(DATA_CAR, tag.hasUUID("Car") ? Optional.of(tag.getUUID("Car")) : Optional.empty());
        CarModel model;
        try {
            model = CarModel.valueOf(tag.getString("Model"));
        } catch (IllegalArgumentException e) {
            model = CarModel.P11;
        }
        this.entityData.set(DATA_MODEL, (byte) model.ordinal());
        Direction facing = Direction.byName(tag.getString("Facing"));
        this.entityData.set(DATA_FACING, facing != null && facing.getAxis().isHorizontal() ? facing : Direction.SOUTH);
        setBoundingBox(makeBoundingBox());
    }
}

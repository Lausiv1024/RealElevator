package net.lausiv1024.realelevator.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.core.design.LandingLayout;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
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
 *
 * <p>乗場の▲▼ボタン、階床表示器、ホールランタン、三方枠と枠のまわりの壁、敷居もこのエンティティが持つ（配置は {@link LandingLayout}）。
 * 応答灯はかごに登録された乗場呼びを表示する。
 *
 * <p>二方向出入口（前後）の奥の乗場の扉は、正面の形をかごの中心のまわりに180°回したもので、三方枠はブロックの格子にそろうよう厚くする。
 */
public class LandingDoorEntity extends Entity implements ButtonHost {
    private static final EntityDataAccessor<Optional<UUID>> DATA_CAR = SynchedEntityData.defineId(LandingDoorEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Byte> DATA_MODEL = SynchedEntityData.defineId(LandingDoorEntity.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Direction> DATA_FACING = SynchedEntityData.defineId(LandingDoorEntity.class, EntityDataSerializers.DIRECTION);
    /** かごのどちらの出入口の乗場か（{@link Entrance} の番号）。 */
    private static final EntityDataAccessor<Byte> DATA_ENTRANCE = SynchedEntityData.defineId(LandingDoorEntity.class, EntityDataSerializers.BYTE);

    @Nullable
    private ElevatorCarEntity cachedCar;

    public LandingDoorEntity(EntityType<? extends LandingDoorEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    public void setup(ElevatorCarEntity car) {
        setup(car, Entrance.FRONT);
    }

    /** かご car の、出入口 entrance の乗場の扉にする。 */
    public void setup(ElevatorCarEntity car, Entrance entrance) {
        this.entityData.set(DATA_ENTRANCE, (byte) entrance.ordinal());
        this.entityData.set(DATA_CAR, Optional.of(car.getUUID()));
        this.entityData.set(DATA_MODEL, CarShapes.encode(car.spec()));
        this.entityData.set(DATA_FACING, car.facing());
        setBoundingBox(makeBoundingBox());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_CAR, Optional.empty());
        builder.define(DATA_MODEL, CarShapes.encode(CarSpec.of(CarModel.P11)));
        builder.define(DATA_FACING, Direction.SOUTH);
        builder.define(DATA_ENTRANCE, (byte) Entrance.FRONT.ordinal());
    }

    /** かごのどちらの出入口の乗場か。 */
    public Entrance entrance() {
        Entrance[] values = Entrance.values();
        return values[Mth.clamp(this.entityData.get(DATA_ENTRANCE), 0, values.length - 1)];
    }

    /** かごのローカル座標の形（正面の形）を、ワールドの向きにするために90°回す回数。 */
    public int quarterTurns() {
        return facing().get2DDataValue() + entrance().quarterTurns();
    }

    /** 乗場の面を正面の乗場の面からずらす量（奥の乗場で三方枠を厚くする分）。 */
    public double faceOffset() {
        return CarShapes.faceOffset(spec(), entrance());
    }

    public CarModel model() {
        return spec().model();
    }

    /** 機種と扉の方式（かごと同じ）。 */
    public CarSpec spec() {
        return CarShapes.decode(this.entityData.get(DATA_MODEL));
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

    /** この乗場の配置。かごが読み込まれていなければ、初期値の設定で求める。 */
    public LandingLayout layout() {
        ElevatorCarEntity car = car();
        FixtureConfig fixtures = car != null ? car.fixtures() : FixtureConfig.DEFAULT;
        InteriorConfig.Landing landing = car != null ? car.interior().landing() : InteriorConfig.Landing.DEFAULT;
        return LandingLayout.of(spec(), fixtures, landing, faceOffset());
    }

    /** かごがこの階に着床しているかどうか（ランディングオープンでは、着床の直前に戸が開き始めたときから）。 */
    public boolean isCarHere() {
        ElevatorCarEntity car = car();
        return car != null && car.isAtLanding(getY());
    }

    /** ゲーム時刻 tick での開き具合。かごがこの階にいなければ全閉。 */
    public double fractionAt(double tick) {
        ElevatorCarEntity car = car();
        return isCarHere() && car != null ? car.doorFractionAt(tick, entrance()) : 0;
    }

    /** 描画用の開き具合。かごと同じ時刻の基準を使う。 */
    public double renderFraction(float partialTick) {
        ElevatorCarEntity car = car();
        return isCarHere() && car != null ? car.doorFractionAt(car.renderTick(partialTick), entrance()) : 0;
    }

    /** 当たり判定の箱（ワールド座標）。戸と三方枠。 */
    public List<AABB> collisionBoxesAt(double tick) {
        List<AABB> boxes = new ArrayList<>();
        for (Box box : CarShapes.doorPanels(spec(), facing(), DoorGeometry.Side.LANDING, fractionAt(tick), entrance())) {
            boxes.add(CarShapes.toAabb(box, getX(), getY(), getZ()));
        }
        for (Box box : CarShapes.landingFrame(spec(), facing(), entrance())) {
            boxes.add(CarShapes.toAabb(box, getX(), getY(), getZ()));
        }
        return boxes;
    }

    /** この扉の階の番号。かごが読み込まれていないか、停止階でなければ −1。 */
    public int floorIndex() {
        ElevatorCarEntity car = car();
        return car != null ? car.floorIndexAt(getY()) : -1;
    }

    /** ホールランタンの点灯する段（点滅の消灯中かどうかは含めない）。到着チャイムが鳴ってから、扉が閉まるまで点く。 */
    public Set<TravelDirection> litLanternSections() {
        ElevatorCarEntity car = car();
        int floor = floorIndex();
        if (car == null || floor < 0) {
            return Set.of();
        }
        return car.lanternSections(floor, entrance());
    }

    @Override
    public List<Button> buttons() {
        ElevatorCarEntity car = car();
        int floor = floorIndex();
        return car != null && floor >= 0
                ? layout().buttons(floor, car.floorCount()) : List.of();
    }

    @Override
    public AABB buttonBox(int index) {
        Box box = buttons().get(index).box().rotateQuarterTurns(quarterTurns());
        return CarShapes.toAabb(box, getX(), getY(), getZ());
    }

    @Override
    public boolean isButtonLit(int index) {
        ElevatorCarEntity car = car();
        Button button = buttons().get(index);
        if (car == null) {
            return false;
        }
        return button.wheelchair() ? car.isWheelchairHallCallLit(button.floor(), direction(button), entrance())
                : car.isHallCallLit(button.floor(), direction(button), entrance());
    }

    @Override
    public void pressButton(int index) {
        ElevatorCarEntity car = car();
        if (car != null) {
            Button button = buttons().get(index);
            car.pressHallCall(button.floor(), direction(button), entrance(), button.wheelchair());
        }
    }

    private static TravelDirection direction(Button button) {
        return button.kind() == ButtonLayout.Kind.HALL_UP ? TravelDirection.UP : TravelDirection.DOWN;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (DATA_MODEL.equals(key) || DATA_FACING.equals(key) || DATA_ENTRANCE.equals(key)) {
            setBoundingBox(makeBoundingBox());
        } else if (DATA_CAR.equals(key)) {
            cachedCar = null;
        }
    }

    @Override
    protected AABB makeBoundingBox() {
        // 描画のカリングに使うので、戸が動く範囲と、器具を含む三方枠の範囲全体を含める
        AABB bounds = CarShapes.toAabb(CarShapes.doorTravelBounds(spec(), facing(), DoorGeometry.Side.LANDING, entrance()), getX(), getY(), getZ());
        Box range = LandingLayout.bounds(spec(), faceOffset()).rotateQuarterTurns(quarterTurns());
        bounds = bounds.minmax(CarShapes.toAabb(range, getX(), getY(), getZ()));
        return bounds;
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
        tag.putString("DoorType", spec().door().name());
        tag.putString("Facing", facing().getSerializedName());
        tag.putString("Entrance", entrance().name());
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
        DoorType door;
        try {
            door = DoorType.valueOf(tag.getString("DoorType"));
        } catch (IllegalArgumentException e) {
            door = DoorType.CENTER;
        }
        this.entityData.set(DATA_MODEL, CarShapes.encode(new CarSpec(model, door)));
        Direction facing = Direction.byName(tag.getString("Facing"));
        this.entityData.set(DATA_FACING, facing != null && facing.getAxis().isHorizontal() ? facing : Direction.SOUTH);
        Entrance entrance = Entrance.FRONT;
        try {
            entrance = Entrance.valueOf(tag.getString("Entrance"));
        } catch (IllegalArgumentException ignored) {
            // 古い保存データは正面
        }
        this.entityData.set(DATA_ENTRANCE, (byte) entrance.ordinal());
        setBoundingBox(makeBoundingBox());
    }
}

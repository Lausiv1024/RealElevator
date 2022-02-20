package lausiv1024.entity;

import lausiv1024.REEntities;
import lausiv1024.REItems;
import net.minecraft.client.renderer.entity.SheepRenderer;
import net.minecraft.entity.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class CwtEntity extends ElevatorPartEntity{
    public static final DataParameter<Integer> AXIS_VALUE = EntityDataManager.defineId(CwtEntity.class, DataSerializers.INT);

    private static final int saVertical = 48;
    private static final int saHorizontal1 = 18;
    private static final int saHorizontal2 = 4;
    private boolean tes1 = false;
    private int tes2 = 0;

    public CwtEntity(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    public CwtEntity(World world){
        super(REEntities.CWT.get(), world);
        setDeltaMovement(Vector3d.ZERO);
    }

    @Override
    protected void defineSynchedData() {
        this.getEntityData().define(AXIS_VALUE, 0);
    }


    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
        putAxis(nbt.getInt("Axis"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("Axis", getAxis());
    }

    @Override
    public boolean isPickable() {
        return true;
    }



    public int getAxis() {
        return this.entityData.get(AXIS_VALUE);
    }

    @Override
    public ActionResultType interact(PlayerEntity playerEntity, Hand hand) {
        LOGGER.info("Interact");
        moving = true;
        tes1 = true;
        //setDeltaMovement(getDeltaMovement().add(new Vector3d(0.0, 0.01, 0.0)));
        return ActionResultType.SUCCESS;
    }

    public void putAxis(int axis) {
        if (axis > 1) {
            LOGGER.warn("Invalid Axis Id. Correct:( 0 : x  z : 1) provided  {}", axis);
            return;
        }
        this.entityData.set(AXIS_VALUE, axis);
    }


    @Override
    protected float getEyeHeight(Pose p_213316_1_, EntitySize p_213316_2_) {
        return 0.0f;
    }

    @Override
    public boolean hurt(DamageSource damageSrc, float damage) {
        if (damageSrc.getEntity() instanceof PlayerEntity){
            PlayerEntity pl = (PlayerEntity) damageSrc.getEntity();
            if (pl.getItemInHand(Hand.MAIN_HAND).getItem() == REItems.WRENCH.get()){
                remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        //LOGGER.info(getDeltaMovement());
        if (getY() > 255){
            setDeltaMovement(Vector3d.ZERO);
        }
        if (tes1){
            if (tes2 < 30){
                setDeltaMovement(getDeltaMovement().add(new Vector3d(0.0, 0.005, 0.0)));
            }
            else if (tes2 < 60){
                setDeltaMovement(getDeltaMovement().add(new Vector3d(0.0, -0.005, 0.0)));
            }
            else{
                tes1 = false;
                tes2 = 0;
                setDeltaMovement(Vector3d.ZERO);
            }
            tes2++;
        }
    }


    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        double asX = (getAxis() == 1 ? saHorizontal2 / 2.0 : saHorizontal1 / 2.0) / 16.0;
        double asZ = (getAxis() == 1 ? saHorizontal1 / 2.0 : saHorizontal2 / 2.0) / 16.0;
        double x1 = getX() - asX;
        double z1 = getZ() - asZ;
        double x2 = getX() + asX;
        double z2 = getZ() + asZ;
        double y1 = (getY() - saVertical / 32.0);
        double y2 = (y1 + saVertical / 16.0);

        return new AxisAlignedBB(x1, y1, z1, x2, y2, z2);
    }
}

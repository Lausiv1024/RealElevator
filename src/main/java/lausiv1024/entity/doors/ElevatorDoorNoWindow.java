package lausiv1024.entity.doors;

import lausiv1024.REEntities;
import lausiv1024.RealElevator;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class ElevatorDoorNoWindow extends AbstractElevatorDoorEntity {

    public ElevatorDoorNoWindow(EntityType<? extends Entity> type, World world) {
        super(type, world);
    }

    public ElevatorDoorNoWindow(World world){
        super(REEntities.ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE.get(), world);
    }

    public ElevatorDoorNoWindow (World world, Direction direction){
        super(REEntities.ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE.get(), world);
    }

    public ElevatorDoorNoWindow(World world, Direction direction, int tall){
        super(REEntities.ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE.get(), world);
    }

    @Override
    public void tick() {
        super.tick();
    }


    @Override
    public ActionResultType interact(PlayerEntity playerEntity, Hand hand) {
        World level = playerEntity.level;
        if (playerEntity.getItemInHand(hand).getItem() == Items.STICK){
            Vector3d pos = position();
            ElevatorDoorNoWindow elevatorDoorNoWindow = new ElevatorDoorNoWindow(level, Direction.SOUTH);
            elevatorDoorNoWindow.setPos(pos.x, pos.y, pos.z);
            level.addFreshEntity(elevatorDoorNoWindow);
            this.remove();
            return ActionResultType.SUCCESS;
        }else if (playerEntity.getItemInHand(hand).getItem() == Items.REDSTONE){
            Vector3d pos = position();
            ElevatorDoorNoWindow elevatorDoorNoWindow = new ElevatorDoorNoWindow(level, Direction.EAST);
            elevatorDoorNoWindow.setPos(pos.x, pos.y, pos.z);
            level.addFreshEntity(elevatorDoorNoWindow);
            this.remove();
            return ActionResultType.SUCCESS;
        }else{
            test(playerEntity, hand, level);
        }
        return ActionResultType.PASS;
    }

    private void test(PlayerEntity playerEntity, Hand hand, World level){

    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT nbt) {
        super.readAdditionalSaveData(nbt);
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT nbt) {
        super.addAdditionalSaveData(nbt);
    }
}

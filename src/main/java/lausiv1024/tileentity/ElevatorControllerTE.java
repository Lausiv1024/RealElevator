package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.RealElevator;
import lausiv1024.elevator.Elevator;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntityType;

import java.util.UUID;

public class ElevatorControllerTE extends ElevatorPartTE{
    Elevator elevator;

    public ElevatorControllerTE(TileEntityType<?> tileEntityType, UUID elevatorID) {
        super(tileEntityType, elevatorID);
        isController = true;
    }

    public ElevatorControllerTE(){
        super(RETileEntities.ELEVATOR_CONTROLLER.get());
        RealElevator.LOGGER.info("Construct");
        //elevator = new Elevator();
        isController = true;
    }

    public ElevatorControllerTE(Elevator elevator){
        super(RETileEntities.ELEVATOR_CONTROLLER.get());
        RealElevator.LOGGER.info("Construct");
        this.elevator = elevator;
        isController = true;
    }

    public Elevator getElevator() {
        return elevator;
    }

    public void setElevator(Elevator elevator) {
        this.elevator = elevator;
    }

    @Override
    protected void serverTick() {
        if (elevator != null) elevator.elevatorTick(level);
    }

    @Override
    public void onChunkUnloaded() {
        super.onChunkUnloaded();
        RealElevator.LOGGER.info("ChunkUnloaded");
        if (elevator != null) elevator.unLoad();
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        RealElevator.LOGGER.info("Saving NBT...");

        if (elevator != null)
            nbt.put("Elevator", elevator.saveToNbt());
        return nbt;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        RealElevator.LOGGER.info("Loading NBT...");

        elevator = Elevator.fromNBT(nbt.getCompound("Elevator"));
    }
}

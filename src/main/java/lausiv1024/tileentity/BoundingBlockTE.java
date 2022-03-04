package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.RealElevator;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;

public class BoundingBlockTE extends TileEntity {
    BlockPos mainPos = BlockPos.ZERO;
    public boolean blockRegistered = false;

    public BoundingBlockTE(TileEntityType<?> type) {
        super(type);
    }

    public BoundingBlockTE(){
        super(RETileEntities.BOUNDING_BLOCK_TE.get());
    }

    public void setMainLocation(BlockPos pos){
        mainPos = pos;
        blockRegistered = mainPos != null;
    }

    public BlockPos getMainPos(){
        if (mainPos == null){
            mainPos = BlockPos.ZERO;
        }
        return mainPos;
    }

    @Nullable
    private TileEntity getMainTE(){
        return blockRegistered ? level.getBlockEntity(mainPos): null;
    }

    private ElevatorPartTE getMain(){
        TileEntity te = getMainTE();
        if (te != null && !(te instanceof ElevatorPartTE)){
            return null;
        }

        return (ElevatorPartTE) te;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        INBT posa = nbt.get("MainPos");
        if (posa instanceof CompoundNBT){
            mainPos = NBTUtil.readBlockPos((CompoundNBT) posa);
        }else mainPos = BlockPos.ZERO;
        blockRegistered = nbt.getBoolean("Registered");
        RealElevator.LOGGER.info(mainPos);
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        nbt.put("MainPos", NBTUtil.writeBlockPos(mainPos));
        nbt.putBoolean("Registered", blockRegistered);
        return nbt;
    }
}

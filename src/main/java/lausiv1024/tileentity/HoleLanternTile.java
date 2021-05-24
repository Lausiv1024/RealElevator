package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.RealElevatorCore;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;

import java.util.UUID;

public class HoleLanternTile extends ElevatorPartTE implements ITickableTileEntity {
    //None : 0; UP : 1; DOWN : 2; BOTH : 3
    int lightMode = 0;
    //Yellow : 0  White : 1
    int lightColor = 0;
    int lightTick = 0;
    boolean isBlinking = true;

    public HoleLanternTile(UUID elevatorID){
        super(RETileEntities.HOLE_LANTERN, elevatorID);
    }

    public HoleLanternTile(){
        super(RETileEntities.HOLE_LANTERN, UUID.randomUUID());
    }

    public void setLightMode(int a){
        lightMode = a;
        lightTick = 0;
    }
    public int getLightMode(){return lightMode;}
    public int getLightTick(){return lightTick;}
    public void setLightColor(int a){lightColor = a;}
    public int getLightColor(){return lightColor;}
    public boolean getBlinking(){return isBlinking;}
    public void setBlinking(boolean blinking) {
        lightTick = 0;
        isBlinking = blinking;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        lightMode = nbt.getInt("LightMode");
        lightColor = nbt.getInt("LightColor");
        lightTick = nbt.getInt("LightTick");
        isBlinking = nbt.getBoolean("IsBlinking");
        RealElevatorCore.LOGGER.info(String.valueOf(lightMode));
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("LightMode", lightMode);
        nbt.putInt("LightColor", lightColor);
        nbt.putInt("LightTick", lightTick);
        nbt.putBoolean("IsBlinking", isBlinking);
        return nbt;
    }

    @Override
    public void tick() {
        if (lightMode > 0 && isBlinking){
            lightTick++;
            if (lightTick >= 20) lightTick = 0;
        }else{
            lightTick = -1;
        }
    }
}

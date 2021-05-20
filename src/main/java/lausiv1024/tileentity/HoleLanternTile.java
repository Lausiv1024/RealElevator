package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.tileentity.ITickableTileEntity;

public class HoleLanternTile extends ElevatorPartTE implements ITickableTileEntity {
    //None : 0 UP : 1 DOWN : 2 BOTH : 3
    int lightMode = 0;
    //Yellow : 0  White : 1
    int lightColor = 0;
    int lightTick = 0;

    public HoleLanternTile(){
        super(RETileEntities.HOLE_LANTERN);
    }

    public void setLightMode(int a){
        lightMode = a;
    }
    public int getLightMode(){return lightMode;}
    public int getLightTick(){return lightTick;}
    public void setLightColor(int a){lightColor = a;}
    public int getLightColor(){return lightColor;}

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        lightMode = nbt.getInt("LightMode");
        lightColor = nbt.getInt("LightColor");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putInt("LightMode", lightMode);
        nbt.putInt("LightColor", lightColor);
        return nbt;
    }

    @Override
    public void tick() {
        if (lightColor > 0){
            lightTick++;
            if (lightTick >= 20) lightTick = 0;
        }else{
            lightTick = -1;
        }
    }
}

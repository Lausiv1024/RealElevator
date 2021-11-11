package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;

import java.util.UUID;

public class LandingButtonBlockTE extends ElevatorPartTE{
    boolean isSingle; //1ボタンかどうか

    public boolean up = false;
    public boolean down = false;
    public int color = 0;

    public LandingButtonBlockTE(TileEntityType<?> tileEntityType, UUID elevatorID, boolean isSingle) {
        super(tileEntityType, elevatorID);
        this.isSingle = isSingle;
    }

    public LandingButtonBlockTE(boolean isSingle){
        super(RETileEntities.LANDING_BUTTON_TE.get());
        this.isSingle = isSingle;
    }

    public LandingButtonBlockTE(){
        super(RETileEntities.LANDING_BUTTON_TE.get());
        isSingle = false;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        up = nbt.getBoolean("Up");
        down = nbt.getBoolean("Down");
        color = nbt.getInt("Color");
        isSingle = nbt.getBoolean("Single");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putBoolean("Up", up);
        nbt.putBoolean("Down", down);
        nbt.putBoolean("Single", isSingle);
        nbt.putInt("Color", color);
        return nbt;
    }
}

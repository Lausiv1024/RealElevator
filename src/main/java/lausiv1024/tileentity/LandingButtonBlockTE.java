package lausiv1024.tileentity;

import lausiv1024.RETileEntities;
import lausiv1024.blocks.FloorController;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntityType;

import java.util.UUID;

public class LandingButtonBlockTE extends ElevatorPartTE{
    protected boolean up = false;
    protected boolean down = false;
    protected boolean called = false;
    protected int color = 0;

    public LandingButtonBlockTE(TileEntityType<?> tileEntityType, UUID elevatorID) {
        super(tileEntityType, elevatorID);
    }

    public LandingButtonBlockTE(TileEntityType<?> tileEntityType){
        super(tileEntityType);
    }

    public LandingButtonBlockTE(){
        super(RETileEntities.LANDING_BUTTON_TE.get());
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        if (color > 1) return;
        this.color = color;
    }

    public boolean isUp() {
        return up;
    }

    public boolean isDown() {
        return down;
    }

    public boolean isCalled() {
        return called;
    }

    public void setCalled(boolean called) {
        if (getBlockState().getValue(FloorController.IS_SINGLE))
            this.called = called;
    }

    public void upA(){
        up = true;
        called = true;
    }

    public void dwA(){
        down = true;
        called = true;
    }

    public void rm(boolean downM, boolean upM){
        down = !downM;
        up = !upM;
        if (getBlockState().getValue(FloorController.IS_SINGLE)) return;
        if (!down && !up) called = false;
    }

    @Override
    public void load(BlockState state, CompoundNBT nbt) {
        super.load(state, nbt);
        up = nbt.getBoolean("Up");
        down = nbt.getBoolean("Down");
        color = nbt.getInt("Color");
        called = nbt.getBoolean("Called");
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {
        super.save(nbt);
        nbt.putBoolean("Up", up);
        nbt.putBoolean("Down", down);
        nbt.putInt("Color", color);
        nbt.putBoolean("Called", called);
        return nbt;
    }
}

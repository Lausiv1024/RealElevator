package lausiv1024.tileentity;

import lausiv1024.elevator.EleVeneerType;
import net.minecraft.tileentity.TileEntityType;

public class JambTE extends ElevatorPartTE{
    private EleVeneerType veneerType;

    public JambTE(TileEntityType<?> tileEntityType) {
        super(tileEntityType);
    }

    public EleVeneerType getVeneerType() {
        return veneerType;
    }
}

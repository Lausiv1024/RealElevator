package net.lausiv1024.realelevator;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// This class will not load on dedicated servers. Accessing client side code from here is safe.
@Mod(value = RealElevator.MODID, dist = Dist.CLIENT)
public class RealElevatorClient {
    public RealElevatorClient(ModContainer container) {
    }
}

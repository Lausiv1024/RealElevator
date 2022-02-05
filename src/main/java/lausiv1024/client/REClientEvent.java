package lausiv1024.client;

import lausiv1024.RealElevator;
import net.minecraft.client.Minecraft;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = RealElevator.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.DEDICATED_SERVER)
public class REClientEvent {
    public static void serverTick (TickEvent.ServerTickEvent event){
        final World level = Minecraft.getInstance().level;

    }
}

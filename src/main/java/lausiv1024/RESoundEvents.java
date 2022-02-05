package lausiv1024;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class RESoundEvents {
    private static final List<SoundEvent> SOUND_EVENTS = new ArrayList<>();

    public static final SoundEvent CALLSOUND = register("call", new ResourceLocation(RealElevator.ID, "call"));
    public static final SoundEvent UPSOUND = register("up", new ResourceLocation(RealElevator.ID, "up"));
    public static final SoundEvent DOWNSOUND = register("down", new ResourceLocation(RealElevator.ID, "down"));

    private static SoundEvent register(String registryName, ResourceLocation location){
        SoundEvent soundEvent = new SoundEvent(location).setRegistryName(registryName);
        SOUND_EVENTS.add(soundEvent);
        return soundEvent;
    }

    @SubscribeEvent
    public static void registerSoundEvents(RegistryEvent.Register<SoundEvent> event){
        for (SoundEvent soundEvent : SOUND_EVENTS){
            event.getRegistry().register(soundEvent);
        }
    }
}

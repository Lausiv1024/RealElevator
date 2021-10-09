package lausiv1024;

import lausiv1024.entity.EleButtonEntity;
import lausiv1024.entity.doors.ElevatorDoorNoWindow;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class REEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITIES, RealElevatorCore.ID);

    public static final RegistryObject<EntityType<EleButtonEntity>> ELEVATOR_BUTTON = ENTITY_TYPES.register("ele_button",
            () -> EntityType.Builder.<EleButtonEntity>of(EleButtonEntity::new, EntityClassification.MISC)
                    .build(RealElevatorCore.ID + ":ele_button"));
    public static final RegistryObject<EntityType<ElevatorDoorNoWindow>> ELEVATOR_DOOR_NO_WINDOW_ENTITY_TYPE = ENTITY_TYPES.register("door_no_window",
            () -> EntityType.Builder.<ElevatorDoorNoWindow>of(ElevatorDoorNoWindow::new, EntityClassification.MISC)
                   .build(RealElevatorCore.ID + ":door_no_window"));

    public static void register(IEventBus eventBus){
        ENTITY_TYPES.register(eventBus);
    }
}

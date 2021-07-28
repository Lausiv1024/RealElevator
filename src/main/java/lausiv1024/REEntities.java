package lausiv1024;

import lausiv1024.entity.EleButtonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntityType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class REEntities {
    private static final List<EntityType<? extends Entity>> ENTITY_TYPES = new ArrayList<>();

    public static final EntityType<EleButtonEntity> ELEVATOR_BUTTON = register("ele_button", EntityType.Builder.of(EleButtonEntity::new, EntityClassification.MISC).sized(0.5f, 0.5f)
            .build("realelevator:ele_button"));

    private static EntityType register(String registryName, EntityType<? extends Entity> type){
        EntityType entityTypeNamed = type.setRegistryName(RealElevatorCore.ID ,registryName);
        ENTITY_TYPES.add(entityTypeNamed);
        return entityTypeNamed;
    }

    @SubscribeEvent
    public static void registerEntities(final RegistryEvent.Register<EntityType<?>> event){
        for (EntityType<? extends Entity> entityType : ENTITY_TYPES) {
            event.getRegistry().register(entityType);
        }
    }
}

package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, RealElevator.MODID);

    // 当たり判定の箱はエンティティ側で機種と向きから作る。ここでの大きさは明るさを測る目の高さにだけ使う
    public static final DeferredHolder<EntityType<?>, EntityType<ElevatorCarEntity>> ELEVATOR_CAR = ENTITY_TYPES.register("elevator_car",
            () -> EntityType.Builder.<ElevatorCarEntity>of(ElevatorCarEntity::new, MobCategory.MISC)
                    .sized(2.0F, 2.0F)
                    .noSummon()
                    .fireImmune()
                    .clientTrackingRange(10)
                    .updateInterval(20)
                    .build("elevator_car"));

    private ModEntities() {
    }
}

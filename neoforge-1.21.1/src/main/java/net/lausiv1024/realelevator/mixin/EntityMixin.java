package net.lausiv1024.realelevator.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

import net.lausiv1024.realelevator.entity.CarCollisions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * このMODの唯一のMixin。処理の中身は各クラスに置き、ここでは呼び出しだけを差し込む。
 */
@Mixin(Entity.class)
abstract class EntityMixin {
    /** エンティティの移動時の衝突判定に、かごの床・壁・天井を加える。 */
    @WrapOperation(
            method = "collide",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntityCollisions(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;"))
    private List<VoxelShape> realelevator$addCarCollisions(Level level, Entity entity, AABB area, Operation<List<VoxelShape>> original) {
        return CarCollisions.withCarShapes((Entity) (Object) this, area, original.call(level, entity, area));
    }
}

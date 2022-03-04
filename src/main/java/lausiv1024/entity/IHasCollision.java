package lausiv1024.entity;

import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.HangingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.util.math.AxisAlignedBB;

import java.util.List;

public interface IHasCollision {

    List<AxisAlignedBB> getEntityCollisions();

    default boolean canCollideW(Entity entity){
        if (entity instanceof PlayerEntity && entity.isSpectator()){
            return false;
        }
        if (entity.noPhysics) return false;
        if (entity instanceof HangingEntity) return false;
        if (entity instanceof ProjectileEntity) return false;
        if (entity instanceof ElevatorPartEntity) return false;
        return entity.getPistonPushReaction() == PushReaction.NORMAL;
    }
}

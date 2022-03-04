package lausiv1024.entity;

import lausiv1024.RealElevator;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import org.apache.commons.lang3.mutable.MutableBoolean;

import static java.lang.Math.signum;
import static java.lang.Math.abs;


import java.util.List;

public class ElevatorCollisionV2 {
    enum PlayerType{
        NONE, CLIENT, REMOTE, SERVER
    }

    //Entityをかご室内に載せたり壁の判定を与える部分。めっちゃ大変。少なくともテスト期間に手をつけていい場所じゃない
    public static void applyCollision(ElevatorPartEntity elevatorPart){
        if (!(elevatorPart instanceof IHasCollision)) return;
        IHasCollision collisionHolder = (IHasCollision) elevatorPart;
        World world = elevatorPart.getCommandSenderWorld();
        AxisAlignedBB bounds = elevatorPart.getBoundingBox();

        Vector3d elePartPosition = elevatorPart.position();
        Vector3d elePartMotion = elePartPosition.subtract(elevatorPart.getPrevPos());

        boolean skipClientPlayer = false;

        List<Entity> entitiesInAABB = world.getEntitiesOfClass(Entity.class,
                bounds.inflate(2).expandTowards(0, 32, 0), collisionHolder::canCollideW);

        for (Entity entity : entitiesInAABB){
            PlayerType playerType = getPlayerType(entity);
            if (playerType == PlayerType.REMOTE) continue;
            if (playerType == PlayerType.SERVER && entity instanceof ServerPlayerEntity){
                ((ServerPlayerEntity) entity).connection.aboveGroundTickCount = 0;
                continue;
            }

            if (playerType == PlayerType.CLIENT){
                if (skipClientPlayer) continue;
                else skipClientPlayer = true;
            }

            //ここからがマジでしんどい

            Vector3d entityPos = entity.position();
            AxisAlignedBB entityBounds = entity.getBoundingBox();
            Vector3d motion = entity.getDeltaMovement();

            motion = motion.subtract(elePartMotion);

            final Vector3d motionCopy = motion;

            List<AxisAlignedBB> collidedAABB = collisionHolder.getEntityCollisions();

            Vector3d collisionResponse = Vector3d.ZERO;
            Vector3d normal = Vector3d.ZERO;
            Vector3d loc = Vector3d.ZERO;
            boolean collide = false;
            float tempResponse = 1;

            for (boolean horizontalPass : RealElevator.TRUE_AND_FALSE){
                boolean verticalPass = !horizontalPass;

                for (AxisAlignedBB bb : collidedAABB){
                    final Vector3d curResponse = collisionResponse;
                    if (bb.getCenter().x - entityBounds.getXsize() - 1 > bb.getXsize() / 2) continue;
                    if (bb.getCenter().y - entityBounds.getYsize() - 1 > bb.getYsize() / 2) continue;
                    if (bb.getCenter().z - entityBounds.getZsize() - 1 > bb.getZsize() / 2) continue;

                    // ????????

                    if (verticalPass && !collide){

                    }
                }
            }
        }
    }

    private static PlayerType getPlayerType(Entity entity) {
        if (!(entity instanceof PlayerEntity))
            return PlayerType.NONE;
        if (!entity.level.isClientSide)
            return PlayerType.SERVER;
        MutableBoolean isClient = new MutableBoolean(false);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> isClient.setValue(isClientPlayerEntity(entity)));
        return isClient.booleanValue() ? PlayerType.CLIENT : PlayerType.REMOTE;
    }

    @OnlyIn(Dist.CLIENT)
    private static boolean isClientPlayerEntity(Entity entity) {
        return entity instanceof ClientPlayerEntity;
    }

    public static class CC1{
        Vector3d axis;
        double separation;

        static final double UNDEFINED = -1;
        double latestCollisionEntryTime = UNDEFINED;
        double earliestCollisionExitTime = Double.MAX_VALUE;
        boolean isDiscreteCollision = true;
        Vector3d collisionPosition;

        Vector3d stepSeparationAxis;
        double stepSeparation;

        Vector3d normalAxis;
        double normalSeparation;

        public double getTimeOfImpact() {
            if (latestCollisionEntryTime == UNDEFINED)
                return UNDEFINED;
            if (latestCollisionEntryTime > earliestCollisionExitTime)
                return UNDEFINED;
            return latestCollisionEntryTime;
        }

        public boolean isSurfaceCollision() {
            return true;
        }

        public Vector3d getCollisionNormal() {
            return normalAxis == null ? null : createSeparationVec(normalSeparation, normalAxis);
        }

        public Vector3d getCollisionPosition() {
            return collisionPosition;
        }

        public Vector3d asSeparationVec(double obbStepHeight) {
            if (isDiscreteCollision) {
                if (stepSeparation <= obbStepHeight)
                    return createSeparationVec(stepSeparation, stepSeparationAxis);
                return asSeparationVec();
            }
            double t = getTimeOfImpact();
            if (t == UNDEFINED)
                return null;
            return Vector3d.ZERO;
        }

        public CC1(){
            axis = Vector3d.ZERO;
            separation = Double.MAX_VALUE;
        }
        public Vector3d asSeparationVec(){
            double d = separation;
            Vector3d axis = this.axis;
            return createSeparationVec(d, axis);
        }

        public Vector3d createSeparationVec(double sep, Vector3d axis){
            return axis.normalize().scale(signum(sep) * (abs(sep) + 1E-4));
        }
    }
}

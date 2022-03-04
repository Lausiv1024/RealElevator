package lausiv1024.util;

import lausiv1024.entity.CageEntity;
import lausiv1024.entity.ElevatorPartEntity;
import lausiv1024.entity.IHasCollision;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class CageUtil {

    //Copy from Thut Core - api.entity.blockentity.BlockEntityUpdater#getIngersect
    private static double intersect(final double minA, final double minB, final double minC,
                                    final double maxA, final double maxB, final double maxC){
        if (!(minC == minA || minC == maxA || maxC == minA || maxC == maxA)) return 0;

        double dmax_min, dmin_max, dmax_max, dmin_min;
        boolean max_max, max_min, min_max, min_min;

        dmax_max = maxA - maxB;
        dmax_min = maxA - minB;
        dmin_max = minA - maxB;
        dmin_min = minA - minB;

        max_max = maxA == maxB;
        max_min = maxA == minB;
        min_max = minA == maxB;
        min_min = minA == minB;

        if (min_min && MathHelper.equal(minB, minC)) min_min = false;
        if (max_max && MathHelper.equal(maxB, maxC)) max_max = false;
        if (max_min && MathHelper.equal(maxB, minC)) max_min = false;
        if (min_max && MathHelper.equal(minB, maxC)) min_max = false;

        if (!(min_min || max_max || max_min || min_max)) return 0;
        double intersectAmount = 0;

        if (min_min) intersectAmount = dmax_min;
        if (max_max) intersectAmount = dmin_max;

        if (max_min) intersectAmount = -dmax_max;
        if (min_max) intersectAmount = -dmin_min;

        return intersectAmount;
    }

    //Copy from Thut Core - api.entity.blockentity.BlockEntityUpdater#intersectsOrAdjacent
    public static boolean intersectsOrAdjacent(final AxisAlignedBB boxA, final AxisAlignedBB boxB)
    {
        return boxA.minX <= boxB.maxX && boxA.maxX >= boxB.minX && boxA.minY <= boxB.maxY && boxA.maxY >= boxB.minY
                && boxA.minZ <= boxB.maxZ && boxA.maxZ >= boxB.minZ;
    }

    //Copy from Thut Core - api.entity.blockentity.BlockEntityUpdater#getIngersect
    private static double intersect(final Direction.Axis axis, final AxisAlignedBB boxA, final AxisAlignedBB boxB,
                                    final AxisAlignedBB boxC)
    {
        switch (axis)
        {
            case X:
                return CageUtil.intersect(boxA.minX, boxB.minX, boxC.minX, boxA.maxX, boxB.maxX, boxC.maxX);
            case Y:
                return CageUtil.intersect(boxA.minY, boxB.minY, boxC.minY, boxA.maxY, boxB.maxY, boxC.maxY);
            case Z:
                return CageUtil.intersect(boxA.minZ, boxB.minZ, boxC.minZ, boxA.maxZ, boxB.maxZ, boxC.maxZ);
            default:
                break;
        }
        return 0;
    }

    public static <T extends ElevatorPartEntity> void handleCollision(T part, List<AxisAlignedBB> collisions, Entity entity){
        if ((part.yRot + 360 % 90) > 5 || part.hasPassenger(entity)) return;
        boolean isServer = !entity.getCommandSenderWorld().isClientSide;
        final boolean isPlayer = entity instanceof PlayerEntity;
        if (isPlayer) isServer = entity instanceof ServerPlayerEntity;

        double dx = 0, dy = 0, dz = 0;
        final Vector3d motionA = part.getDeltaMovement();
        Vector3d motionE = entity.getDeltaMovement();
        final AxisAlignedBB enBoundingBox = entity.getBoundingBox();
        if (isPlayer && isServer){
            final ServerPlayerEntity player = (ServerPlayerEntity) entity;
            dx = player.xCloak - player.xCloakO;
            dy = player.yCloak - player.yCloakO;
            dz = player.zCloak - player.zCloakO;
            motionE = new Vector3d(dx, dy, dz).scale(0.5);
        }

        final Vector3d dif = motionA.subtract(motionE);

        final AxisAlignedBB tes = enBoundingBox.expandTowards(dif);

        final AxisAlignedBB hitTes = tes.inflate(0.1 + dif.length());

        final boolean collided = CageUtil.applyEntityCollision(entity, enBoundingBox, collisions, motionA);

        if (isPlayer && (collided || motionA.y < 0))
        {
            final PlayerEntity player = (PlayerEntity) entity;

            if (isServer)
            {
                final ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                // Meed to set floatingTickCount to prevent being kicked
                serverPlayer.connection.aboveGroundVehicleTickCount = 0;
                serverPlayer.connection.aboveGroundTickCount = 0;
            }

            if (!isServer && (Minecraft.getInstance().options.bobView || JitterPreventer.playerTickTracker
                    .containsKey(player.getUUID())))
            { // This fixes jitter, need a better way to handle this.
                JitterPreventer.playerTickTracker.put(player.getUUID(), (int) (System.currentTimeMillis() % 2000));
                Minecraft.getInstance().options.bobView = false;
            }
            /** This is for clearing jump values on client. */
            if (!isServer) player.getPersistentData().putInt("lastStandTick", player.tickCount);

        }
    }

    public static boolean applyEntityCollision(final Entity entity, final AxisAlignedBB entityBox,
                                               final List<AxisAlignedBB> blockBoxes, final Vector3d ref_motion)
    {
        if (blockBoxes.isEmpty()) return false;

        double dx = 0, dz = 0, dy = 0;
        Vector3d motion_b = entity.getDeltaMovement();

        boolean serverSide = !entity.getCommandSenderWorld().isClientSide;
        final boolean isPlayer = entity instanceof PlayerEntity;
        if (isPlayer) serverSide = entity instanceof ServerPlayerEntity;

        final Vector3d diffV = ref_motion.subtract(motion_b);
        final AxisAlignedBB boundingBox = entityBox;
        if (isPlayer && serverSide)
        {
            final ServerPlayerEntity player = (ServerPlayerEntity) entity;
            dx = player.xCloak - player.xCloakO;
            dy = player.yCloak - player.yCloakO;
            dz = player.zCloak - player.zCloakO;
            motion_b = new Vector3d(dx, dy, dz).scale(0.5);
        }
        final AxisAlignedBB testBox = boundingBox.expandTowards(diffV.x, diffV.y, diffV.z);// .grow(0.1);

        dx = 0;
        dy = 0;
        dz = 0;
        boolean colX = false;
        boolean colY = false;
        boolean colZ = false;

        AxisAlignedBB toUse = testBox;
        final AxisAlignedBB orig = toUse;

        for (final AxisAlignedBB aabb : blockBoxes)
        {
            double dx1 = 0, dy1 = 0, dz1 = 0;
            // Only use ones that actually intersect for this loop
            if (!CageUtil.intersectsOrAdjacent(aabb, toUse)) continue;

            final AxisAlignedBB inter = toUse.intersect(aabb);

            // This is the floor of the box, so mark it as collided
            if (inter.getYsize() == 0 && inter.minY == aabb.maxY) colY = true;

            // This means we don't actually intersect as far as the below checks
            // are concerned
            if (inter.getXsize() == 0 || inter.getYsize() == 0 || inter.getZsize() == 0) continue;

            // System.out.println("X");
            dx1 = CageUtil.intersect(Direction.Axis.X, inter, toUse, aabb);
            // System.out.println("Y");
            dy1 = CageUtil.intersect(Direction.Axis.Y, inter, toUse, aabb);
            // System.out.println("Z");
            dz1 = CageUtil.intersect(Direction.Axis.Z, inter, toUse, aabb);

            // Take the minimum of x and z
            if (dx1 != 0 && dz1 != 0)
            {
                final boolean max = Math.abs(dx1) > Math.abs(dz1);
                if (max) dx1 = 0;
                else dz1 = 0;
            }
            // Take the minimum of y and x
            if (dy1 != 0 && dx1 != 0)
            {
                final boolean max = Math.abs(dx1) > Math.abs(dy1);
                if (max) dx1 = 0;
                else dy1 = 0;
            }
            // Take the minimum of y and z
            if (dy1 != 0 && dz1 != 0)
            {
                final boolean max = Math.abs(dz1) > Math.abs(dy1);
                if (max) dz1 = 0;
                else dy1 = 0;
            }

            // If no y movement, but x or z, see if we should step up instead.
            if (dy1 == 0 && !(dz1 == 0 && dx1 == 0))
            {
                dy = inter.maxY - toUse.minY;
                if (dy >= 0 && dy < entity.maxUpStep)
                {
                    boolean valid = true;
                    // check if none of the other boxes disagree with the step
                    for (final AxisAlignedBB aabb2 : blockBoxes)
                    {
                        if (aabb2 == aabb) continue;
                        if (aabb2.intersects(toUse))
                        {
                            valid = false;
                            break;
                        }
                    }
                    if (valid)
                    {
                        dx1 = 0;
                        dz1 = 0;
                        dy1 = dy;
                    }
                }
            }

            colX = colX || dx1 != 0;
            colY = colY || dy1 != 0;
            colZ = colZ || dz1 != 0;

            toUse = toUse.move(dx1, dy1, dz1);
        }

        dx = toUse.minX - orig.minX;
        dy = toUse.minY - orig.minY;
        dz = toUse.minZ - orig.minZ;

        final boolean collided = colX || colY || colZ;

        // If entity has collided, adjust motion accordingly.
        if (collided)
        {
            motion_b = entity.getDeltaMovement();
            if (colY)
            {
                final Vector3d motion = new Vector3d(0, dy, 0);
                entity.move(MoverType.SELF, motion);
                dy = ref_motion.y;
            }
            else dy = motion_b.y;
            if (colX)
            {
                final Vector3d motion = new Vector3d(dx, 0, 0);
                entity.move(MoverType.SELF, motion);
                dx = ref_motion.x;
            }
            else dx = 0.9 * motion_b.x;
            if (colZ)
            {
                final Vector3d motion = new Vector3d(0, 0, dz);
                entity.move(MoverType.SELF, motion);
                dz = ref_motion.z;
            }
            else dz = 0.9 * motion_b.z;
            entity.setDeltaMovement(dx, dy, dz);

            if (colY)
            {
                entity.setOnGround(true);
                entity.causeFallDamage(entity.fallDistance, 0);
                entity.fallDistance = 0;
            }
        }
        return collided;
    }
}

package lausiv1024.util;

import lausiv1024.entity.CageEntity;
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

public class CageUtil {
    public static void applyCollision(CageEntity cage){
        World world = cage.getCommandSenderWorld();
        Vector3d cagePos = cage.position();
        Vector3d cageMotion = cagePos.subtract(cage.getPrevPos());
        List<Entity> entitiesInBounds = world.getEntitiesOfClass(Entity.class,
                cage.getBoundingBox().inflate(1.5),
                cage::canCollideWith);


        for (Entity e : entitiesInBounds){
            double dx = 0, dy = 0, dz = 0;
            AxisAlignedBB entityBB = e.getBoundingBox();
            Vector3d entityMotion = e.getDeltaMovement();

            boolean isServer = !e.getCommandSenderWorld().isClientSide;
            boolean isPlayer = e instanceof PlayerEntity;
            if (isPlayer) isServer = e instanceof ServerPlayerEntity;


            entityMotion = entityMotion.subtract(cageMotion);

            if (isPlayer && isServer){
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) e;
                dx = serverPlayer.xCloak - serverPlayer.xCloakO;
                dy = serverPlayer.yCloak - serverPlayer.yCloakO;
                dz = serverPlayer.zCloak - serverPlayer.zCloakO;
                entityMotion = new Vector3d(dx, dy, dz).scale(0.5);
            }
            final Vector3d dif = cageMotion.subtract(entityMotion);

            AxisAlignedBB testes = entityBB.expandTowards(dif.x, dif.y, dif.z);

            dx = 0; dy = 0; dz = 0;

            boolean colX = false, colY = false, colZ = false;

            AxisAlignedBB toUse = testes;
            final AxisAlignedBB orig = toUse;

            List<AxisAlignedBB> collisions = new ArrayList<>();
            collisions.add(getFloorCollision(cage.getX(), cage.getY(), cage.getZ()));
            collisions.add(getRoofCollision(cage.getX(), cage.getY(), cage.getZ()));

            for (final AxisAlignedBB aabb : collisions){
                double dx1 = 0, dy1 = 0, dz1 = 0;
                if(!CageUtil.intersectsOrAdjacent(aabb, toUse))continue;
                AxisAlignedBB inter = toUse.intersect(aabb);

                if (inter.getYsize() == 0 && inter.minY == aabb.maxY) colY = true;

                if (inter.getXsize() == 0 || inter.getYsize() == 0 || inter.getZsize() == 0) continue;

                dx1 = CageUtil.intersect(Direction.Axis.X, inter, toUse, aabb);

                dy1 = CageUtil.intersect(Direction.Axis.Y, inter, toUse, aabb);

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
                    if (dy >= 0 && dy < e.maxUpStep)
                    {
                        boolean valid = true;
                        // check if none of the other boxes disagree with the step
                        for (final AxisAlignedBB aabb2 : collisions)
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

            if (collided)
            {
                entityMotion = e.getDeltaMovement();
                if (colY)
                {
                    final Vector3d motion = new Vector3d(0, dy, 0);
                    e.move(MoverType.SELF, motion);
                    dy = cageMotion.y;
                }
                else dy = entityMotion.y;
                if (colX)
                {
                    final Vector3d motion = new Vector3d(dx, 0, 0);
                    e.move(MoverType.SELF, motion);
                    dx = cageMotion.x;
                }
                else dx = 0.9 * entityMotion.x;
                if (colZ)
                {
                    final Vector3d motion = new Vector3d(0, 0, dz);
                    e.move(MoverType.SELF, motion);
                    dz = cageMotion.z;
                }
                else dz = 0.9 * entityMotion.z;
                e.setDeltaMovement(dx, dy, dz);

                if (colY)
                {
                    e.setOnGround(true);
                    e.causeFallDamage(e.fallDistance, 0);
                    e.fallDistance = 0;
                }
            }
        }
    }

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

    public static AxisAlignedBB getFloorCollision(double x, double y, double z){
        return new AxisAlignedBB(x + 24.0 / 16.0, y - 8.0 / 16.0, z - 29.0 / 16
                , x - 24.0 / 16.0, y, z + 25.0 / 16);
    }

    public static AxisAlignedBB getRoofCollision(double x, double y, double z){
        return new AxisAlignedBB(x + 25.0 / 16.0, y + 52.0 / 16.0, z - 27.0 / 16.0,
                x - 25.0 / 16.0, y + 60.0 / 16.0, z + 28.0 / 16.0);
    }
}

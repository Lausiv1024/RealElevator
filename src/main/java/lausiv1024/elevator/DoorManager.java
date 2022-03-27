package lausiv1024.elevator;

import lausiv1024.RealElevator;
import lausiv1024.entity.CageEntity;
import lausiv1024.entity.ElevatorPartEntity;
import lausiv1024.entity.doors.AbstractDoorEntity;
import lausiv1024.util.ObjHelper;
import lausiv1024.util.REMath;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class DoorManager {
    private AbstractDoorEntity[] cageDoors;
    private int doorTick;
    private int doorSequence = 0;
    DoorType doorType = DoorType.CO;

    public DoorManager(AbstractDoorEntity[] cageDoors){
        this.cageDoors = cageDoors;
    }

    public void doorTick(AbstractElevator parent, World world){
        int doorState = parent.doorState;
        CageEntity en = (CageEntity) ElevatorPartEntity.getEntityFromUUID(parent.ref.cage, world);

        ObjHelper.checkNullAndExec(en, () -> RealElevator.LOGGER.info(en.position()));

        switch (doorState){
            case 1:
                if (doorType == DoorType.CO){
                    int minIndex;
                    if (parent.facingMain.getAxis() == Direction.Axis.X){
                        minIndex = REMath.getMinSide(cageDoors[0].position().z, cageDoors[1].position().z);
                        AbstractDoorEntity d1 = cageDoors[minIndex];
                        AbstractDoorEntity d2 = cageDoors[Math.abs(minIndex - 1)];
                        double cz = ElevatorPartEntity.getEntityFromUUID(parent.ref.cage, world).position().z;
                        if (doorTick == 0) {

                        }
                        if (doorTick == 102){
                            d1.setDeltaMovement(Vector3d.ZERO);
                            d2.setDeltaMovement(Vector3d.ZERO);
                            parent.doorState = 2;
                            doorTick = 0;
                        }

                    }else{
                        minIndex = REMath.getMinSide(cageDoors[0].position().x, cageDoors[1].position().x);
                        AbstractDoorEntity d1 = cageDoors[minIndex];
                        AbstractDoorEntity d2 = cageDoors[Math.abs(minIndex - 1)];
                        double cx = ElevatorPartEntity.getEntityFromUUID(parent.ref.cage, world).position().x;
                        if (doorTick == 0) {
                            d1.setDeltaMovement(-0.05, 0, 0);
                            d2.setDeltaMovement(0.05, 0, 0);
                        }
                        if (doorTick == 102){
                            d1.setDeltaMovement(Vector3d.ZERO);
                            d2.setDeltaMovement(Vector3d.ZERO);
                            parent.doorState = 2;
                            doorTick = 0;
                        }
                    }
                }
                doorTick++;
                break;
            case 3:
                break;
        }
    }

    private Vector3d[] getLocalDoorPos(Vector3d baseVal, Vector3d... doorPoses){
        List<Vector3d> l = new ArrayList<>();
        for (Vector3d v : doorPoses){
            Vector3d tmp = new Vector3d(v.x - baseVal.x, v.y - baseVal.y, v.z - baseVal.z);
            l.add(tmp);
        }
        return l.toArray(new Vector3d[0]);
    }

    private void setDoorMotion(double speed){
        Vector3d vector3d = new Vector3d(0.19, 0, 0.19);
        for (AbstractDoorEntity door : cageDoors){
            Direction d = door.getDirection1();
            AxisAlignedBB bb = door.getBoundingBox();
            World level = door.level;

            Vector3d idou = vector3d.multiply(d.getStepX(), 0, d.getStepZ());
            //方角に応じて動作を変換する (まだできてない)
            Vector3d motion = Vector3d.ZERO;
            if (d.getAxis() == Direction.Axis.X) motion = new Vector3d(0, 0, speed);
            else if (d.getAxis() == Direction.Axis.Z) motion = new Vector3d(speed, 0, 0);

            AxisAlignedBB moved = bb.move(idou).inflate(-0.005);//隣のドアを拾わないよう少し小さくする
            List<AbstractDoorEntity> doorEntities = level.getEntitiesOfClass(AbstractDoorEntity.class, moved, AbstractDoorEntity::getLand);
            final Vector3d finalMotion = motion;
            doorEntities.forEach(dd -> dd.setDeltaMovement(finalMotion));
        }
    }

    private Vector3d getLocalDoorPos(Vector3d baseval, Vector3d doorPos){
        return new Vector3d(doorPos.x - baseval.x, doorPos.y - baseval.y, doorPos.z - baseval.z);
    }

    public void setCageDoors(AbstractDoorEntity[] cageDoors) {
        this.cageDoors = cageDoors;
    }

    public int getDoorTick() {
        return doorTick;
    }

    public DoorType getDoorType() {
        return doorType;
    }

    public enum DoorType{
        CO, //2枚中央開き
        _2S, //2枚片開き
        _2CO //4枚中央開き
    }
}

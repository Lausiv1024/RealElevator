package lausiv1024.elevator;

import lausiv1024.RealElevator;
import lausiv1024.entity.CageEntity;
import lausiv1024.entity.doors.AbstractDoorEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DoorManager {
    private static final Logger LOGGER = LogManager.getLogger("DoorManager");


    private int doorTick;
    private int doorSequence = 0;
    DoorType doorType = DoorType.CO;
    CageEntity parentCage;
    double posO = 0;

    public DoorManager(CageEntity parent){
        this.parentCage = parent;
    }

    public void doorTick(AbstractElevator parent, World world){
        int doorState = parent.doorState;

        switch (doorState){
            case 1:
                if (doorType == DoorType.CO){
                    switch (doorSequence){
                        case 0:
                            doorTick = 0;
                            if (posO > 1.05){
                                doorSequence = 1;
                                break;
                            }

                            posO = setDoorMotion(0.4 / 20.0);
                            break;
                        case 1:
                            if (posO > 1.10){
                                doorSequence = 2;
                                doorTick = 0;
                                setDoorMotion(0);
                                break;
                            }

                            if (posO <= 1.05){
                                doorSequence = 0;
                            }

                            posO = setDoorMotion(0.1 / 20.0);
                            break;
                        case 2:
                            doorTick++;
                            RealElevator.LOGGER.info("doorTick > {}", doorTick);

                            if (doorTick >= 11){
                                parent.doorState = 2;
                                doorSequence = 0;
                                doorTick = 0;
                                break;
                            }
                            break;
                    }
                }
                break;
            case 3:
                if (doorType == DoorType.CO){
                    if (doorSequence == 0){
                        doorTick = 0;
                        posO = setDoorMotion(0);
                        if (posO < 0.475){
                            doorSequence = 1;
                            break;
                        }

                        posO = setDoorMotion(-0.35 / 20.0);
                    }else if (doorSequence == 1){
                        if (posO <= 0.4){
                            //????????????????????????
                            setDoorMotion(0);
                            setDoorPos(0.375);

                            //parent.doorState = 0;
                            doorTick = 0;
                            doorSequence = 2;
                            return;
                        }
                        posO = setDoorMotion(-0.1 / 20.0);
                    }else if (doorSequence == 2){
                        if (doorTick >= 21){
                            doorTick = 0;
                            parent.doorState = 0;
                            return;
                        }
                        doorTick++;
                    }
                }
                break;
            default:
                setDoorMotion(0);
                doorSequence = 0;
        }
    }

//    private Vector3d[] getLocalDoorPos(Vector3d baseVal, Vector3d... doorPoses){
//        List<Vector3d> l = new ArrayList<>();
//        for (Vector3d v : doorPoses){
//            Vector3d tmp = new Vector3d(v.x - baseVal.x, v.y - baseVal.y, v.z - baseVal.z);
//            l.add(tmp);
//        }
//        return l.toArray(new Vector3d[0]);
//    }

    //???????????????????????????????????????????????????
    //???????????????????????????????????????
    private double setDoorMotion(double speed){
        double abd = 0;
        for (AbstractDoorEntity door : parentCage.getDoors(false)){
            //?????????????????????????????????

            Vector3d local = getLocalDoorPos(parentCage.position(), door.position());

            Direction.Axis doorAxis = door.getDirection1().getAxis();

            int a = 0;
            Vector3d mot = Vector3d.ZERO;

            switch (doorAxis){
                case X:
                    a = local.z < 0 ? -1 : 1;
                    mot = new Vector3d(0, door.getDeltaMovement().y, speed * a);
                    abd = getLocalDoorPos(parentCage.position(), door.position()).z;
                    break;
                case Z:
                    a = local.x < 0 ? -1 : 1;
                    mot = new Vector3d(speed * a, door.getDeltaMovement().y, 0);
                    abd = getLocalDoorPos(parentCage.position(), door.position()).x;
            }
            door.setDeltaMovement(mot);
            if (door.getLand()) return 0;


            Vector3d vector3d = new Vector3d(0.19, 0, 0.19);
            Direction d = door.getDirection1();
            AxisAlignedBB bb = door.getBoundingBox();
            World level = door.level;

            Vector3d idou = vector3d.multiply(d.getStepX(), 0, d.getStepZ());
            Vector3d motion = Vector3d.ZERO;
            if (d.getAxis() == Direction.Axis.X) motion = new Vector3d(0, door.getDeltaMovement().y /*?????????*/, speed * a);
            else if (d.getAxis() == Direction.Axis.Z) motion = new Vector3d(speed * a, door.getDeltaMovement().y, 0);

            AxisAlignedBB moved = bb.move(idou).inflate(-0.005);//??????????????????????????????????????????????????????
            List<AbstractDoorEntity> doorEntities = level.getEntitiesOfClass(AbstractDoorEntity.class, moved, AbstractDoorEntity::getLand);
            final Vector3d finalMotion = motion.multiply(1, 0, 1);
            doorEntities.forEach(dd -> dd.setDeltaMovement(finalMotion));
        }
        return abd;
    }

    private void setDoorPos(double pos1){
        for (AbstractDoorEntity door : parentCage.getDoors(false)){
            //??????????????????????????????
            Vector3d local = getLocalDoorPos(parentCage.position(), door.position());
            Direction.Axis doorAxis = door.getDirection1().getAxis();
            double a = 0;
            Vector3d pos = Vector3d.ZERO;
            Vector3d vector3d = new Vector3d(0.19, 0, 0.19);
            Direction d = door.getDirection1();
            AxisAlignedBB bb = door.getBoundingBox();
            World level = door.level;

            switch (doorAxis){
                case X:
                    a = local.z < 0.0 ? -1.0 : 1.0;
                    pos = getGlobalPos(parentCage.position(), new Vector3d(local.x, 0., pos1 * a));
                    break;
                case Z:
                    a = local.x < 0. ? -1.0 : 1.0;
                    pos = getGlobalPos(parentCage.position(), new Vector3d(pos1 * a, 0., local.z));
            }
            door.setPos(pos);

            if (door.getLand()) return;

            Vector3d idou = vector3d.multiply(d.getStepX(), 0., d.getStepZ());
            Vector3d pos11 = Vector3d.ZERO;

            if (d.getAxis() == Direction.Axis.X) pos11 = getGlobalPos(parentCage.position(), new Vector3d(local.x + 0.19, 0, pos1 * a));
            else if (d.getAxis() == Direction.Axis.Z) pos11 = getGlobalPos(parentCage.position(), new Vector3d(pos1 * a, 0, local.z + 0.19));

            AxisAlignedBB moved = bb.move(idou).inflate(-0.07);
            List<AbstractDoorEntity> doorEntities = level.getEntitiesOfClass(AbstractDoorEntity.class, moved, AbstractDoorEntity::getLand);
            final Vector3d finalPos = pos11;

            doorEntities.forEach(dd -> dd.setPos(finalPos));
        }
    }

    private Vector3d getLocalDoorPos(Vector3d baseval, Vector3d doorPos){
        return new Vector3d(doorPos.x - baseval.x, doorPos.y - baseval.y, doorPos.z - baseval.z);
    }

    private Vector3d getGlobalPos(Vector3d base, Vector3d localDPos){
        return new Vector3d(base.x + localDPos.x, base.y + localDPos.y, base.z + localDPos.z);
    }

    public DoorType getDoorType() {
        return doorType;
    }

    public void setParentCage(CageEntity parentCage) {
        this.parentCage = parentCage;
    }

    public CompoundNBT save(){
        CompoundNBT nbt = new CompoundNBT();
        nbt.putInt("DoorTick", doorTick);
        nbt.putInt("DoorSequence", doorSequence);
        nbt.putString("DoorType", doorType.name());
        return nbt;
    }

    public static DoorManager fromNBT(CompoundNBT nbt, CageEntity parent){
        DoorManager mgr = new DoorManager(parent);
        mgr.doorTick = nbt.getInt("DoorTick");
        mgr.doorSequence = nbt.getInt("DoorSequence");
        try {
            mgr.doorType = DoorType.valueOf(nbt.getString("DoorType"));
        }catch (IllegalArgumentException e){
            mgr.doorType = DoorType.CO;
        }
        return mgr;
    }

    public enum DoorType{
        CO, //2???????????????
        _2S, //2????????????
        _2CO //4???????????????
    }
}

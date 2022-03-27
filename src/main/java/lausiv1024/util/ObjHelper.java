package lausiv1024.util;

import lausiv1024.RealElevator;

import java.util.Comparator;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class ObjHelper {
    public static boolean checkNullAndExec(Object targ, Runnable cmd){
        RealElevator.LOGGER.info("Null Check");
        if (targ == null) return false;
        RealElevator.LOGGER.info("{} is Not Null", targ.getClass().toString());
        cmd.run();
        return true;
    }
}

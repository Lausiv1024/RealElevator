package lausiv1024.util;

public class REMath {
    public static double toPerTick(double perSec){
        return toPerTick(perSec, 20);
    }

    public static double toPerTick(double perSec, int tickCount){
        return perSec /(double) tickCount;
    }

    public static double toPerSec(double perSec){
        return toPerSec(perSec, 20);
    }

    public static double toPerSec(double perSec, int tickCount){
        return perSec * (double) tickCount;
    }
}

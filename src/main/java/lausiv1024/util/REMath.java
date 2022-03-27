package lausiv1024.util;

public class REMath {

    public static int getMinSide(double d1, double d2){
        double minVal = Math.min(d1, d2);
        if (d1 == minVal) return 0;
        if (d2 == minVal) return 1;
        return 0;
    }
}

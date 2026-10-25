package net.lausiv1024.realelevator.core.car;

/**
 * 水平な面の外向きの法線（面を見る人は、この向きの反対を向いている）。
 */
public enum Normal {
    POS_X(1, 0),
    NEG_X(-1, 0),
    POS_Z(0, 1),
    NEG_Z(0, -1);

    private final int x;
    private final int z;

    Normal(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    /** 面を見る人から見て右の向きの X 成分。 */
    public int rightX() {
        return z;
    }

    /** 面を見る人から見て右の向きの Z 成分。 */
    public int rightZ() {
        return -x;
    }

    /** {@link Box#rotateQuarterTurns} と同じ向きに、Y軸まわりに90°ずつ回す（(x, z) → (−z, x)）。 */
    public Normal rotateQuarterTurns(int quarterTurns) {
        int nx = x;
        int nz = z;
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
            int t = nx;
            nx = -nz;
            nz = t;
        }
        return of(nx, nz);
    }

    private static Normal of(int x, int z) {
        for (Normal normal : values()) {
            if (normal.x == x && normal.z == z) {
                return normal;
            }
        }
        throw new IllegalArgumentException(x + "," + z);
    }
}

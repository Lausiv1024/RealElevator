package net.lausiv1024.realelevator.core.car;

/**
 * 軸に平行な直方体。単位はブロック。
 */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
    public Box {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("min must not exceed max: " + minX + "," + minY + "," + minZ
                    + " / " + maxX + "," + maxY + "," + maxZ);
        }
    }

    /** px（1/16ブロック）単位の座標から作る。 */
    public static Box ofPixels(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double p = CarModel.PIXELS_PER_BLOCK;
        return new Box(minX / p, minY / p, minZ / p, maxX / p, maxY / p, maxZ / p);
    }

    /**
     * Y軸まわりに90°ずつ回す。1回転ごとに (x, z) → (−z, x) となり、+Z の向きが南→西→北→東と回る。
     */
    public Box rotateQuarterTurns(int quarterTurns) {
        Box box = this;
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
            box = new Box(-box.maxZ, box.minY, box.minX, -box.minZ, box.maxY, box.maxX);
        }
        return box;
    }

    public Box move(double dx, double dy, double dz) {
        return new Box(minX + dx, minY + dy, minZ + dz, maxX + dx, maxY + dy, maxZ + dz);
    }

    /** 境界が接しているだけの場合は交差とみなさない。 */
    public boolean intersects(Box other) {
        return minX < other.maxX && maxX > other.minX
                && minY < other.maxY && maxY > other.minY
                && minZ < other.maxZ && maxZ > other.minZ;
    }

    public double sizeX() {
        return maxX - minX;
    }

    public double sizeY() {
        return maxY - minY;
    }

    public double sizeZ() {
        return maxZ - minZ;
    }
}

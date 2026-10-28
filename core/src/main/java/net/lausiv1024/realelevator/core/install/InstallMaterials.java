package net.lausiv1024.realelevator.core.install;

/**
 * 設置するときに使う材料。ガイドレールは{@value #RAILS_PER_IRON}ブロックごとに鉄インゴット1つ（端数は切り上げ）、乗場の扉（三方枠・ボタン・表示器を含む）は
 * 1つごとに鉄インゴット{@value #IRON_PER_DOOR}つとレッドストーン{@value #REDSTONE_PER_DOOR}つ。かごと頂部の制御盤の分は、設置用ブロックのレシピに含める。
 * 解体すると、使った材料をすべて返す。
 *
 * @param iron     鉄インゴットの数
 * @param redstone レッドストーンの数
 */
public record InstallMaterials(int iron, int redstone) {
    public static final InstallMaterials NONE = new InstallMaterials(0, 0);
    public static final int RAILS_PER_IRON = 2;
    public static final int IRON_PER_DOOR = 4;
    public static final int REDSTONE_PER_DOOR = 1;

    /** ガイドレール railBlocks ブロックと、乗場の扉 landingDoors 個の分。 */
    public static InstallMaterials of(int railBlocks, int landingDoors) {
        int railIron = (railBlocks + RAILS_PER_IRON - 1) / RAILS_PER_IRON;
        return new InstallMaterials(railIron + landingDoors * IRON_PER_DOOR, landingDoors * REDSTONE_PER_DOOR);
    }

    public boolean isEmpty() {
        return iron == 0 && redstone == 0;
    }
}

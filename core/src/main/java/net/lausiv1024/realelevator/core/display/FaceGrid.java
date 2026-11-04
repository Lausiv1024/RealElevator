package net.lausiv1024.realelevator.core.display;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.Normal;

/**
 * 水平な向きの面の上に、細かいドット（表示器の文字やボタンの図柄）を並べるための格子。
 *
 * <p>列は面を見る人から見て左から右、行は上から下へ数える。ドットは面からわずかに浮かせ、面とちらつかないようにする。
 *
 * @param normal   面の外向きの法線
 * @param plane    面の位置（法線の軸の座標）[ブロック]
 * @param left     格子の左端の、水平の座標（法線が ±Z なら X、±X なら Z）[ブロック]
 * @param top      格子の上端の Y [ブロック]
 * @param dotSize  ドット1つの大きさ [ブロック]
 */
public record FaceGrid(Normal normal, double plane, double left, double top, double dotSize) {
    /** ドットが面から浮く量。 */
    public static final double LIFT = 0.002;

    /** 見る人から見て column 列目・row 行目のドット。中央に寄せるため、半端な位置（0.5 ずれ）も指せる。 */
    public Box dot(double column, double row) {
        return cell(column, row, 1, 1);
    }

    /** 見る人から見て (column, row) から columns × rows 個分の長方形。 */
    public Box cell(double column, double row, int columns, int rows) {
        double y1 = top - row * dotSize;
        double y0 = y1 - rows * dotSize;
        double h0 = left + column * dotSize * sign();
        double h1 = h0 + columns * dotSize * sign();
        double p0 = plane;
        double p1 = plane + LIFT * (normal.x() + normal.z());
        double minH = Math.min(h0, h1);
        double maxH = Math.max(h0, h1);
        double minP = Math.min(p0, p1);
        double maxP = Math.max(p0, p1);
        return normal.x() != 0
                ? new Box(minP, y0, minH, maxP, y1, maxH)
                : new Box(minH, y0, minP, maxH, y1, maxP);
    }

    /** 見る人の右が、水平の座標の増える向きなら 1。 */
    private int sign() {
        return normal.x() != 0 ? normal.rightZ() : normal.rightX();
    }
}

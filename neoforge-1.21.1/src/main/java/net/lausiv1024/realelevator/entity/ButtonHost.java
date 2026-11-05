package net.lausiv1024.realelevator.entity;

import java.util.List;

import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;
import net.minecraft.world.phys.AABB;

/**
 * ボタンを持つエンティティ（かごの操作盤、乗場のボタン）。ボタンは {@link #buttons()} の並びの番号で指す。
 */
public interface ButtonHost {
    /** ボタンの一覧（かごのローカル座標）。 */
    List<Button> buttons();

    /** index 番のボタンの、今の位置での箱（ワールド座標）。 */
    AABB buttonBox(int index);

    /** index 番のボタンの応答灯がついているかどうか。 */
    boolean isButtonLit(int index);

    /** index 番のボタンを押す。サーバーで呼ぶ。 */
    void pressButton(int index);

    /** index 番のボタンを押したままにしている（使用キーを押したままのあいだの繰り返し）。サーバーで呼ぶ。既定では何もしない。 */
    default void holdButton(int index) {
    }

    /** index 番のボタンが、押したままのあいだ繰り返し知らせるものか（開ボタンと乗場のボタン）。 */
    static boolean isHoldable(Button button) {
        return switch (button.kind()) {
            case OPEN, HALL_UP, HALL_DOWN -> true;
            default -> false;
        };
    }

    /** index 番のボタンの面に描く階名（行先ボタンだけ）。 */
    default String buttonLabel(int index) {
        return "";
    }
}

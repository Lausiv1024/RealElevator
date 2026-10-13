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
}

package net.lausiv1024.realelevator.client;

import org.joml.Quaternionf;

import com.mojang.blaze3d.vertex.PoseStack;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.Normal;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.entity.ButtonHost;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;

/**
 * 行先ボタンに視線を合わせたとき、階名が{@value #MIN_LENGTH}文字以上なら、ネームタグのようにボタンの上へ浮かべる
 * （ボタンの面には小さくしか描けないため）。ドットとフォントのどちらの描き方でも浮かべる。
 */
final class ButtonTags {
    static final int MIN_LENGTH = 3;
    /** 浮かべる位置：ボタンの面から手前へ、と上へ [ブロック]。 */
    private static final double OUT = 0.06;
    private static final double UP = 0.1;

    private ButtonTags() {
    }

    /** host（ボタンの座標を quarterTurns 回して描くもの）の、視線の先のボタンの階名を浮かべる。 */
    static <T extends Entity & ButtonHost> void draw(PoseStack poseStack, MultiBufferSource buffers, T host, int quarterTurns, Quaternionf camera) {
        int index = ButtonInput.focusedIndex(host);
        if (index < 0 || index >= host.buttons().size()) {
            return;
        }
        ButtonLayout.Button button = host.buttons().get(index);
        String label = host.buttonLabel(index);
        if (button.kind() != ButtonLayout.Kind.CAR_CALL || label.length() < MIN_LENGTH) {
            return;
        }
        Box box = button.box().rotateQuarterTurns(quarterTurns);
        Normal normal = button.normal().rotateQuarterTurns(quarterTurns);
        WorldText.drawTag(poseStack, buffers,
                (box.minX() + box.maxX()) / 2 + normal.x() * OUT,
                box.maxY() + UP,
                (box.minZ() + box.maxZ()) / 2 + normal.z() * OUT, label, camera);
    }
}

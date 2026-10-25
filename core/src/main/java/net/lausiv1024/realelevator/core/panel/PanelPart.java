package net.lausiv1024.realelevator.core.panel;

import net.lausiv1024.realelevator.core.car.Box;

/**
 * 操作盤の、ボタン以外の部品（板・黒い帯・表示窓・車椅子マーク）。座標はかごのローカル座標（乗場なら着床したときの位置）。
 */
public record PanelPart(Type type, Box box) {
    public enum Type {
        /** ステンレスやブロンズの板。 */
        PLATE,
        /** 板の上の黒い帯。 */
        BAND,
        /** 表示器の窓（黒）。表示器のドットはこの上に描く。 */
        WINDOW,
        /** 車椅子マークの青い地。 */
        MARK,
        /** 車椅子マークの白い図柄（細かいドット）。 */
        MARK_SYMBOL
    }
}

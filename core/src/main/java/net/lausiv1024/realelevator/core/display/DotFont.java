package net.lausiv1024.realelevator.core.display;

import java.util.Map;

/**
 * 階床表示器の 3×5 ドットの文字。1ドットは 1px（1/16ブロック）。
 *
 * <p>階名に使う数字と、地下・屋上・中間階などを表す英字の一部だけを持つ。
 */
public final class DotFont {
    public static final int WIDTH = 3;
    public static final int HEIGHT = 5;

    private static final Map<Character, String[]> GLYPHS = Map.ofEntries(
            Map.entry('0', new String[] {"###", "#.#", "#.#", "#.#", "###"}),
            Map.entry('1', new String[] {".#.", "##.", ".#.", ".#.", "###"}),
            Map.entry('2', new String[] {"###", "..#", "###", "#..", "###"}),
            Map.entry('3', new String[] {"###", "..#", "###", "..#", "###"}),
            Map.entry('4', new String[] {"#.#", "#.#", "###", "..#", "..#"}),
            Map.entry('5', new String[] {"###", "#..", "###", "..#", "###"}),
            Map.entry('6', new String[] {"###", "#..", "###", "#.#", "###"}),
            Map.entry('7', new String[] {"###", "..#", "..#", ".#.", ".#."}),
            Map.entry('8', new String[] {"###", "#.#", "###", "#.#", "###"}),
            Map.entry('9', new String[] {"###", "#.#", "###", "..#", "###"}),
            Map.entry('B', new String[] {"##.", "#.#", "##.", "#.#", "##."}),
            Map.entry('F', new String[] {"###", "#..", "##.", "#..", "#.."}),
            Map.entry('G', new String[] {"###", "#..", "#.#", "#.#", "###"}),
            Map.entry('L', new String[] {"#..", "#..", "#..", "#..", "###"}),
            Map.entry('M', new String[] {"#.#", "###", "###", "#.#", "#.#"}),
            Map.entry('P', new String[] {"##.", "#.#", "##.", "#..", "#.."}),
            Map.entry('R', new String[] {"##.", "#.#", "##.", "#.#", "#.#"}),
            Map.entry('-', new String[] {"...", "...", "###", "...", "..."}));

    private DotFont() {
    }

    public static boolean supports(char c) {
        return GLYPHS.containsKey(Character.toUpperCase(c));
    }

    /**
     * 文字 c のドット (x, y) が点灯するかどうか。x は左から、y は上から数える。対応していない文字は点灯しない。
     */
    public static boolean isLit(char c, int x, int y) {
        String[] glyph = GLYPHS.get(Character.toUpperCase(c));
        return glyph != null && x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && glyph[y].charAt(x) == '#';
    }
}

package net.lausiv1024.realelevator.core.display;

import java.util.Map;

/**
 * 主操作盤の表示窓などに使う、5×7 ドットの文字。ドットの大きさは使う側が決める（主操作盤の表示窓では 0.5px）。
 *
 * <p>{@link DotFont} と同じ文字（数字と、地下・屋上・中間階などを表す英字の一部）を持つ。
 */
public final class LargeDotFont {
    public static final int WIDTH = 5;
    public static final int HEIGHT = 7;

    private static final Map<Character, String[]> GLYPHS = Map.ofEntries(
            Map.entry('0', new String[] {".###.", "#...#", "#..##", "#.#.#", "##..#", "#...#", ".###."}),
            Map.entry('1', new String[] {"..#..", ".##..", "..#..", "..#..", "..#..", "..#..", ".###."}),
            Map.entry('2', new String[] {".###.", "#...#", "....#", "...#.", "..#..", ".#...", "#####"}),
            Map.entry('3', new String[] {"#####", "...#.", "..#..", "...#.", "....#", "#...#", ".###."}),
            Map.entry('4', new String[] {"...#.", "..##.", ".#.#.", "#..#.", "#####", "...#.", "...#."}),
            Map.entry('5', new String[] {"#####", "#....", "####.", "....#", "....#", "#...#", ".###."}),
            Map.entry('6', new String[] {"..##.", ".#...", "#....", "####.", "#...#", "#...#", ".###."}),
            Map.entry('7', new String[] {"#####", "....#", "...#.", "..#..", ".#...", ".#...", ".#..."}),
            Map.entry('8', new String[] {".###.", "#...#", "#...#", ".###.", "#...#", "#...#", ".###."}),
            Map.entry('9', new String[] {".###.", "#...#", "#...#", ".####", "....#", "...#.", ".##.."}),
            Map.entry('B', new String[] {"####.", "#...#", "#...#", "####.", "#...#", "#...#", "####."}),
            Map.entry('F', new String[] {"#####", "#....", "#....", "####.", "#....", "#....", "#...."}),
            Map.entry('G', new String[] {".###.", "#...#", "#....", "#.###", "#...#", "#...#", ".####"}),
            Map.entry('L', new String[] {"#....", "#....", "#....", "#....", "#....", "#....", "#####"}),
            Map.entry('M', new String[] {"#...#", "##.##", "#.#.#", "#.#.#", "#...#", "#...#", "#...#"}),
            Map.entry('P', new String[] {"####.", "#...#", "#...#", "####.", "#....", "#....", "#...."}),
            Map.entry('R', new String[] {"####.", "#...#", "#...#", "####.", "#.#..", "#..#.", "#...#"}),
            Map.entry('-', new String[] {".....", ".....", ".....", "#####", ".....", ".....", "....."}));

    private LargeDotFont() {
    }

    public static boolean supports(char c) {
        return GLYPHS.containsKey(Character.toUpperCase(c));
    }

    /** 文字 c のドット (x, y) が点灯するかどうか。x は左から、y は上から数える。対応していない文字は点灯しない。 */
    public static boolean isLit(char c, int x, int y) {
        String[] glyph = GLYPHS.get(Character.toUpperCase(c));
        return glyph != null && x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT && glyph[y].charAt(x) == '#';
    }
}

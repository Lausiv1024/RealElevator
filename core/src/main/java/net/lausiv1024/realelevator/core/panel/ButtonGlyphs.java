package net.lausiv1024.realelevator.core.panel;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.core.car.Box;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.display.DotFont;
import net.lausiv1024.realelevator.core.display.FaceGrid;
import net.lausiv1024.realelevator.core.panel.ButtonLayout.Button;

/**
 * ボタンの面の図柄。行先ボタンは階名、開・閉ボタンは ◀|▶・▶|◀、開延長は ◀|▶ と「＋」、乗場のボタンは矢印、非常呼びは「SOS」。
 * 応答灯がつくと、図柄そのものが光る。
 *
 * <p>図柄だけは、1px を4分割した細かいドット（1/64 ブロック）で描く（1ブロック＝16px の原則の例外）。
 * 3px角のボタンは 12×12、2px角は 8×8 の格子になる。
 */
public final class ButtonGlyphs {
    /** 1px あたりのドットの数。 */
    public static final int DOTS_PER_PX = 4;

    private static final String[] OPEN = {"..#.#.#..", ".##.#.##.", "###.#.###", ".##.#.##.", "..#.#.#.."};
    private static final String[] CLOSE = {"#...#...#", "##..#..##", "###.#.###", "##..#..##", "#...#...#"};
    private static final String[] OPEN_SMALL = {".#.#.#.", "##.#.##", ".#.#.#."};
    private static final String[] CLOSE_SMALL = {"#..#..#", "##.#.##", "#..#..#"};
    private static final String[] EXTEND = {"..#.#.#..", ".##.#.##.", "###.#.###", ".##.#.##.", "..#.#.#..", ".........",
            "....#....", "....#....", "..#####..", "....#....", "....#...."};
    private static final String[] EXTEND_SMALL = {".#.#.#.", "##.#.##", ".#.#.#.", ".......", "...#...", "..###..", "...#..."};
    private static final String[] UP = {"..#..", ".###.", "#.#.#", "..#..", "..#.."};

    /**
     * @param symbol 図柄のドット。応答灯がつくと光る
     */
    public record Glyph(List<Box> symbol) {
    }

    private ButtonGlyphs() {
    }

    /**
     * ボタン button の面の図柄。
     *
     * @param label 行先ボタンの階名（ほかのボタンでは使わない）。3×5 ドットの文字で、2文字まで
     */
    public static Glyph of(Button button, String label) {
        int size = (int) Math.round(button.box().sizeY() * CarModel.PIXELS_PER_BLOCK) * DOTS_PER_PX;
        FaceGrid grid = grid(button);
        List<Box> symbol = new ArrayList<>();
        boolean small = size < 3 * DOTS_PER_PX;
        switch (button.kind()) {
            case CAR_CALL -> addText(symbol, grid, label, size, (size - DotFont.HEIGHT) / 2);
            case HALL_UP, HALL_DOWN -> addPattern(symbol, grid, button.kind() == ButtonLayout.Kind.HALL_UP ? UP : flip(UP), size,
                    (size - UP.length) / 2);
            case OPEN -> addPattern(symbol, grid, small ? OPEN_SMALL : OPEN, size, (size - (small ? 3 : 5)) / 2);
            case CLOSE -> addPattern(symbol, grid, small ? CLOSE_SMALL : CLOSE, size, (size - (small ? 3 : 5)) / 2);
            case OPEN_EXTEND -> addPattern(symbol, grid, small ? EXTEND_SMALL : EXTEND, size, (size - (small ? EXTEND_SMALL : EXTEND).length) / 2);
            case EMERGENCY -> addText(symbol, grid, small ? "S" : "SOS", size, (size - DotFont.HEIGHT) / 2);
        }
        return new Glyph(symbol);
    }

    /** ボタンの手前の面の格子。左上が (0, 0)。 */
    private static FaceGrid grid(Button button) {
        Box box = button.box();
        double plane = switch (button.normal()) {
            case POS_X -> box.maxX();
            case NEG_X -> box.minX();
            case POS_Z -> box.maxZ();
            case NEG_Z -> box.minZ();
        };
        boolean alongX = button.normal().x() == 0;
        int rightSign = alongX ? button.normal().rightX() : button.normal().rightZ();
        double left = alongX ? (rightSign > 0 ? box.minX() : box.maxX()) : (rightSign > 0 ? box.minZ() : box.maxZ());
        return new FaceGrid(button.normal(), plane, left, box.maxY(), 1.0 / (CarModel.PIXELS_PER_BLOCK * DOTS_PER_PX));
    }

    private static void addText(List<Box> dots, FaceGrid grid, String text, int size, int top) {
        StringBuilder shown = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (DotFont.supports(c) && shown.length() < 3) {
                shown.append(c);
            }
        }
        int width = shown.length() * (DotFont.WIDTH + 1) - 1;
        int left = Math.max(0, (size - width) / 2);
        for (int i = 0; i < shown.length(); i++) {
            for (int y = 0; y < DotFont.HEIGHT; y++) {
                for (int x = 0; x < DotFont.WIDTH; x++) {
                    int column = left + i * (DotFont.WIDTH + 1) + x;
                    if (DotFont.isLit(shown.charAt(i), x, y) && column < size) {
                        dots.add(grid.dot(column, top + y));
                    }
                }
            }
        }
    }

    private static void addPattern(List<Box> dots, FaceGrid grid, String[] pattern, int size, int top) {
        int left = (size - pattern[0].length()) / 2;
        for (int y = 0; y < pattern.length; y++) {
            for (int x = 0; x < pattern[y].length(); x++) {
                if (pattern[y].charAt(x) == '#') {
                    dots.add(grid.dot(left + x, top + y));
                }
            }
        }
    }

    private static String[] flip(String[] pattern) {
        String[] flipped = new String[pattern.length];
        for (int i = 0; i < pattern.length; i++) {
            flipped[i] = pattern[pattern.length - 1 - i];
        }
        return flipped;
    }
}

package net.lausiv1024.realelevator.client;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import net.lausiv1024.realelevator.core.design.InteriorConfig;
import net.lausiv1024.realelevator.design.DesignPreset;
import net.lausiv1024.realelevator.design.Materials;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * デザインの画面。設置用ブロックの設定画面と保守用レンチのリニューアル画面から開く。
 *
 * <p>上の「共通／1／2／…／＋」で、共通の意匠と、一部の階だけ変える乗場の意匠（{@link InteriorConfig.LandingVariant}）を切り替える。
 * 「＋」で乗場の意匠を足し、選んだ意匠では、その意匠にする階と乗場の部位だけを変えられる（「削除」で共通に戻す）。
 *
 * <p>左の列にプリセットと、天井・手すり・防犯窓・乗場の意匠の設定。中の列に素材を選ぶ部位。右に、選んだ部位の候補のブロックを
 * アイテムのアイコンで並べ、クリックで選ぶ（多ければホイールでスクロール）。トランサムの「前壁と同じ／ドアと同じ」、乗場の壁の
 * 「まわりに合わせる」、乗場のドアの「かごのドアと同じ」、カーペットの「なし」は、候補の上のボタンで選ぶ。サーバーで自由設定がオンなら、
 * 候補のあとの「その他のブロック」に、持ち物にある素材にできるブロックを並べる。候補の下に、かご内と乗場を切り替えて見られるプレビューを出す。
 * 値を変えるたびに設定を送る。
 */
final class DesignScreen extends Screen {
    private static final int MARGIN = 10;
    private static final int TOP = 28;
    private static final int ROW = 22;
    /** 共通と乗場の意匠の切り替えの下の、設定を並べ始める高さ。 */
    private static final int CONTENT_TOP = TOP + ROW;
    private static final int LEFT_WIDTH = 150;
    private static final int PART_WIDTH = 100;
    private static final int GAP = 6;
    private static final int SLOT = 18;
    private static final int TAB_WIDTH = 24;
    private static final int FLOOR_BUTTON_WIDTH = 24;
    private static final int FLOOR_BUTTON_HEIGHT = 16;
    private static final int SELECTED = 0xFFFFD040;
    private static final int SLOT_BORDER = 0xFF555555;
    private static final int SLOT_FILL = 0xFF202020;
    /** プレビューに残す高さの下限 [px]。候補が多くても、これだけはプレビューに残す。 */
    private static final int MIN_PREVIEW_HEIGHT = 80;
    /** プレビューの幅の、高さに対する上限（広い画面で横長になりすぎないように）。 */
    private static final double MAX_PREVIEW_ASPECT = 1.8;
    private ElevatorPreview.View previewView = ElevatorPreview.View.CAR;

    /** 素材を選ぶ部位（カーペットは素材ではなく色）。 */
    private enum Choice {
        WALL(InteriorConfig.Part.WALL),
        FRONT(InteriorConfig.Part.FRONT),
        TRANSOM(InteriorConfig.Part.TRANSOM),
        DOOR(InteriorConfig.Part.DOOR),
        FLOOR(InteriorConfig.Part.FLOOR),
        CARPET(null),
        BASEBOARD(InteriorConfig.Part.BASEBOARD),
        LANDING_FRAME(InteriorConfig.Part.LANDING_FRAME),
        LANDING_WALL(InteriorConfig.Part.LANDING_WALL),
        LANDING_DOOR(InteriorConfig.Part.LANDING_DOOR);

        @Nullable
        final InteriorConfig.Part part;

        Choice(@Nullable InteriorConfig.Part part) {
            this.part = part;
        }

        boolean isLanding() {
            return part != null && part.isLanding();
        }
    }

    /** 候補の欄の一マス。ブロックの候補か、「その他のブロック」の見出し（行の頭）か、空き。 */
    private record Cell(@Nullable String value, boolean header) {
        static final Cell EMPTY = new Cell(null, false);
        static final Cell HEADER = new Cell(null, true);
    }

    private final Screen parent;
    private final SettingsForm form;
    private final Runnable send;
    private Choice choice = Choice.WALL;
    /** 編集している意匠。−1 は共通、0 以上は階ごとの乗場の意匠の番号。 */
    private int variant = -1;
    private DesignPreset preset;
    private int scroll;
    @Nullable
    private Component message;

    DesignScreen(Screen parent, SettingsForm form, Runnable send) {
        super(Component.translatable("gui.realelevator.design.title"));
        this.parent = parent;
        this.form = form;
        this.send = send;
        List<DesignPreset> presets = ClientPresets.presets();
        this.preset = presets.isEmpty() ? null : presets.get(0);
    }

    @Override
    protected void init() {
        List<InteriorConfig.LandingVariant> variants = form.interior().landingVariants();
        if (variant >= variants.size()) {
            variant = -1;
        }
        if (variant >= 0 && !choice.isLanding()) {
            choice = Choice.LANDING_FRAME;
        }
        updatePreviewFloor();
        initTabs(variants);
        if (variant < 0) {
            initCommonSettings();
        } else {
            initVariantSettings(variants.get(variant));
        }

        // 部位（選んでいる部位のボタンは押せない見た目にする）
        int partX = MARGIN + LEFT_WIDTH + GAP;
        List<Choice> choices = choices();
        int partRow = Math.min(ROW, Math.max(16, (height - 34 - CONTENT_TOP) / choices.size()));
        for (int i = 0; i < choices.size(); i++) {
            Choice c = choices.get(i);
            Button button = Button.builder(Component.translatable("gui.realelevator.design.part." + c.name().toLowerCase(Locale.ROOT)), b -> {
                choice = c;
                scroll = 0;
                message = null;
                rebuildWidgets();
            }).bounds(partX, CONTENT_TOP + i * partRow, PART_WIDTH, Math.min(20, partRow - 2)).build();
            button.active = c != choice;
            addRenderableWidget(button);
        }

        // 候補の上の、特別な値のボタン
        int gridX = gridLeft();
        int specialX = gridX;
        for (String value : specialValues()) {
            Component label = Component.translatable("gui.realelevator.design.value." + specialKey(value));
            int w = font.width(label) + 10;
            Button button = Button.builder(label, b -> select(value)).bounds(specialX, CONTENT_TOP, w, 20).build();
            button.active = !value.equals(current());
            addRenderableWidget(button);
            specialX += w + 4;
        }
        if (form.preview() != null) {
            int tabWidth = Math.min(100, (width - MARGIN - gridX - GAP) / 2);
            for (ElevatorPreview.View view : ElevatorPreview.View.values()) {
                Button button = Button.builder(Component.translatable("gui.realelevator.preview.tab." + view.name().toLowerCase(Locale.ROOT)), b -> {
                    previewView = view;
                    rebuildWidgets();
                }).bounds(previewLeft() + view.ordinal() * (tabWidth + GAP), previewTop() - ROW, tabWidth, 20).build();
                button.active = view != previewView;
                addRenderableWidget(button);
            }
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - 75, height - 26, 150, 20).build());
    }

    /** 上の「共通／1／2／…／＋」。 */
    private void initTabs(List<InteriorConfig.LandingVariant> variants) {
        int x = MARGIN;
        Component commonLabel = Component.translatable("gui.realelevator.design.common");
        int commonWidth = font.width(commonLabel) + 12;
        Button common = Button.builder(commonLabel, b -> selectVariant(-1)).bounds(x, TOP, commonWidth, 20).build();
        common.active = variant >= 0;
        addRenderableWidget(common);
        x += commonWidth + 2;
        for (int i = 0; i < variants.size(); i++) {
            int index = i;
            Button tab = Button.builder(Component.literal(Integer.toString(i + 1)), b -> selectVariant(index))
                    .bounds(x, TOP, TAB_WIDTH, 20).build();
            tab.active = variant != i;
            addRenderableWidget(tab);
            x += TAB_WIDTH + 2;
        }
        Button add = Button.builder(Component.literal("+"), b -> addVariant()).bounds(x, TOP, TAB_WIDTH, 20).build();
        add.active = variants.size() < InteriorConfig.MAX_LANDING_VARIANTS;
        addRenderableWidget(add);
    }

    /** 共通の意匠の左の列：プリセット、天井・手すり・かごの防犯窓、共通の乗場の意匠。 */
    private void initCommonSettings() {
        int y = CONTENT_TOP;
        List<DesignPreset> presets = ClientPresets.presets();
        if (!presets.isEmpty()) {
            if (!presets.contains(preset)) {
                preset = presets.get(0);
            }
            addRenderableWidget(CycleButton.<DesignPreset>builder(p -> Component.translatable(p.name()))
                    .withValues(presets).withInitialValue(preset)
                    .create(MARGIN, y, LEFT_WIDTH - 44, 20, Component.translatable("gui.realelevator.design.preset"), (b, v) -> preset = v));
            addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.design.apply"), b -> applyPreset())
                    .bounds(MARGIN + LEFT_WIDTH - 40, y, 40, 20).build());
            y += ROW;
        }
        InteriorConfig interior = form.interior();
        addRenderableWidget(left(y, SettingsForm.enumButton("ceiling", InteriorConfig.CeilingType.values(), interior.ceiling(),
                v -> form.setInterior(form.interior().withCeiling(v)), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.enumButton("handrail", InteriorConfig.HandrailPlacement.values(), interior.handrail(),
                v -> form.setInterior(form.interior().withHandrail(v)), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.enumButton("security_window", InteriorConfig.SecurityWindow.values(), interior.securityWindow(),
                v -> form.setInterior(form.interior().withSecurityWindow(v)), send)));
        y += ROW;
        addLandingSettings(y, interior.landing());
    }

    /** 階ごとの乗場の意匠の左の列：その意匠にする階、乗場の意匠、削除。 */
    private void initVariantSettings(InteriorConfig.LandingVariant selected) {
        int y = CONTENT_TOP + font.lineHeight + 2;
        int[] floors = form.floors();
        int perRow = Math.max(1, (LEFT_WIDTH + 2) / (FLOOR_BUTTON_WIDTH + 2));
        for (int i = 0; i < floors.length; i++) {
            int floor = i;
            int owner = ownerOf(i);
            String label = form.label(floors[i]);
            Component text = Component.literal(label.isEmpty() ? Integer.toString(i + 1) : label)
                    .withStyle(owner == variant ? ChatFormatting.YELLOW : owner >= 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.WHITE);
            Button button = Button.builder(text, b -> toggleFloor(floor))
                    .bounds(MARGIN + (i % perRow) * (FLOOR_BUTTON_WIDTH + 2), y + (i / perRow) * (FLOOR_BUTTON_HEIGHT + 2),
                            FLOOR_BUTTON_WIDTH, FLOOR_BUTTON_HEIGHT)
                    .tooltip(Tooltip.create(owner == variant
                            ? Component.translatable("gui.realelevator.design.floor.in_this")
                            : owner >= 0 ? Component.translatable("gui.realelevator.design.floor.in_other", owner + 1)
                            : Component.translatable("gui.realelevator.design.floor.common")))
                    .build();
            addRenderableWidget(button);
        }
        y += Math.max(1, (floors.length + perRow - 1) / perRow) * (FLOOR_BUTTON_HEIGHT + 2) + 4;
        y = addLandingSettings(y, selected.landing());
        addRenderableWidget(Button.builder(Component.translatable("gui.realelevator.design.delete_variant"), b -> deleteVariant())
                .bounds(MARGIN, y, LEFT_WIDTH, 20).build());
    }

    /** 乗場の意匠の設定（枠・操作盤・ランタンの背面プレート・乗場の防犯窓）を y から並べ、次の高さを返す。 */
    private int addLandingSettings(int y, InteriorConfig.Landing landing) {
        addRenderableWidget(left(y, SettingsForm.enumButton("landing_frame", InteriorConfig.FrameStyle.values(), landing.frame(),
                v -> setLanding(landing().withFrame(v)), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.enumButton("landing_panel", InteriorConfig.PanelStyle.values(), landing.panel(),
                v -> setLanding(landing().withPanel(v)), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.onOff("lantern_plate", landing.lanternPlate(),
                v -> setLanding(landing().withLanternPlate(v)), send)));
        y += ROW;
        addRenderableWidget(left(y, SettingsForm.enumButton("landing_security_window", InteriorConfig.LandingWindow.values(),
                landing.securityWindow(), v -> setLanding(landing().withSecurityWindow(v)), send)));
        return y + ROW;
    }

    /** 編集している乗場の意匠（共通か、階ごとの意匠）。 */
    private InteriorConfig.Landing landing() {
        InteriorConfig interior = form.interior();
        return variant >= 0 ? interior.landingVariants().get(variant).landing() : interior.landing();
    }

    private void setLanding(InteriorConfig.Landing landing) {
        InteriorConfig interior = form.interior();
        if (variant < 0) {
            form.setInterior(interior.withLanding(landing));
            return;
        }
        List<InteriorConfig.LandingVariant> variants = new ArrayList<>(interior.landingVariants());
        variants.set(variant, variants.get(variant).withLanding(landing));
        form.setInterior(interior.withLandingVariants(variants));
    }

    /** 停止階 floor が入っている階ごとの意匠の番号。共通なら −1。 */
    private int ownerOf(int floor) {
        List<InteriorConfig.LandingVariant> variants = form.interior().landingVariants();
        for (int i = 0; i < variants.size(); i++) {
            if (variants.get(i).floors().contains(floor)) {
                return i;
            }
        }
        return -1;
    }

    private void selectVariant(int index) {
        variant = index;
        scroll = 0;
        message = null;
        if (index >= 0) {
            previewView = ElevatorPreview.View.LANDING;
        }
        rebuildWidgets();
    }

    /** 乗場の意匠を足す。共通の乗場の意匠を写して始め、階はまだ選ばない。 */
    private void addVariant() {
        InteriorConfig interior = form.interior();
        if (interior.landingVariants().size() >= InteriorConfig.MAX_LANDING_VARIANTS) {
            return;
        }
        List<InteriorConfig.LandingVariant> variants = new ArrayList<>(interior.landingVariants());
        variants.add(new InteriorConfig.LandingVariant(List.of(), interior.landing()));
        form.setInterior(interior.withLandingVariants(variants));
        send.run();
        selectVariant(variants.size() - 1);
    }

    private void deleteVariant() {
        InteriorConfig interior = form.interior();
        List<InteriorConfig.LandingVariant> variants = new ArrayList<>(interior.landingVariants());
        variants.remove(variant);
        form.setInterior(interior.withLandingVariants(variants));
        send.run();
        selectVariant(-1);
    }

    /** 停止階 floor を、選んでいる意匠に入れる（入っていれば共通に戻す）。ほかの意匠に入っていれば、そこから移す。 */
    private void toggleFloor(int floor) {
        InteriorConfig interior = form.interior();
        form.setInterior(interior.assignLandingFloor(floor, ownerOf(floor) == variant ? -1 : variant));
        send.run();
        rebuildWidgets();
    }

    /** 階ごとの意匠を選んでいるときは、その意匠の最初の階の乗場をプレビューで見せる。 */
    private void updatePreviewFloor() {
        if (form.preview() == null) {
            return;
        }
        List<Integer> floors = variant >= 0 ? form.interior().landingVariants().get(variant).floors() : List.of();
        form.preview().showFloor(floors.isEmpty() ? null : floors.get(0));
    }

    /** 中の列に出す部位。階ごとの意匠では乗場の部位だけ。 */
    private List<Choice> choices() {
        List<Choice> choices = new ArrayList<>();
        for (Choice c : Choice.values()) {
            if (variant < 0 || c.isLanding()) {
                choices.add(c);
            }
        }
        return choices;
    }

    /** 候補の欄の下端（候補とプレビューを合わせた範囲）。 */
    private int rightBottom() {
        return height - 32;
    }

    private int previewHeight() {
        return form.preview() == null ? 0 : rightBottom() - previewTop();
    }

    /**
     * プレビューの上端。切り替えのボタンはその上に置く。素材の候補には、どの部位の候補も収まる段数を先に取り（2段より少なくはしない）、
     * 残りの高さをプレビューにする。候補が多くても、プレビューには {@value #MIN_PREVIEW_HEIGHT}px は残す。
     */
    private int previewTop() {
        int area = rightBottom() - gridTop();
        int gridHeight = Math.min(candidateRowsNeeded() * SLOT, Math.max(SLOT * 2, area - ROW - 4 - MIN_PREVIEW_HEIGHT));
        return gridTop() + gridHeight + 4 + ROW;
    }

    /** どの部位の候補も収まる段数。 */
    private int candidateRowsNeeded() {
        int most = (InteriorConfig.Carpet.values().length + gridColumns() - 1) / gridColumns();
        for (Choice c : Choice.values()) {
            if (c.part != null) {
                most = Math.max(most, (cells(c).size() + gridColumns() - 1) / gridColumns());
            }
        }
        return most;
    }

    /** プレビューの幅。広い画面では、高さの {@value #MAX_PREVIEW_ASPECT} 倍までにして、候補の欄の中央に置く。 */
    private int previewWidth() {
        return Math.min(width - MARGIN - gridLeft(), (int) (previewHeight() * MAX_PREVIEW_ASPECT));
    }

    private int previewLeft() {
        return gridLeft() + (width - MARGIN - gridLeft() - previewWidth()) / 2;
    }

    private static CycleButton<?> left(int y, CycleButton<?> button) {
        button.setX(MARGIN);
        button.setY(y);
        button.setWidth(LEFT_WIDTH);
        return button;
    }

    private int gridLeft() {
        return MARGIN + LEFT_WIDTH + GAP + PART_WIDTH + GAP;
    }

    private int gridTop() {
        return specialValues().isEmpty() ? CONTENT_TOP : CONTENT_TOP + ROW;
    }

    private int gridColumns() {
        return Math.max(1, (width - MARGIN - gridLeft()) / SLOT);
    }

    private int gridRows() {
        int bottom = form.preview() != null ? previewTop() - ROW - 4 : rightBottom();
        return Math.max(1, (bottom - gridTop()) / SLOT);
    }

    /** 候補の上にボタンで出す、特別な値。 */
    private List<String> specialValues() {
        return switch (choice) {
            case TRANSOM -> List.of(InteriorConfig.SAME_AS_FRONT, InteriorConfig.SAME_AS_DOOR);
            case LANDING_WALL -> List.of(InteriorConfig.AUTO);
            case LANDING_DOOR -> List.of(InteriorConfig.SAME_AS_DOOR);
            case CARPET -> List.of(InteriorConfig.Carpet.NONE.name());
            default -> List.of();
        };
    }

    private String specialKey(String value) {
        return switch (value) {
            case InteriorConfig.SAME_AS_FRONT -> "same_as_front";
            case InteriorConfig.SAME_AS_DOOR -> choice == Choice.LANDING_DOOR ? "same_as_car_door" : "same_as_door";
            case InteriorConfig.AUTO -> "auto";
            default -> "none";
        };
    }

    /** 選んでいる部位の、今の値（素材のID。カーペットは色の名前）。 */
    private String current() {
        if (choice.part == null) {
            return form.interior().carpet().name();
        }
        return choice.isLanding() ? landing().material(choice.part) : form.interior().material(choice.part);
    }

    /** 選んでいる部位の候補の欄。 */
    private List<Cell> cells() {
        return cells(choice);
    }

    /**
     * 部位 c の候補の欄。タグの候補（カーペットは色）を並べ、自由設定がオンなら、行を改めて「その他のブロック」の見出しと、
     * 持ち物にある素材にできるブロック（タグの候補にないもの）を並べる。自由設定で選んだ、タグにない今の素材も「その他」に出す。
     */
    private List<Cell> cells(Choice c) {
        List<Cell> cells = new ArrayList<>();
        if (c.part == null) {
            for (InteriorConfig.Carpet carpet : InteriorConfig.Carpet.values()) {
                if (carpet != InteriorConfig.Carpet.NONE) {
                    cells.add(new Cell(carpet.name(), false));
                }
            }
            return cells;
        }
        List<String> tagged = Materials.candidates(c.part);
        for (String id : tagged) {
            cells.add(new Cell(id, false));
        }
        Set<String> others = new LinkedHashSet<>();
        if (c == choice) {
            String current = current();
            if (!tagged.contains(current) && !specialValues().contains(current)) {
                others.add(current);
            }
        }
        if (ClientPresets.freeMaterials()) {
            others.addAll(inventoryMaterials(c.part, tagged));
        }
        if (ClientPresets.freeMaterials() || !others.isEmpty()) {
            int columns = gridColumns();
            while (cells.size() % columns != 0) {
                cells.add(Cell.EMPTY);
            }
            cells.add(Cell.HEADER);
            while (cells.size() % columns != 0) {
                cells.add(Cell.EMPTY);
            }
            for (String id : others) {
                cells.add(new Cell(id, false));
            }
        }
        return cells;
    }

    /** 持ち物にある、部位 part の素材にできるブロック（タグの候補 tagged にないもの）のID。並びは持ち物の順。 */
    private List<String> inventoryMaterials(InteriorConfig.Part part, List<String> tagged) {
        Set<String> ids = new LinkedHashSet<>();
        if (minecraft == null || minecraft.player == null) {
            return List.of();
        }
        Inventory inventory = minecraft.player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.getItem() instanceof BlockItem item && Materials.isFreeMaterial(part, item.getBlock())) {
                String id = Materials.id(item.getBlock());
                if (!tagged.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return new ArrayList<>(ids);
    }

    private static Block blockOf(String value) {
        InteriorConfig.Carpet carpet = carpetOf(value);
        return Materials.block(carpet != null ? carpet.blockId() : value);
    }

    @Nullable
    private static InteriorConfig.Carpet carpetOf(String value) {
        for (InteriorConfig.Carpet carpet : InteriorConfig.Carpet.values()) {
            if (carpet.name().equals(value)) {
                return carpet;
            }
        }
        return null;
    }

    private void select(String value) {
        InteriorConfig interior = form.interior();
        if (choice.part == null) {
            InteriorConfig.Carpet carpet = carpetOf(value);
            form.setInterior(interior.withCarpet(carpet != null ? carpet : InteriorConfig.Carpet.NONE));
        } else if (choice.isLanding()) {
            setLanding(landing().withMaterial(choice.part, value));
        } else {
            form.setInterior(interior.withMaterial(choice.part, value));
        }
        message = null;
        send.run();
        rebuildWidgets();
    }

    /** プリセットを適用する。共通の意匠に入れ、階ごとの乗場の意匠はそのまま残す。 */
    private void applyPreset() {
        if (preset == null) {
            return;
        }
        form.setInterior(preset.interior().withLandingVariants(form.interior().landingVariants()));
        if (form.observationHandler() != null) {
            form.observationHandler().accept(preset.observation());
            message = null;
        } else {
            // リニューアルでは、展望仕様（奥壁のガラス）は変えられない
            message = preset.observation() ? Component.translatable("gui.realelevator.design.observation_install_only") : null;
        }
        send.run();
        rebuildWidgets();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        if (variant >= 0) {
            graphics.drawString(font, Component.translatable("gui.realelevator.design.floors"), MARGIN, CONTENT_TOP, 0xFFFFFF);
        }
        List<Cell> cells = cells();
        int columns = gridColumns();
        int rows = gridRows();
        scroll = Math.clamp(scroll, 0, Math.max(0, (cells.size() + columns - 1) / columns - rows));
        String current = current();
        String hovered = null;
        for (int i = scroll * columns; i < cells.size() && i < (scroll + rows) * columns; i++) {
            int x = gridLeft() + (i % columns) * SLOT;
            int y = gridTop() + (i / columns - scroll) * SLOT;
            Cell cell = cells.get(i);
            if (cell.header()) {
                graphics.drawString(font, Component.translatable("gui.realelevator.design.other_blocks"), x + 1, y + (SLOT - font.lineHeight) / 2 + 1,
                        0xC0C0C0);
                continue;
            }
            if (cell.value() == null) {
                continue;
            }
            String value = cell.value();
            graphics.fill(x, y, x + SLOT, y + SLOT, value.equals(current) ? SELECTED : SLOT_BORDER);
            graphics.fill(x + 1, y + 1, x + SLOT - 1, y + SLOT - 1, SLOT_FILL);
            graphics.renderItem(new ItemStack(blockOf(value).asItem()), x + 1, y + 1);
            if (mouseX >= x && mouseX < x + SLOT && mouseY >= y && mouseY < y + SLOT) {
                hovered = value;
            }
        }
        if (form.preview() != null) {
            form.preview().render(graphics, previewLeft(), previewTop(), previewWidth(), previewHeight(), form, previewView, partialTick);
        }
        if (message != null) {
            graphics.drawCenteredString(font, message, width / 2, height - 40, 0xFFA0A0);
        }
        if (hovered != null) {
            graphics.renderTooltip(font, blockOf(hovered).getName(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        List<Cell> cells = cells();
        int columns = gridColumns();
        int column = (int) Math.floor((mouseX - gridLeft()) / SLOT);
        int row = (int) Math.floor((mouseY - gridTop()) / SLOT);
        if (button == 0 && column >= 0 && column < columns && row >= 0 && row < gridRows()) {
            int index = (row + scroll) * columns + column;
            if (index < cells.size() && cells.get(index).value() != null) {
                select(cells.get(index).value());
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean overPreview(double mouseX, double mouseY) {
        return form.preview() != null && mouseX >= previewLeft() && mouseX <= previewLeft() + previewWidth()
                && mouseY >= previewTop() && mouseY <= previewTop() + previewHeight();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (overPreview(mouseX, mouseY)) {
            form.preview().drag(dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (overPreview(mouseX, mouseY)) {
            form.preview().zoom(scrollY);
            return true;
        }
        if (mouseX >= gridLeft()) {
            scroll -= (int) Math.signum(scrollY);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void onClose() {
        if (form.preview() != null) {
            form.preview().showFloor(null);
        }
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
}

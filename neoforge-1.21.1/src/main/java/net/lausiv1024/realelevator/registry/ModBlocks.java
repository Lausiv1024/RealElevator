package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.InstallerBlock;
import net.lausiv1024.realelevator.block.ControllerBlock;
import net.lausiv1024.realelevator.block.GuideRailBlock;
import net.lausiv1024.realelevator.block.PowerReceiverBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RealElevator.MODID);

    /** 設置用ブロック。最下階の乗場の壁に置き、ここから昇降路を設置する。設置すると消え、頂部の制御盤に置き換わる。 */
    public static final DeferredBlock<InstallerBlock> INSTALLER = BLOCKS.register("installer",
            () -> new InstallerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    /** 頂部の制御盤（縦2ブロック）。設置したときだけ置かれ、サバイバルでは壊せない。 */
    public static final DeferredBlock<ControllerBlock> CONTROLLER = BLOCKS.register("controller",
            () -> new ControllerBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(-1.0F, 3600000.0F).sound(SoundType.METAL).noOcclusion().noLootTable()
                    .pushReaction(PushReaction.BLOCK).isValidSpawn((state, level, pos, type) -> false)));

    /** ガイドレール（仮）。制御盤から設置したときだけ置かれ、壊してもアイテムにならない。 */
    public static final DeferredBlock<GuideRailBlock> GUIDE_RAIL = BLOCKS.register("guide_rail",
            () -> new GuideRailBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).noOcclusion().noLootTable()));

    /** 運転切替盤。レッドストーン信号でエレベーターの通常運転と休止を切り替える。保守用レンチでエレベーターにつなぐ。 */
    public static final DeferredBlock<PowerReceiverBlock> POWER_RECEIVER = BLOCKS.register("power_receiver",
            () -> new PowerReceiverBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    // 内装の素材（オリジナル）。かごの壁・床などの素材として選べ、建築にも使える
    public static final DeferredBlock<Block> WOODGRAIN_LIGHT = material("woodgrain_light", MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> WOODGRAIN_MEDIUM = material("woodgrain_medium", MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> WOODGRAIN_DARK = material("woodgrain_dark", MapColor.WOOD, SoundType.WOOD);
    public static final DeferredBlock<Block> STAINLESS_HAIRLINE = material("stainless_hairline", MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> STAINLESS_MIRROR = material("stainless_mirror", MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> STAINLESS_ETCHED = material("stainless_etched", MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> BRONZE_HAIRLINE = material("bronze_hairline", MapColor.METAL, SoundType.METAL);
    public static final DeferredBlock<Block> BLACK_HAIRLINE = material("black_hairline", MapColor.COLOR_BLACK, SoundType.METAL);
    public static final DeferredBlock<Block> VINYL_GRAY = material("vinyl_gray", MapColor.STONE, SoundType.STONE);
    public static final DeferredBlock<Block> VINYL_BEIGE = material("vinyl_beige", MapColor.STONE, SoundType.STONE);

    private ModBlocks() {
    }

    private static DeferredBlock<Block> material(String name, MapColor color, SoundType sound) {
        return BLOCKS.registerSimpleBlock(name, BlockBehaviour.Properties.of().mapColor(color).strength(1.5F, 6.0F).sound(sound));
    }
}

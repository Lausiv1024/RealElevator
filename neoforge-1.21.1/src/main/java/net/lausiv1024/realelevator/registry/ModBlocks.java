package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.InstallerBlock;
import net.lausiv1024.realelevator.block.ControllerBlock;
import net.lausiv1024.realelevator.block.GuideRailBlock;
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

    private ModBlocks() {
    }
}

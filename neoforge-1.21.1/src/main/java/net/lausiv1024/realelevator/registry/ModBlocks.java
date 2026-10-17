package net.lausiv1024.realelevator.registry;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControlPanelBlock;
import net.lausiv1024.realelevator.block.GuideRailBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(RealElevator.MODID);

    /** 制御盤。最下階の乗場の壁に置き、ここから昇降路を設置する。 */
    public static final DeferredBlock<ControlPanelBlock> CONTROL_PANEL = BLOCKS.register("control_panel",
            () -> new ControlPanelBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    /** ガイドレール（仮）。制御盤から設置したときだけ置かれ、壊してもアイテムにならない。 */
    public static final DeferredBlock<GuideRailBlock> GUIDE_RAIL = BLOCKS.register("guide_rail",
            () -> new GuideRailBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL).strength(5.0F, 6.0F).sound(SoundType.METAL).noOcclusion().noLootTable()));

    private ModBlocks() {
    }
}

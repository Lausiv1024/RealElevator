package net.lausiv1024.realelevator.gametest;

import java.util.List;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControlPanelBlockEntity;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * 制御盤からの設置の動作確認。空の 7×14×7 の空間の南寄りに、南向き（乗場が南）の制御盤を置き、
 * 2階床（5ブロック上）を省スペースのピットで設置する。
 */
@GameTestHolder(RealElevator.MODID)
@PrefixGameTestTemplate(false)
public class InstallGameTests {
    private static final String TEMPLATE = "empty_shaft";
    private static final BlockPos PANEL = new BlockPos(5, 2, 5);
    private static final int RISE = 5;

    private static ControlPanelBlockEntity placePanel(GameTestHelper helper) {
        helper.setBlock(PANEL, ModBlocks.CONTROL_PANEL.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        ControlPanelBlockEntity panel = (ControlPanelBlockEntity) helper.getBlockEntity(PANEL);
        CompoundTag settings = panel.settingsTag();
        settings.putString("Pit", ShaftLayout.PitMode.COMPACT.name());
        panel.applySettings(settings);
        panel.toggleFloor(helper.absolutePos(PANEL).getY() + RISE);
        return panel;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void installPlacesTheCarDoorsAndRails(GameTestHelper helper) {
        ControlPanelBlockEntity panel = placePanel(helper);
        helper.assertTrue(panel.obstructions(helper.getLevel()).isEmpty(), "the shaft should be clear");
        panel.install();
        helper.assertTrue(panel.isInstalled(), "should be installed");

        BlockPos panelPos = helper.absolutePos(PANEL);
        Vec3 expected = panel.carOrigin();
        List<ElevatorCarEntity> cars = ElevatorTracker.cars(helper.getLevel()).stream()
                .filter(car -> car.position().distanceToSqr(expected) < 1e-6).toList();
        helper.assertTrue(cars.size() == 1, "the car should be at the expected place");
        ElevatorCarEntity car = cars.get(0);
        helper.assertTrue(car.facing() == Direction.SOUTH, "the car should face the landing");
        // 出入口の中心は、乗場から見て制御盤の2ブロック左（南向きなら西）
        helper.assertTrue(Math.abs(car.getX() - (panelPos.getX() + 0.5 - 2)) < 1e-9, "x: " + car.getX());
        helper.assertTrue(car.floorCount() == 2 && car.floors()[1] == panelPos.getY() + RISE, "floors");

        long doors = ElevatorTracker.landingDoors(helper.getLevel()).stream()
                .filter(door -> door.carUuid().filter(car.getUUID()::equals).isPresent()).count();
        helper.assertTrue(doors == 2, "a landing door per floor: " + doors);
        ShaftLayout layout = panel.layout();
        for (ShaftLayout.Rail rail : layout.rails()) {
            helper.assertTrue(helper.getLevel().getBlockState(panel.cellPos(rail.cell())).is(ModBlocks.GUIDE_RAIL.get()), "rail at " + rail);
        }

        // 設置したエレベーターで呼べる
        helper.assertTrue(car.pressCarCall(1) == PressResult.REGISTERED, "the call should be registered");
        helper.succeedWhen(() -> helper.assertTrue(!car.isMoving() && car.getY() == panelPos.getY() + RISE, "not arrived yet"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void installIsRefusedWhileTheShaftIsBlocked(GameTestHelper helper) {
        ControlPanelBlockEntity panel = placePanel(helper);
        // 昇降路の中に石を1つ置く
        BlockPos blocker = panel.cellPos(new ShaftLayout.Cell(ShaftLayout.CAR_COLUMN, 2, -1));
        helper.getLevel().setBlockAndUpdate(blocker, Blocks.STONE.defaultBlockState());
        helper.assertTrue(panel.obstructions(helper.getLevel()).equals(List.of(blocker)), "the stone should be reported");
        panel.install();
        helper.assertTrue(!panel.isInstalled(), "should not install while blocked");
        helper.assertTrue(ElevatorTracker.cars(helper.getLevel()).stream()
                .noneMatch(car -> car.position().distanceToSqr(panel.carOrigin()) < 1e-6), "no car should be placed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void breakingThePanelRemovesTheElevator(GameTestHelper helper) {
        ControlPanelBlockEntity panel = placePanel(helper);
        panel.install();
        ShaftLayout layout = panel.layout();
        Vec3 origin = panel.carOrigin();
        List<BlockPos> rails = layout.rails().stream().map(rail -> panel.cellPos(rail.cell())).toList();
        helper.setBlock(PANEL, Blocks.AIR);
        helper.succeedWhen(() -> {
            helper.assertTrue(ElevatorTracker.cars(helper.getLevel()).stream()
                    .noneMatch(car -> car.position().distanceToSqr(origin) < 1e-6), "the car should be removed");
            helper.assertTrue(ElevatorTracker.landingDoors(helper.getLevel()).stream()
                    .noneMatch(door -> Math.abs(door.getX() - origin.x) < 1e-6 && Math.abs(door.getZ() - origin.z) < 1e-6),
                    "the landing doors should be removed");
            for (BlockPos rail : rails) {
                helper.assertTrue(helper.getLevel().getBlockState(rail).isAir(), "the rail should be removed: " + rail);
            }
        });
    }
}

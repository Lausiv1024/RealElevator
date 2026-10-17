package net.lausiv1024.realelevator.gametest;

import java.util.List;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControlPanelBlockEntity;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.menu.ControlPanelMenu;
import net.lausiv1024.realelevator.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
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

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void somethingFallingOntoTheCounterweightStopsOnIt(GameTestHelper helper) {
        ControlPanelBlockEntity panel = placePanel(helper);
        panel.install();
        ElevatorCarEntity car = ElevatorTracker.cars(helper.getLevel()).stream()
                .filter(c -> c.position().distanceToSqr(panel.carOrigin()) < 1e-6).findFirst().orElseThrow();
        HoistMechanism hoist = car.hoist();
        helper.assertTrue(hoist != null && hoist.pitMode() == ShaftLayout.PitMode.COMPACT, "the car should have the hoistway");
        // かごが最下階にいるので、釣合い重りは上のほうにある
        AABB counterweight = car.counterweightAt(helper.getLevel().getGameTime());
        Vec3 centre = counterweight.getCenter();
        ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, helper.getLevel());
        stand.moveTo(centre.x, counterweight.maxY + 1, centre.z, 0, 0);
        helper.getLevel().addFreshEntity(stand);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(Math.abs(stand.getY() - counterweight.maxY) < 1e-3,
                    "should stand on the counterweight at " + counterweight.maxY + " but is at " + stand.getY());
            helper.succeed();
        });
    }

    private static void requestSpeed(ControlPanelBlockEntity panel, SpeedSpec spec, double acceleration, double jerk) {
        CompoundTag settings = panel.settingsTag();
        settings.putString("Speed", spec.name());
        settings.putDouble("Acceleration", acceleration);
        settings.putDouble("Jerk", jerk);
        panel.applySettings(settings);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void highSpeedNeedsANetherStarAndLocksItOnceInstalled(GameTestHelper helper) {
        ControlPanelBlockEntity panel = placePanel(helper);
        requestSpeed(panel, SpeedSpec.HIGH, 1.8, 2.5);
        helper.assertTrue(!panel.speed().spec().isHighSpeed(), "high speed should need a nether star");

        panel.starContainer().setItem(0, new ItemStack(Items.NETHER_STAR));
        requestSpeed(panel, SpeedSpec.HIGH, 1.8, 2.5);
        helper.assertTrue(panel.speed().equals(new SpeedSetting(SpeedSpec.HIGH, 1.8, 2.5)), "speed: " + panel.speed());

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ControlPanelMenu menu = new ControlPanelMenu(0, player.getInventory(), panel);
        helper.assertTrue(menu.getSlot(0).mayPickup(player), "the star can be taken out before installing");
        panel.install();
        ElevatorCarEntity car = ElevatorTracker.cars(helper.getLevel()).stream()
                .filter(c -> c.position().distanceToSqr(panel.carOrigin()) < 1e-6).findFirst().orElseThrow();
        helper.assertTrue(car.speed().equals(panel.speed()), "the car should get the speed: " + car.speed());
        helper.assertTrue(panel.isStarLocked() && !menu.getSlot(0).mayPickup(player), "the star should be locked");
        menu.quickMoveStack(player, 0);
        helper.assertTrue(panel.hasNetherStar(), "shift-clicking should not take the star out");
        // 設置したあとは、速度を変えられない
        requestSpeed(panel, SpeedSpec.NORMAL_LOW, 1.5, 1.5);
        helper.assertTrue(panel.speed().spec() == SpeedSpec.HIGH, "the speed should not change after installing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void takingTheStarOutBeforeInstallingFallsBackToANormalSpeed(GameTestHelper helper) {
        ControlPanelBlockEntity panel = placePanel(helper);
        panel.starContainer().setItem(0, new ItemStack(Items.NETHER_STAR));
        requestSpeed(panel, SpeedSpec.ULTRA_HIGH, 2.0, 3.0);
        helper.assertTrue(panel.speed().spec() == SpeedSpec.ULTRA_HIGH, "ultra high should be chosen");
        panel.starContainer().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(panel.speed().equals(SpeedSetting.standard(SpeedSpec.NORMAL_HIGH)), "speed: " + panel.speed());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void breakingThePanelDropsTheStar(GameTestHelper helper) {
        ControlPanelBlockEntity panel = placePanel(helper);
        panel.starContainer().setItem(0, new ItemStack(Items.NETHER_STAR));
        helper.setBlock(PANEL, Blocks.AIR);
        helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.NETHER_STAR, PANEL, 2.0));
    }
}

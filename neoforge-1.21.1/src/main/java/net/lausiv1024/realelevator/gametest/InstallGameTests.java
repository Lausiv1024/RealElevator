package net.lausiv1024.realelevator.gametest;

import java.util.List;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControllerBlockEntity;
import net.lausiv1024.realelevator.block.ElevatorPlan;
import net.lausiv1024.realelevator.block.InstallerBlockEntity;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.hoist.HoistMechanism;
import net.lausiv1024.realelevator.core.install.ShaftLayout;
import net.lausiv1024.realelevator.core.motion.SpeedSetting;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.ConfigTags;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.menu.InstallerMenu;
import net.lausiv1024.realelevator.registry.ModBlocks;
import net.lausiv1024.realelevator.registry.ModItems;
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

    private static InstallerBlockEntity placePanel(GameTestHelper helper) {
        helper.setBlock(PANEL, ModBlocks.INSTALLER.get().defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.SOUTH));
        InstallerBlockEntity panel = (InstallerBlockEntity) helper.getBlockEntity(PANEL);
        CompoundTag settings = panel.settingsTag();
        settings.putString("Pit", ShaftLayout.PitMode.COMPACT.name());
        panel.applySettings(settings);
        panel.toggleFloor(helper.absolutePos(PANEL).getY() + RISE);
        return panel;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void installPlacesTheCarDoorsRailsAndController(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        helper.assertTrue(panel.obstructions(helper.getLevel()).isEmpty(), "the shaft should be clear");
        ElevatorPlan plan = panel.plan();
        panel.install();
        // 設置用ブロックは消える
        helper.assertBlockNotPresent(ModBlocks.INSTALLER.get(), PANEL);

        BlockPos panelPos = helper.absolutePos(PANEL);
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        helper.assertTrue(car.facing() == Direction.SOUTH, "the car should face the landing");
        // 出入口の中心は、乗場から見て設置用ブロックの2ブロック左（南向きなら西）
        helper.assertTrue(Math.abs(car.getX() - (panelPos.getX() + 0.5 - 2)) < 1e-9, "x: " + car.getX());
        helper.assertTrue(car.floorCount() == 2 && car.floors()[1] == panelPos.getY() + RISE, "floors");

        long doors = ElevatorTracker.landingDoors(helper.getLevel()).stream()
                .filter(door -> door.carUuid().filter(car.getUUID()::equals).isPresent()).count();
        helper.assertTrue(doors == 2, "a landing door per floor: " + doors);
        ShaftLayout layout = plan.layout();
        for (ShaftLayout.Rail rail : layout.rails()) {
            helper.assertTrue(helper.getLevel().getBlockState(plan.cellPos(rail.cell())).is(ModBlocks.GUIDE_RAIL.get()), "rail at " + rail);
        }
        // 頂部の左奥に、縦2ブロックの制御盤
        List<ShaftLayout.Cell> cells = layout.controllerCells();
        BlockPos lower = plan.cellPos(cells.get(0));
        helper.assertTrue(helper.getLevel().getBlockState(lower).is(ModBlocks.CONTROLLER.get())
                && helper.getLevel().getBlockState(plan.cellPos(cells.get(1))).is(ModBlocks.CONTROLLER.get()), "the controller should be placed");
        helper.assertTrue(lower.equals(car.controllerPos()), "the car should know its controller");
        ControllerBlockEntity controller = (ControllerBlockEntity) helper.getLevel().getBlockEntity(lower);
        helper.assertTrue(controller != null && car.getUUID().equals(controller.carUuid()), "the controller should know its car");

        // 設置したエレベーターで呼べる
        helper.assertTrue(car.pressCarCall(1) == PressResult.REGISTERED, "the call should be registered");
        helper.succeedWhen(() -> helper.assertTrue(!car.isMoving() && car.getY() == panelPos.getY() + RISE, "not arrived yet"));
    }

    private static ElevatorCarEntity carAt(GameTestHelper helper, Vec3 origin) {
        List<ElevatorCarEntity> cars = ElevatorTracker.cars(helper.getLevel()).stream()
                .filter(car -> car.position().distanceToSqr(origin) < 1e-6).toList();
        helper.assertTrue(cars.size() == 1, "the car should be at the expected place");
        return cars.get(0);
    }

    private static ControllerBlockEntity controllerOf(GameTestHelper helper, ElevatorCarEntity car) {
        BlockPos pos = car.controllerPos();
        helper.assertTrue(pos != null && helper.getLevel().getBlockEntity(pos) instanceof ControllerBlockEntity, "the controller should exist");
        return (ControllerBlockEntity) helper.getLevel().getBlockEntity(pos);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void installIsRefusedWhileTheShaftIsBlocked(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        // 昇降路の中に石を1つ置く
        BlockPos blocker = panel.cellPos(new ShaftLayout.Cell(-2, 2, -1));
        helper.getLevel().setBlockAndUpdate(blocker, Blocks.STONE.defaultBlockState());
        helper.assertTrue(panel.obstructions(helper.getLevel()).equals(List.of(blocker)), "the stone should be reported");
        panel.install();
        helper.assertBlockPresent(ModBlocks.INSTALLER.get(), PANEL);
        helper.assertTrue(ElevatorTracker.cars(helper.getLevel()).stream()
                .noneMatch(car -> car.position().distanceToSqr(panel.carOrigin()) < 1e-6), "no car should be placed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dismantlingRemovesEverythingAndReturnsTheItems(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        panel.starContainer().setItem(0, new ItemStack(Items.NETHER_STAR));
        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        ControllerBlockEntity controller = controllerOf(helper, car);
        helper.assertTrue(controller.star().is(Items.NETHER_STAR), "the star should move to the controller");
        BlockPos controllerPos = controller.getBlockPos();
        List<BlockPos> rails = plan.layout().rails().stream().map(rail -> plan.cellPos(rail.cell())).toList();
        Vec3 origin = plan.carOrigin();

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        controller.dismantle(player);
        helper.succeedWhen(() -> {
            helper.assertTrue(ElevatorTracker.cars(helper.getLevel()).stream()
                    .noneMatch(c -> c.position().distanceToSqr(origin) < 1e-6), "the car should be removed");
            helper.assertTrue(ElevatorTracker.landingDoors(helper.getLevel()).stream()
                    .noneMatch(door -> Math.abs(door.getX() - origin.x) < 1e-6 && Math.abs(door.getZ() - origin.z) < 1e-6),
                    "the landing doors should be removed");
            for (BlockPos rail : rails) {
                helper.assertTrue(helper.getLevel().getBlockState(rail).isAir(), "the rail should be removed: " + rail);
            }
            helper.assertTrue(helper.getLevel().getBlockState(controllerPos).isAir()
                    && helper.getLevel().getBlockState(controllerPos.above()).isAir(), "the controller should be removed");
            helper.assertTrue(player.getInventory().contains(new ItemStack(ModItems.INSTALLER.get()))
                    && player.getInventory().contains(new ItemStack(Items.NETHER_STAR)), "the installer and the star should be returned");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void removingTheControllerBlockDismantlesTheElevator(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        BlockPos upper = controllerOf(helper, car).getBlockPos().above();
        // クリエイティブやコマンドで上のブロックを消しても、エレベーターごと解体する（設置用ブロックは最下階の乗場に落とす）
        helper.getLevel().setBlockAndUpdate(upper, Blocks.AIR.defaultBlockState());
        helper.succeedWhen(() -> {
            helper.assertTrue(car.isRemoved(), "the car should be removed");
            helper.assertItemEntityPresent(ModItems.INSTALLER.get(), PANEL, 2.0);
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void renewalChangesTheCarButNotTheSpeed(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        ControllerBlockEntity controller = controllerOf(helper, car);
        CompoundTag renewal = controller.renewalTag();
        renewal.put("Fixtures", ConfigTags.writeFixtures(FixtureConfig.DEFAULT.withPanelSides(FixtureConfig.PanelSides.BOTH)));
        CompoundTag labels = new CompoundTag();
        labels.putString(Integer.toString(car.floors()[1]), "R");
        renewal.put("Labels", labels);
        renewal.putString("Speed", SpeedSpec.NORMAL_HIGH.name());
        controller.applyRenewal(renewal);
        helper.assertTrue(car.fixtures().panelSides() == FixtureConfig.PanelSides.BOTH, "the fixtures should change");
        helper.assertTrue(car.floorLabel(1).equals("R"), "the label should change: " + car.floorLabel(1));
        helper.assertTrue(car.speed().spec() == SpeedSpec.NORMAL_LOW, "the speed should not change");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void somethingFallingOntoTheCounterweightStopsOnIt(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
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

    private static void requestSpeed(InstallerBlockEntity panel, SpeedSpec spec, double acceleration, double jerk) {
        CompoundTag settings = panel.settingsTag();
        settings.putString("Speed", spec.name());
        settings.putDouble("Acceleration", acceleration);
        settings.putDouble("Jerk", jerk);
        panel.applySettings(settings);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void highSpeedNeedsANetherStar(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        requestSpeed(panel, SpeedSpec.HIGH, 1.8, 2.5);
        helper.assertTrue(!panel.speed().spec().isHighSpeed(), "high speed should need a nether star");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        InstallerMenu menu = new InstallerMenu(0, player.getInventory(), panel);
        player.getInventory().add(new ItemStack(Items.NETHER_STAR));
        // 持ち物のネザースターをシフトクリックではめる
        menu.quickMoveStack(player, menu.slots.size() - 9);
        helper.assertTrue(panel.hasNetherStar(), "shift-clicking should put the star in");
        requestSpeed(panel, SpeedSpec.HIGH, 1.8, 2.5);
        helper.assertTrue(panel.speed().equals(new SpeedSetting(SpeedSpec.HIGH, 1.8, 2.5)), "speed: " + panel.speed());

        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        helper.assertTrue(car.speed().equals(new SpeedSetting(SpeedSpec.HIGH, 1.8, 2.5)), "the car should get the speed: " + car.speed());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void takingTheStarOutBeforeInstallingFallsBackToANormalSpeed(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        panel.starContainer().setItem(0, new ItemStack(Items.NETHER_STAR));
        requestSpeed(panel, SpeedSpec.ULTRA_HIGH, 2.0, 3.0);
        helper.assertTrue(panel.speed().spec() == SpeedSpec.ULTRA_HIGH, "ultra high should be chosen");
        panel.starContainer().setItem(0, ItemStack.EMPTY);
        helper.assertTrue(panel.speed().equals(SpeedSetting.standard(SpeedSpec.NORMAL_HIGH)), "speed: " + panel.speed());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void breakingThePanelDropsTheStar(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        panel.starContainer().setItem(0, new ItemStack(Items.NETHER_STAR));
        helper.setBlock(PANEL, Blocks.AIR);
        helper.succeedWhen(() -> helper.assertItemEntityPresent(Items.NETHER_STAR, PANEL, 2.0));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void aWideP24InstallsInAFourBlockShaft(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        CompoundTag settings = panel.settingsTag();
        settings.putString("Model", CarModel.P24.name());
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        ShaftLayout layout = plan.layout();
        helper.assertTrue(layout.leftColumn() == -4, "the shaft should be four blocks wide");
        helper.assertTrue(panel.obstructions(helper.getLevel()).isEmpty(), "the shaft should be clear: " + panel.obstructions(helper.getLevel()));
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        BlockPos panelPos = helper.absolutePos(PANEL);
        // 出入口の中心は、設置用ブロックの左の4ブロックの中央（ブロックの境目）
        helper.assertTrue(Math.abs(car.getX() - (panelPos.getX() + 0.5 - 2.5)) < 1e-9, "x: " + car.getX());
        helper.assertTrue(car.model() == CarModel.P24, "model");
        for (ShaftLayout.Rail rail : layout.rails()) {
            helper.assertTrue(helper.getLevel().getBlockState(plan.cellPos(rail.cell())).is(ModBlocks.GUIDE_RAIL.get()), "rail at " + rail);
        }
        helper.assertTrue(car.pressCarCall(1) == PressResult.REGISTERED, "the call should be registered");
        helper.succeedWhen(() -> helper.assertTrue(!car.isMoving() && car.getY() == panelPos.getY() + RISE, "not arrived yet"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void sideOpeningAndFourPanelDoorsOpenAndClose(GameTestHelper helper) {
        for (CarSpec spec : List.of(new CarSpec(CarModel.P11, DoorType.SIDE_LEFT), new CarSpec(CarModel.P24, DoorType.FOUR_CENTER))) {
            helper.assertTrue(spec.door() != DoorType.CENTER, "the door type should be available: " + spec);
        }
        InstallerBlockEntity panel = placePanel(helper);
        CompoundTag settings = panel.settingsTag();
        settings.putString("Model", CarModel.P24.name());
        settings.putString("Door", DoorType.FOUR_CENTER.name());
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        helper.assertTrue(plan.spec().door() == DoorType.FOUR_CENTER, "4CO should be chosen for P-24");
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        helper.assertTrue(car.spec().equals(new CarSpec(CarModel.P24, DoorType.FOUR_CENTER)), "the car should keep the door type: " + car.spec());
        long doors = ElevatorTracker.landingDoors(helper.getLevel()).stream()
                .filter(door -> door.carUuid().filter(car.getUUID()::equals).isPresent())
                .filter(door -> door.spec().equals(car.spec())).count();
        helper.assertTrue(doors == 2, "the landing doors should share the door type: " + doors);
        // 4枚の戸で当たり判定ができ、開いて閉じる
        helper.assertTrue(car.collisionBoxesAt(helper.getLevel().getGameTime()).size() == car.parts().size() + 4, "four car door panels");
        car.pressCarCall(1);
        BlockPos panelPos = helper.absolutePos(PANEL);
        helper.succeedWhen(() -> helper.assertTrue(!car.isMoving() && car.getY() == panelPos.getY() + RISE
                && car.doorFractionAt(helper.getLevel().getGameTime()) > 0.99, "the car should arrive and open"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void fourPanelDoorsAreNotOfferedForSmallModels(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        CompoundTag settings = panel.settingsTag();
        settings.putString("Model", CarModel.P13.name());
        settings.putString("Door", DoorType.FOUR_CENTER.name());
        panel.applySettings(settings);
        helper.assertTrue(panel.plan().spec().door() == DoorType.CENTER, "P-13 should fall back to CO");
        settings.putString("Door", DoorType.SIDE_RIGHT.name());
        panel.applySettings(settings);
        helper.assertTrue(panel.plan().spec().door() == DoorType.SIDE_RIGHT, "2S should be chosen for P-13");
        helper.succeed();
    }
}

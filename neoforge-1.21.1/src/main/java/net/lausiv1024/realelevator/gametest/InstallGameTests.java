package net.lausiv1024.realelevator.gametest;

import java.util.ArrayList;
import java.util.List;

import net.lausiv1024.realelevator.RealElevator;
import net.lausiv1024.realelevator.block.ControllerBlock;
import net.lausiv1024.realelevator.block.ControllerBlockEntity;
import net.lausiv1024.realelevator.block.ElevatorPlan;
import net.lausiv1024.realelevator.block.InstallerBlockEntity;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.panel.ButtonLayout;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.car.FloorEntrances;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.car.CarSpec;
import net.lausiv1024.realelevator.core.car.DoorType;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.design.InteriorConfig;
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
import net.minecraft.world.level.block.state.BlockState;
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
        renewal.put("Fixtures", ConfigTags.writeFixtures(FixtureConfig.DEFAULT.withPanelSides(FixtureConfig.PanelSides.BOTH)
                .withGroupControl(true).withHallLantern(FixtureConfig.HallLanternPlacement.RIGHT_OF_DOOR)));
        CompoundTag labels = new CompoundTag();
        labels.putString(Integer.toString(car.floors()[1]), "R");
        renewal.put("Labels", labels);
        renewal.putString("Speed", SpeedSpec.NORMAL_HIGH.name());
        helper.assertTrue(!car.landingOpen(), "landing open is off by default");
        renewal.putBoolean("LandingOpen", true);
        // 内装：タグにある素材は使い、タグにないもの（床に鉄ブロック）は初期値に戻す
        InteriorConfig wanted = InteriorConfig.DEFAULT.withMaterial(InteriorConfig.Part.WALL, "realelevator:woodgrain_dark")
                .withMaterial(InteriorConfig.Part.FLOOR, "minecraft:iron_block").withCeiling(InteriorConfig.CeilingType.INDIRECT)
                .withLanding(InteriorConfig.Landing.DEFAULT.withFrame(InteriorConfig.FrameStyle.FASCIA).withPanel(InteriorConfig.PanelStyle.EMBEDDED)
                        .withLanternPlate(false))
                .withMaterial(InteriorConfig.Part.LANDING_FRAME, "minecraft:smooth_quartz");
        renewal.put("Interior", ConfigTags.writeInterior(wanted));
        controller.applyRenewal(renewal);
        helper.assertTrue(car.fixtures().panelSides() == FixtureConfig.PanelSides.BOTH, "the fixtures should change");
        helper.assertTrue(car.floorLabel(1).equals("R"), "the label should change: " + car.floorLabel(1));
        helper.assertTrue(car.speed().spec() == SpeedSpec.NORMAL_LOW, "the speed should not change");
        helper.assertTrue(car.landingOpen() && controller.renewalTag().getBoolean("LandingOpen"), "landing open should be turned on");
        helper.assertTrue(car.interior().wall().equals("realelevator:woodgrain_dark") && car.interior().ceiling() == InteriorConfig.CeilingType.INDIRECT,
                "the interior should change: " + car.interior());
        helper.assertTrue(car.interior().floor().equals(InteriorConfig.DEFAULT.floor()), "a material outside the tag should be rejected");
        // 乗場の意匠と群管理向け。乗場の壁は「まわりに合わせる」のまま
        InteriorConfig.Landing landing = car.interior().landing();
        helper.assertTrue(landing.frame() == InteriorConfig.FrameStyle.FASCIA && landing.panel() == InteriorConfig.PanelStyle.EMBEDDED
                && !landing.lanternPlate() && landing.wallMaterial().equals(InteriorConfig.AUTO)
                && landing.frameMaterial().equals("minecraft:smooth_quartz"), "the landing should change: " + landing);
        helper.assertTrue(car.fixtures().groupControl() && car.fixtures().hallLantern() == FixtureConfig.HallLanternPlacement.RIGHT_OF_DOOR,
                "group control should be on: " + car.fixtures());
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

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void sideCounterweightPutsTheWeightBehindTheInstaller(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        CompoundTag settings = panel.settingsTag();
        settings.putString("Counterweight", ShaftLayout.CounterweightPosition.SIDE.name());
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        ShaftLayout layout = plan.layout();
        helper.assertTrue(layout.counterweight() == ShaftLayout.CounterweightPosition.SIDE, "side counterweight");
        // 重りの列（設置用ブロックの真後ろ）も空いている必要がある
        helper.assertTrue(layout.requiredClearCells().contains(new ShaftLayout.Cell(0, 0, -1)), "the counterweight column should be cleared");
        helper.assertTrue(panel.obstructions(helper.getLevel()).isEmpty(), "the shaft should be clear: " + panel.obstructions(helper.getLevel()));
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        AABB counterweight = car.counterweightAt(helper.getLevel().getGameTime());
        BlockPos panelPos = helper.absolutePos(PANEL);
        // 南向きの設置用ブロックの真後ろ（北）の列。X は設置用ブロックと同じ
        helper.assertTrue(counterweight.minX >= panelPos.getX() && counterweight.maxX <= panelPos.getX() + 1,
                "the counterweight should be behind the installer: " + counterweight);
        BlockPos controllerPos = car.controllerPos();
        BlockState controller = helper.getLevel().getBlockState(controllerPos);
        helper.assertTrue(controller.is(ModBlocks.CONTROLLER.get()) && controller.getValue(ControllerBlock.CORNER)
                && controller.getValue(HorizontalDirectionalBlock.FACING) == Direction.EAST, "the controller should be on the left wall: " + controller);
        helper.assertTrue(controllerPos.getX() == panelPos.getX() + layout.leftColumn(), "the controller should be in the left column");
        helper.assertTrue(car.pressCarCall(1) == PressResult.REGISTERED, "the call should be registered");
        helper.succeedWhen(() -> helper.assertTrue(!car.isMoving() && car.getY() == panelPos.getY() + RISE, "not arrived yet"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void throughCarOpensOnlyTheSideThatWasCalled(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        BlockPos panelPos = helper.absolutePos(PANEL);
        int top = panelPos.getY() + RISE;
        CompoundTag settings = panel.settingsTag();
        settings.putBoolean("Through", true);
        CompoundTag entrances = new CompoundTag();
        entrances.putString(Integer.toString(top), FloorEntrances.BOTH.name());
        settings.put("Entrances", entrances);
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        helper.assertTrue(plan.isThrough() && plan.counterweight() == ShaftLayout.CounterweightPosition.SIDE, "through cars use the side counterweight");
        helper.assertTrue(panel.obstructions(helper.getLevel()).isEmpty(), "the shaft should be clear: " + panel.obstructions(helper.getLevel()));
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        helper.assertTrue(car.isThrough(), "the car should be a through car");
        List<LandingDoorEntity> doors = ElevatorTracker.landingDoors(helper.getLevel()).stream()
                .filter(door -> door.carUuid().filter(car.getUUID()::equals).isPresent()).toList();
        helper.assertTrue(doors.size() == 3, "front at both floors and rear at the top: " + doors.size());
        LandingDoorEntity rearTop = doors.stream().filter(door -> door.entrance() == Entrance.REAR).findFirst().orElseThrow();
        helper.assertTrue(rearTop.getY() == top, "the rear door should be at the top floor");
        // 奥の乗場で▼を押すと、かごが来て奥の扉だけ開く
        car.pressHallCall(1, TravelDirection.DOWN, Entrance.REAR);
        helper.assertTrue(car.isHallCallLit(1, TravelDirection.DOWN, Entrance.REAR) && !car.isHallCallLit(1, TravelDirection.DOWN, Entrance.FRONT),
                "only the rear hall button should light");
        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(!car.isMoving() && car.getY() == top && car.doorFractionAt(now, Entrance.REAR) > 0.99,
                    "the rear door should open");
            helper.assertTrue(car.doorFractionAt(now, Entrance.FRONT) == 0 && rearTop.fractionAt(now) > 0.99, "only the rear doors");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void theDoorTypeSurvivesSavingAndLoading(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        CompoundTag settings = panel.settingsTag();
        settings.putString("Door", DoorType.SIDE_RIGHT.name());
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        CompoundTag saved = car.saveWithoutId(new CompoundTag());
        ElevatorCarEntity loaded = ModEntities.ELEVATOR_CAR.get().create(helper.getLevel());
        loaded.load(saved);
        helper.assertTrue(loaded.spec().equals(car.spec()) && loaded.spec().door() == DoorType.SIDE_RIGHT,
                "the door type should be kept: " + loaded.spec());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void theOtherSideOpensAfterTheOpenDoorsClose(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        BlockPos panelPos = helper.absolutePos(PANEL);
        CompoundTag settings = panel.settingsTag();
        settings.putBoolean("Through", true);
        CompoundTag entrances = new CompoundTag();
        entrances.putString(Integer.toString(panelPos.getY()), FloorEntrances.BOTH.name());
        settings.put("Entrances", entrances);
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        // かごがいる最下階で、正面の▲で正面を開き、開いているあいだに奥の▲を押す
        car.pressHallCall(0, TravelDirection.UP, Entrance.FRONT);
        helper.runAfterDelay(50, () -> {
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(car.doorFractionAt(now, Entrance.FRONT) > 0.99, "the front doors should be open");
            car.pressHallCall(0, TravelDirection.UP, Entrance.REAR);
            helper.assertTrue(car.doorFractionAt(now, Entrance.REAR) == 0, "the rear doors should wait");
        });
        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(car.doorFractionAt(now, Entrance.REAR) > 0.99, "the rear doors should open after the front closes");
            helper.assertTrue(car.doorFractionAt(now, Entrance.FRONT) == 0 && !car.isMoving(), "the car should stay with the front closed");
        });
    }

    /** 二方向出入口で、車椅子用操作盤の行先ボタンでは、乗ったとき（最後に開いた）の扉の反対側を開く。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aWheelchairCallOpensTheDoorOppositeTheOneUsedToBoard(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        BlockPos panelPos = helper.absolutePos(PANEL);
        int top = panelPos.getY() + RISE;
        CompoundTag settings = panel.settingsTag();
        settings.putBoolean("Through", true);
        CompoundTag entrances = new CompoundTag();
        entrances.putString(Integer.toString(top), FloorEntrances.BOTH.name());
        settings.put("Entrances", entrances);
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        car.setFixtures(car.fixtures().withWheelchairPanel(true));
        // 最下階で正面から乗った
        car.openDoor(Entrance.FRONT);
        helper.runAfterDelay(20, () -> car.pressWheelchairCarCall(1));
        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(!car.isMoving() && car.getY() == top && car.doorFractionAt(now, Entrance.REAR) > 0.99,
                    "the rear door should open");
            helper.assertTrue(car.doorFractionAt(now, Entrance.FRONT) == 0 && car.isDoorSlowClose(),
                    "only the rear door, held for the wheelchair");
        });
    }

    /** 二方向出入口で両側の操作盤から登録した行先は、2度押しした側だけ取り消され、反対側が残れば止まる。 */
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aDoublePressCancelsOnlyThatSidesCarCall(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        BlockPos panelPos = helper.absolutePos(PANEL);
        int top = panelPos.getY() + RISE;
        CompoundTag settings = panel.settingsTag();
        settings.putBoolean("Through", true);
        CompoundTag entrances = new CompoundTag();
        entrances.putString(Integer.toString(top), FloorEntrances.BOTH.name());
        settings.put("Entrances", entrances);
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        List<ButtonLayout.Button> buttons = car.buttons();
        List<Integer> callButtons = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).kind() == ButtonLayout.Kind.CAR_CALL && buttons.get(i).floor() == 1) {
                callButtons.add(i);
            }
        }
        int front = callButtons.get(0);
        int rear = callButtons.get(1);
        car.pressButton(front);
        car.pressButton(rear);
        car.pressButton(rear);
        car.pressButton(rear);
        helper.assertTrue(car.isButtonLit(front) && !car.isButtonLit(rear), "only the rear call should be cancelled");
        helper.assertTrue(car.isCarCallLit(1), "the car should still go there for the front call");
        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(!car.isMoving() && car.getY() == top && car.doorFractionAt(now, Entrance.FRONT) > 0.99,
                    "the front door should open");
            helper.assertTrue(car.doorFractionAt(now, Entrance.REAR) == 0, "the rear door should stay closed");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aCarCallFromTheRearPanelOpensOnlyTheRearDoor(GameTestHelper helper) {
        InstallerBlockEntity panel = placePanel(helper);
        BlockPos panelPos = helper.absolutePos(PANEL);
        int top = panelPos.getY() + RISE;
        CompoundTag settings = panel.settingsTag();
        settings.putBoolean("Through", true);
        CompoundTag entrances = new CompoundTag();
        entrances.putString(Integer.toString(top), FloorEntrances.BOTH.name());
        settings.put("Entrances", entrances);
        panel.applySettings(settings);
        ElevatorPlan plan = panel.plan();
        panel.install();
        ElevatorCarEntity car = carAt(helper, plan.carOrigin());
        car.pressCarCall(1, Entrance.REAR);
        // 奥の操作盤の行先ボタンだけが点き、保存して読み込み直しても（同期と同じデータ）点いたまま
        List<ButtonLayout.Button> buttons = car.buttons();
        List<Integer> callButtons = new ArrayList<>();
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).kind() == ButtonLayout.Kind.CAR_CALL && buttons.get(i).floor() == 1) {
                callButtons.add(i);
            }
        }
        helper.assertTrue(callButtons.size() == 2, "a car call button on each panel");
        helper.assertTrue(!car.isButtonLit(callButtons.get(0)) && car.isButtonLit(callButtons.get(1)), "only the rear button should light");
        ElevatorCarEntity copy = ModEntities.ELEVATOR_CAR.get().create(helper.getLevel());
        copy.load(car.saveWithoutId(new CompoundTag()));
        helper.assertTrue(copy.isButtonLit(callButtons.get(1)), "the rear car call should survive saving");
        // 2方向目（正面の操作盤）の登録も、クライアントへ送り直される
        car.pressCarCall(1, Entrance.FRONT);
        helper.assertTrue(car.isCarCallSynced(1, Entrance.REAR) && car.isCarCallSynced(1, Entrance.FRONT),
                "both sides should be sent to the client");
        helper.succeedWhen(() -> {
            long now = helper.getLevel().getGameTime();
            helper.assertTrue(!car.isMoving() && car.getY() == top && car.doorFractionAt(now, Entrance.REAR) > 0.99,
                    "the rear door should open");
            // 奥が先に登録されたので奥から開き、正面は奥が閉まってから。正面の応答灯は、それまで点いたまま
            helper.assertTrue(car.doorFractionAt(now, Entrance.FRONT) == 0, "the front door should wait");
            helper.assertTrue(!car.isButtonLit(callButtons.get(1)) && car.isButtonLit(callButtons.get(0)),
                    "the rear lamp should go out and the front lamp should stay lit");
            helper.assertTrue(car.isCarCallSynced(1, Entrance.FRONT) && !car.isCarCallSynced(1, Entrance.REAR),
                    "the client should see the same lamps");
        });
    }
}

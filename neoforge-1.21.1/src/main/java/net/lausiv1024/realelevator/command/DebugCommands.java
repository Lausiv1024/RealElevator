package net.lausiv1024.realelevator.command;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.BiFunction;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.announce.ChimeType;
import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.CallType;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.core.panel.FixtureConfig;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.entity.ElevatorGroups;
import net.lausiv1024.realelevator.entity.ElevatorTracker;
import net.lausiv1024.realelevator.entity.LandingDoorEntity;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * フェーズ1の動作確認用コマンド。ウィザード設置とボタンができたら置き換える。
 *
 * <pre>
 * /realelevator spawn &lt;かごを置く階の床面&gt; &lt;機種&gt; &lt;正面の向き&gt; &lt;速度&gt; &lt;ほかの停止階の床面のY…&gt;
 * /realelevator call &lt;階&gt;    かご呼び（行先ボタン）
 * /realelevator hall &lt;階&gt; [up|down]   乗場呼び（省略時は最上階なら下り、それ以外は上り）
 * /realelevator open | close   開・閉ボタン
 * /realelevator remove         かごと乗場の扉を撤去
 * /realelevator config panel right|both                    操作盤を右側だけ／両側に置く
 * /realelevator config left_indicator true|false           両側の操作盤のとき、左側にも表示器を付けるか
 * /realelevator config wheelchair_panel true|false         車椅子用操作盤と乗場の車椅子用ボタンを付けるか
 * /realelevator config hall_indicator over_door|over_buttons  乗場の階床表示器を扉の上／ボタンの上に置く
 * /realelevator config hall_lantern over_door|beside_door|none  ホールランタンを扉の上／扉の横に置く、または付けない
 * /realelevator config chime true|false                       到着チャイムを鳴らすか
 * /realelevator config arrival_announcement true|false        到着して戸を開くときに「◯階です。」を流すか
 * /realelevator config close_announcement true|false          戸を閉める前に「ドアが閉まります。」を流すか
 * </pre>
 *
 * call 以降は、半径32ブロック以内で最も近いかごに対して行う。
 */
public final class DebugCommands {
    private static final double CAR_RANGE = 32.0;

    private static final DynamicCommandExceptionType UNKNOWN_VALUE =
            new DynamicCommandExceptionType(value -> Component.literal("不明な値です: " + value));
    /** 停止階の間隔の最小値。かごの外形の高さ（約3.7ブロック）より広くする。 */
    private static final int MIN_FLOOR_SPACING = 4;

    private static final DynamicCommandExceptionType BAD_FLOORS =
            new DynamicCommandExceptionType(message -> Component.literal(String.valueOf(message)));
    private static final SimpleCommandExceptionType NO_CAR =
            new SimpleCommandExceptionType(Component.literal("近くにかごがありません"));

    private interface CarAction {
        int run(CommandContext<CommandSourceStack> context, ElevatorCarEntity car) throws CommandSyntaxException;
    }

    private DebugCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("realelevator")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("spawn")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("model", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(names(CarModel.values()), b))
                                        .then(Commands.argument("facing", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                        Direction.Plane.HORIZONTAL.stream().map(Direction::getSerializedName), b))
                                                .then(Commands.argument("speed", StringArgumentType.word())
                                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(names(SpeedSpec.values()), b))
                                                        .then(Commands.argument("floors", StringArgumentType.greedyString())
                                                                .executes(DebugCommands::spawn)))))))
                .then(Commands.literal("call")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1))
                                .executes(c -> withNearestCar(c, (ctx, car) -> call(ctx, car, null)))))
                .then(Commands.literal("hall")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1))
                                .executes(c -> withNearestCar(c, (ctx, car) -> call(ctx, car,
                                        IntegerArgumentType.getInteger(ctx, "floor") >= car.floorCount() ? TravelDirection.DOWN : TravelDirection.UP)))
                                .then(Commands.argument("direction", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(new String[] {"up", "down"}, b))
                                        .executes(c -> withNearestCar(c, (ctx, car) -> call(ctx, car,
                                                parse(TravelDirection.class, StringArgumentType.getString(ctx, "direction"))))))))
                .then(Commands.literal("open")
                        .executes(c -> withNearestCar(c, DebugCommands::open)))
                .then(Commands.literal("close")
                        .executes(c -> withNearestCar(c, DebugCommands::close)))
                .then(Commands.literal("remove")
                        .executes(c -> withNearestCar(c, DebugCommands::remove)))
                .then(Commands.literal("config")
                        .then(Commands.literal("panel")
                                .then(enumValue(FixtureConfig.PanelSides.class, FixtureConfig::withPanelSides)))
                        .then(Commands.literal("left_indicator")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> withNearestCar(c, (ctx, car) -> configure(ctx, car,
                                                car.fixtures().withLeftPanelIndicator(BoolArgumentType.getBool(ctx, "value")))))))
                        .then(Commands.literal("wheelchair_panel")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> withNearestCar(c, (ctx, car) -> configure(ctx, car,
                                                car.fixtures().withWheelchairPanel(BoolArgumentType.getBool(ctx, "value")))))))
                        .then(Commands.literal("hall_indicator")
                                .then(enumValue(FixtureConfig.HallIndicator.class, FixtureConfig::withHallIndicator)))
                        .then(Commands.literal("hall_lantern")
                                .then(enumValue(FixtureConfig.HallLanternPlacement.class, FixtureConfig::withHallLantern)))
                        .then(Commands.literal("arrival_announcement")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> withNearestCar(c, (ctx, car) -> configureAnnouncements(ctx, car,
                                                car.announcements().withArrival(BoolArgumentType.getBool(ctx, "value")))))))
                        .then(Commands.literal("chime")
                                .then(Commands.argument("value", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(names(ChimeType.values()), b))
                                        .executes(c -> withNearestCar(c, (ctx, car) -> configureAnnouncements(ctx, car,
                                                car.announcements().withChime(parse(ChimeType.class, StringArgumentType.getString(ctx, "value"))))))))
                        .then(Commands.literal("close_announcement")
                                .then(Commands.argument("value", BoolArgumentType.bool())
                                        .executes(c -> withNearestCar(c, (ctx, car) -> configureAnnouncements(ctx, car,
                                                car.announcements().withDoorClose(BoolArgumentType.getBool(ctx, "value")))))))));
    }

    private static int spawn(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CarModel model = parse(CarModel.class, StringArgumentType.getString(context, "model"));
        String facingName = StringArgumentType.getString(context, "facing");
        Direction facing = Direction.byName(facingName);
        if (facing == null || facing.getAxis().isVertical()) {
            throw UNKNOWN_VALUE.create(facingName);
        }
        SpeedSpec speed = parse(SpeedSpec.class, StringArgumentType.getString(context, "speed"));
        int[] floors = parseFloors(pos.getY(), StringArgumentType.getString(context, "floors"));

        ElevatorCarEntity car = ModEntities.ELEVATOR_CAR.get().create(level);
        if (car == null) {
            return 0;
        }
        car.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        car.setup(model, facing, floors, speed);
        level.addFreshEntity(car);
        for (int floorY : car.floors()) {
            LandingDoorEntity door = ModEntities.LANDING_DOOR.get().create(level);
            if (door != null) {
                door.moveTo(car.getX(), floorY, car.getZ(), 0, 0);
                door.setup(car);
                level.addFreshEntity(door);
            }
        }
        source.sendSuccess(() -> Component.literal("かごを設置しました（" + model.name() + "、"
                + facing.getSerializedName() + "向き、" + speed.name().toLowerCase(Locale.ROOT) + "、"
                + floors.length + "階床：Y=" + Arrays.toString(floors) + "）"), true);
        return 1;
    }

    /** direction が null ならかご呼び、そうでなければその方向の乗場呼び。 */
    /** かごを置く階と、スペース区切りで並べたほかの停止階の床面のYから、停止階の一覧（下から順）を作る。 */
    private static int[] parseFloors(int startY, String text) throws CommandSyntaxException {
        String[] tokens = text.trim().split("\\s+");
        int[] floors = new int[tokens.length + 1];
        floors[0] = startY;
        for (int i = 0; i < tokens.length; i++) {
            try {
                floors[i + 1] = Integer.parseInt(tokens[i]);
            } catch (NumberFormatException e) {
                throw UNKNOWN_VALUE.create(tokens[i]);
            }
        }
        Arrays.sort(floors);
        for (int i = 1; i < floors.length; i++) {
            if (floors[i] - floors[i - 1] < MIN_FLOOR_SPACING) {
                throw BAD_FLOORS.create("停止階の間隔は" + MIN_FLOOR_SPACING + "ブロック以上にしてください（Y=" + floors[i - 1] + "と" + floors[i] + "）");
            }
        }
        return floors;
    }

    private static int call(CommandContext<CommandSourceStack> context, ElevatorCarEntity car, TravelDirection direction)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int floor = IntegerArgumentType.getInteger(context, "floor");
        PressResult result = direction == null ? car.pressCarCall(floor - 1) : car.pressHallCall(floor - 1, direction);
        return switch (result) {
            case REGISTERED, ALREADY_REGISTERED -> {
                source.sendSuccess(() -> Component.literal(floor + "階の呼びを登録しました"), false);
                yield 1;
            }
            case SERVED_HERE -> {
                source.sendSuccess(() -> Component.literal(floor + "階で戸を開きます"), false);
                yield 1;
            }
            case INVALID -> throw UNKNOWN_VALUE.create(floor + "階" + (direction != null ? "の" + direction.name().toLowerCase(Locale.ROOT) : "")
                    + "（このかごは" + car.floorCount() + "階まで）");
        };
    }

    private static int open(CommandContext<CommandSourceStack> context, ElevatorCarEntity car) {
        if (!car.openDoor(CallType.CAR)) {
            context.getSource().sendFailure(Component.literal("走行中は開きません"));
            return 0;
        }
        return 1;
    }

    private static int close(CommandContext<CommandSourceStack> context, ElevatorCarEntity car) {
        if (!car.closeDoor()) {
            context.getSource().sendFailure(Component.literal("開放待機中のときだけ閉じられます"));
            return 0;
        }
        return 1;
    }

    private static int remove(CommandContext<CommandSourceStack> context, ElevatorCarEntity car) {
        ServerLevel level = context.getSource().getLevel();
        for (LandingDoorEntity door : ElevatorTracker.landingDoors(level)) {
            if (door.carUuid().filter(car.getUUID()::equals).isPresent()) {
                door.discard();
            }
        }
        car.discard();
        context.getSource().sendSuccess(() -> Component.literal("かごと乗場の扉を撤去しました"), true);
        return 1;
    }

    /** 列挙型の値を1つ受け取って、かごの配置の設定を変える引数。 */
    private static <E extends Enum<E>> RequiredArgumentBuilder<CommandSourceStack, String> enumValue(
            Class<E> type, BiFunction<FixtureConfig, E, FixtureConfig> apply) {
        return Commands.argument("value", StringArgumentType.word())
                .suggests((c, b) -> SharedSuggestionProvider.suggest(names(type.getEnumConstants()), b))
                .executes(c -> withNearestCar(c, (ctx, car) -> configure(ctx, car,
                        apply.apply(car.fixtures(), parse(type, StringArgumentType.getString(ctx, "value"))))));
    }

    private static int configure(CommandContext<CommandSourceStack> context, ElevatorCarEntity car, FixtureConfig fixtures) {
        car.setFixtures(fixtures);
        context.getSource().sendSuccess(() -> Component.literal("配置を変更しました：操作盤 "
                + fixtures.panelSides().name().toLowerCase(Locale.ROOT)
                + "、左の表示器 " + fixtures.leftPanelIndicator()
                + "、乗場の表示器 " + fixtures.hallIndicator().name().toLowerCase(Locale.ROOT)
                + "、ホールランタン " + fixtures.hallLantern().name().toLowerCase(Locale.ROOT)
                + "、車椅子用操作盤 " + fixtures.wheelchairPanel()), false);
        return 1;
    }

    private static int configureAnnouncements(CommandContext<CommandSourceStack> context, ElevatorCarEntity car,
                                              AnnouncementConfig announcements) {
        car.setAnnouncements(announcements);
        ElevatorGroups.shareAnnouncements(car);
        context.getSource().sendSuccess(() -> Component.literal("アナウンスの設定を変更しました：到着 " + announcements.arrival()
                + "、戸閉 " + announcements.doorClose() + "、チャイム " + announcements.chime().name().toLowerCase(Locale.ROOT)), false);
        return 1;
    }

    private static int withNearestCar(CommandContext<CommandSourceStack> context, CarAction action) throws CommandSyntaxException {
        Vec3 origin = context.getSource().getPosition();
        ElevatorCarEntity car = ElevatorTracker.cars(context.getSource().getLevel()).stream()
                .filter(c -> !c.isRemoved())
                .filter(c -> horizontalDistanceSqr(c, origin) <= CAR_RANGE * CAR_RANGE)
                .min(Comparator.comparingDouble(c -> horizontalDistanceSqr(c, origin)))
                .orElseThrow(NO_CAR::create);
        return action.run(context, car);
    }

    private static double horizontalDistanceSqr(Entity entity, Vec3 pos) {
        double dx = entity.getX() - pos.x;
        double dz = entity.getZ() - pos.z;
        return dx * dx + dz * dz;
    }

    private static String[] names(Enum<?>[] values) {
        return Arrays.stream(values).map(v -> v.name().toLowerCase(Locale.ROOT)).toArray(String[]::new);
    }

    private static <E extends Enum<E>> E parse(Class<E> type, String name) throws CommandSyntaxException {
        try {
            return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw UNKNOWN_VALUE.create(name);
        }
    }
}

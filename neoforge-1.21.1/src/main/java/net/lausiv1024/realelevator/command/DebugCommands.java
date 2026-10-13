package net.lausiv1024.realelevator.command;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.control.CallType;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
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
 * /realelevator spawn &lt;下階の床面&gt; &lt;機種&gt; &lt;正面の向き&gt; &lt;上階の床面のY&gt; [速度]
 * /realelevator call &lt;階&gt;    かご呼び（行先ボタン）
 * /realelevator hall &lt;階&gt;    乗場呼び
 * /realelevator open | close   開・閉ボタン
 * /realelevator remove         かごと乗場の扉を撤去
 * </pre>
 *
 * call 以降は、半径32ブロック以内で最も近いかごに対して行う。
 */
public final class DebugCommands {
    private static final double CAR_RANGE = 32.0;

    private static final DynamicCommandExceptionType UNKNOWN_VALUE =
            new DynamicCommandExceptionType(value -> Component.literal("不明な値です: " + value));
    private static final SimpleCommandExceptionType SAME_FLOOR =
            new SimpleCommandExceptionType(Component.literal("上階は下階と別の高さにしてください"));
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
                                                .then(Commands.argument("upperY", IntegerArgumentType.integer())
                                                        .executes(c -> spawn(c, SpeedSpec.NORMAL_LOW))
                                                        .then(Commands.argument("speed", StringArgumentType.word())
                                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(names(SpeedSpec.values()), b))
                                                                .executes(c -> spawn(c, parse(SpeedSpec.class, StringArgumentType.getString(c, "speed"))))))))))
                .then(Commands.literal("call")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1))
                                .executes(c -> withNearestCar(c, (ctx, car) -> call(ctx, car, CallType.CAR)))))
                .then(Commands.literal("hall")
                        .then(Commands.argument("floor", IntegerArgumentType.integer(1))
                                .executes(c -> withNearestCar(c, (ctx, car) -> call(ctx, car, CallType.HALL)))))
                .then(Commands.literal("open")
                        .executes(c -> withNearestCar(c, DebugCommands::open)))
                .then(Commands.literal("close")
                        .executes(c -> withNearestCar(c, DebugCommands::close)))
                .then(Commands.literal("remove")
                        .executes(c -> withNearestCar(c, DebugCommands::remove))));
    }

    private static int spawn(CommandContext<CommandSourceStack> context, SpeedSpec speed) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        CarModel model = parse(CarModel.class, StringArgumentType.getString(context, "model"));
        String facingName = StringArgumentType.getString(context, "facing");
        Direction facing = Direction.byName(facingName);
        if (facing == null || facing.getAxis().isVertical()) {
            throw UNKNOWN_VALUE.create(facingName);
        }
        int upperY = IntegerArgumentType.getInteger(context, "upperY");
        if (upperY == pos.getY()) {
            throw SAME_FLOOR.create();
        }

        ElevatorCarEntity car = ModEntities.ELEVATOR_CAR.get().create(level);
        if (car == null) {
            return 0;
        }
        car.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        car.setup(model, facing, new int[] {pos.getY(), upperY}, speed);
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
                + facing.getSerializedName() + "向き、" + speed.name().toLowerCase(Locale.ROOT) + "）"), true);
        return 1;
    }

    private static int call(CommandContext<CommandSourceStack> context, ElevatorCarEntity car, CallType callType)
            throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        int floor = IntegerArgumentType.getInteger(context, "floor");
        return switch (car.call(floor - 1, callType)) {
            case ACCEPTED -> {
                source.sendSuccess(() -> Component.literal(floor + "階の呼びを受け付けました"), false);
                yield 1;
            }
            case OPENED -> {
                source.sendSuccess(() -> Component.literal(floor + "階で戸を開きます"), false);
                yield 1;
            }
            case BUSY -> {
                source.sendFailure(Component.literal("ほかの呼びに応えている途中です"));
                yield 0;
            }
            case INVALID -> throw UNKNOWN_VALUE.create(floor + "階（このかごは" + car.floorCount() + "階まで）");
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

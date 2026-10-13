package net.lausiv1024.realelevator.command;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;

import net.lausiv1024.realelevator.core.car.CarModel;
import net.lausiv1024.realelevator.core.motion.SpeedSpec;
import net.lausiv1024.realelevator.entity.CarTracker;
import net.lausiv1024.realelevator.entity.ElevatorCarEntity;
import net.lausiv1024.realelevator.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * フェーズ1の動作確認用コマンド。ウィザード設置とボタンができたら置き換える。
 *
 * <pre>
 * /realelevator spawn &lt;下階の床面&gt; &lt;機種&gt; &lt;正面の向き&gt; &lt;上階の床面のY&gt; [速度]
 * /realelevator call &lt;階&gt;
 * </pre>
 */
public final class DebugCommands {
    private static final double CALL_RANGE = 32.0;

    private static final DynamicCommandExceptionType UNKNOWN_VALUE =
            new DynamicCommandExceptionType(value -> Component.literal("不明な値です: " + value));
    private static final SimpleCommandExceptionType SAME_FLOOR =
            new SimpleCommandExceptionType(Component.literal("上階は下階と別の高さにしてください"));
    private static final SimpleCommandExceptionType NO_CAR =
            new SimpleCommandExceptionType(Component.literal("近くにかごがありません"));

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
                                .executes(DebugCommands::call))));
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
        source.sendSuccess(() -> Component.literal("かごを設置しました（" + model.name() + "、"
                + facing.getSerializedName() + "向き、" + speed.name().toLowerCase(Locale.ROOT) + "）"), true);
        return 1;
    }

    private static int call(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        Vec3 origin = source.getPosition();
        Optional<ElevatorCarEntity> nearest = CarTracker.cars(source.getLevel()).stream()
                .filter(car -> !car.isRemoved())
                .filter(car -> horizontalDistanceSqr(car, origin) <= CALL_RANGE * CALL_RANGE)
                .min(Comparator.comparingDouble(car -> horizontalDistanceSqr(car, origin)));
        ElevatorCarEntity car = nearest.orElseThrow(NO_CAR::create);

        int floor = IntegerArgumentType.getInteger(context, "floor");
        if (floor > car.floorCount()) {
            throw UNKNOWN_VALUE.create(floor + "階（このかごは" + car.floorCount() + "階まで）");
        }
        if (!car.departTo(floor - 1)) {
            source.sendFailure(Component.literal(car.isMoving() ? "走行中です" : "既に" + floor + "階にいます"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(floor + "階へ発車しました"), false);
        return 1;
    }

    private static double horizontalDistanceSqr(ElevatorCarEntity car, Vec3 pos) {
        double dx = car.getX() - pos.x;
        double dz = car.getZ() - pos.z;
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

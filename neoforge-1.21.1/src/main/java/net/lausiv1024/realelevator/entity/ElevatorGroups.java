package net.lausiv1024.realelevator.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.lausiv1024.realelevator.block.ControllerBlockEntity;
import net.lausiv1024.realelevator.core.announce.AnnouncementConfig;
import net.lausiv1024.realelevator.core.car.Entrance;
import net.lausiv1024.realelevator.core.control.CollectiveControl.PressResult;
import net.lausiv1024.realelevator.core.control.GroupDispatcher;
import net.lausiv1024.realelevator.core.control.TravelDirection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * 複数台の群管理。同じ群のかごで乗場呼びを分け合う。
 *
 * <ul>
 * <li>群は保守用レンチでつなぐ（{@link #join}、{@link #leave}）。最大{@value #MAX_CARS}台、同じディメンションの、水平に
 * {@value #MAX_DISTANCE}ブロック以内のかご。停止階はかごごとに違ってよい。</li>
 * <li>群のどの乗場で押した乗場呼びも、その階（同じY）に止まるかごのうち、着くまでの時間の見積もりがいちばん短いかご
 * （{@link GroupDispatcher}）に割り当て、そのかごの呼びとして登録する。割り当てたら変えない。</li>
 * <li>割り当てたらすぐ、そのかごのその階のホールランタンを点け（点滅せずに点灯）、チャイムの種類ごとの予報の音で知らせる
 * （即時予報）。到着すると今までどおり点滅に変わる。</li>
 * <li>アナウンスの設定（到着・戸閉のアナウンスとチャイムの種類）は群でそろえる。1台を変えると群のほかのかごも変わり
 * （{@link #shareAnnouncements}）、群に入れたかごは群の側の設定になる。</li>
 * <li>見積もりが同じかごが複数あれば（同じ階で待っているかごなど）、押された乗場にいちばん近いかごに割り当てる。</li>
 * <li>乗場のボタンの応答灯は、群のどれかのかごにその階・その向きの呼びがあれば、群のすべての乗場で点く。</li>
 * <li>車椅子用のボタンの呼びは、車椅子用操作盤のあるかごだけに割り当てる（応えられるかごがなければ登録しない）。</li>
 * </ul>
 */
public final class ElevatorGroups {
    public static final int MAX_CARS = 8;
    public static final int MAX_DISTANCE = 32;

    private ElevatorGroups() {
    }

    /** car と同じ群の、読み込まれているかご（car を含む）。群に入っていなければ car だけ。 */
    public static List<ElevatorCarEntity> members(ElevatorCarEntity car) {
        Optional<UUID> group = car.groupId();
        if (group.isEmpty()) {
            return List.of(car);
        }
        List<ElevatorCarEntity> members = new ArrayList<>();
        for (ElevatorCarEntity other : ElevatorTracker.cars(car.level())) {
            if (!other.isRemoved() && other.groupId().equals(group)) {
                members.add(other);
            }
        }
        if (!members.contains(car)) {
            members.add(car);
        }
        return members;
    }

    /**
     * かご origin の停止階 floor の乗場（側 side）で押された乗場呼び。群に入っていればいちばん早く着くかごに割り当てる。
     * 車椅子用のボタン（wheelchair）の呼びは、車椅子用操作盤のあるかごだけに割り当てる。応えられるかごがなければ登録しない。
     */
    public static PressResult pressHallCall(ElevatorCarEntity origin, int floor, TravelDirection direction, Entrance side, boolean wheelchair) {
        List<ElevatorCarEntity> members = members(origin);
        if (members.size() <= 1) {
            return origin.pressHallCall(floor, direction, side, wheelchair);
        }
        if (wheelchair) {
            members = members.stream().filter(member -> member.fixtures().wheelchairPanel()).toList();
            if (members.isEmpty()) {
                return PressResult.INVALID;
            }
        }
        double y = origin.floors()[floor];
        // すでに群のどれかのかご（車椅子用なら車椅子対応のかご）に割り当ててあれば、そのかごの呼びとして押す
        // （車椅子用の印や、その場で開くときのため）
        for (ElevatorCarEntity member : members) {
            int index = member.floorIndexAt(y);
            if (index >= 0 && member.isHallCallLit(index, direction)) {
                return member.pressHallCall(index, direction, sideFor(member, origin, side, index), wheelchair);
            }
        }
        List<GroupDispatcher.CarState> states = members.stream().map(member -> member.groupState(y)).toList();
        // 見積もりが同じなら、押された乗場にいちばん近いかごにする
        Vec3 pressed = origin.landingPoint(floor, side);
        List<Double> distances = members.stream().map(member -> {
            int index = member.floorIndexAt(y);
            if (index < 0) {
                return Double.POSITIVE_INFINITY;
            }
            Vec3 landing = member.landingPoint(index, sideFor(member, origin, side, index));
            return Math.hypot(landing.x - pressed.x, landing.z - pressed.z);
        }).toList();
        int chosen = GroupDispatcher.choose(states, y, direction, distances);
        if (chosen < 0) {
            return PressResult.INVALID;
        }
        ElevatorCarEntity car = members.get(chosen);
        int index = car.floorIndexAt(y);
        PressResult result = car.pressHallCall(index, direction, sideFor(car, origin, side, index), wheelchair);
        if (result == PressResult.REGISTERED) {
            car.announceAssignment(index);
        }
        return result;
    }

    /** かご origin の停止階 floor の乗場の、乗場呼び（車椅子用なら wheelchair）の応答灯。群のどれかのかごに呼びがあれば点く。 */
    public static boolean isHallCallLit(ElevatorCarEntity origin, int floor, TravelDirection direction, Entrance side, boolean wheelchair) {
        List<ElevatorCarEntity> members = members(origin);
        if (members.size() <= 1) {
            return wheelchair ? origin.isWheelchairHallCallLit(floor, direction, side) : origin.isHallCallLit(floor, direction, side);
        }
        double y = origin.floors()[floor];
        for (ElevatorCarEntity member : members) {
            int index = member.floorIndexAt(y);
            if (index < 0) {
                continue;
            }
            for (Entrance entrance : member.entrancesAt(index)) {
                boolean lit = wheelchair ? member.isWheelchairHallCallLit(index, direction, entrance)
                        : member.isHallCallLit(index, direction, entrance);
                if (lit) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 群の別のかごに割り当てるときに開く側。押したかごならその側、ほかのかごなら、その階の正面（なければ奥）。 */
    private static Entrance sideFor(ElevatorCarEntity car, ElevatorCarEntity origin, Entrance side, int floor) {
        if (car == origin) {
            return side;
        }
        return car.entrancesAt(floor).contains(Entrance.FRONT) ? Entrance.FRONT : Entrance.REAR;
    }

    /**
     * かご target を、かご selected と同じ群に入れる。selected が群に入っていなければ、2台で新しい群を作る。target がほかの群に
     * 入っていれば、そこから外す。
     *
     * @return 結果を伝える文
     */
    public static Component join(ElevatorCarEntity selected, ElevatorCarEntity target) {
        if (selected == target) {
            return Component.translatable("message.realelevator.group.same");
        }
        if (selected.level() != target.level()
                || Math.hypot(selected.getX() - target.getX(), selected.getZ() - target.getZ()) > MAX_DISTANCE) {
            return Component.translatable("message.realelevator.group.too_far", MAX_DISTANCE);
        }
        UUID group = selected.groupId().orElseGet(UUID::randomUUID);
        if (target.groupId().equals(Optional.of(group))) {
            return Component.translatable("message.realelevator.group.already", members(target).size());
        }
        List<ElevatorCarEntity> current = selected.groupId().isPresent() ? members(selected) : List.of(selected);
        if (current.size() >= MAX_CARS) {
            return Component.translatable("message.realelevator.group.full", MAX_CARS);
        }
        leave(target);
        selected.setGroupId(group);
        target.setGroupId(group);
        // 入れたかごのアナウンスの設定は、群の側（選んだかご）に合わせる
        shareAnnouncements(selected);
        return Component.translatable("message.realelevator.group.joined", members(target).size());
    }

    /**
     * かご source のアナウンスの設定を、同じ群の読み込まれているほかのかごと、その頂部の制御盤の計画にもそろえる。
     * 群に入っていなければ何もしない。
     */
    public static void shareAnnouncements(ElevatorCarEntity source) {
        AnnouncementConfig announcements = source.announcements();
        for (ElevatorCarEntity other : members(source)) {
            if (other == source || other.announcements().equals(announcements)) {
                continue;
            }
            other.setAnnouncements(announcements);
            ControllerBlockEntity controller = ControllerBlockEntity.of(other);
            if (controller != null) {
                controller.setAnnouncements(announcements);
            }
        }
    }

    /**
     * かご car を群から外す。残りが1台になったら、その群はなくす。
     *
     * @return 結果を伝える文
     */
    public static Component leave(ElevatorCarEntity car) {
        if (car.groupId().isEmpty()) {
            return Component.translatable("message.realelevator.group.none");
        }
        List<ElevatorCarEntity> others = new ArrayList<>(members(car));
        others.remove(car);
        car.setGroupId(null);
        if (others.size() == 1) {
            others.get(0).setGroupId(null);
        }
        return Component.translatable("message.realelevator.group.left");
    }
}

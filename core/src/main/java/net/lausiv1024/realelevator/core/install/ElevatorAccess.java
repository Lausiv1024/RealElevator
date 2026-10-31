package net.lausiv1024.realelevator.core.install;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * エレベーターを管理できるプレイヤー。設置者と、設置者が許可したプレイヤー（名前も覚えておき、一覧に出す）。
 *
 * <p>設置者の記録がないエレベーター（この機能より前に設置したもの）は、最初に管理の操作をしたプレイヤーを設置者にする（{@link #claim}）。
 */
public final class ElevatorAccess {
    /** 許可できるプレイヤーの数の上限。 */
    public static final int MAX_ALLOWED = 16;

    private UUID owner;
    private String ownerName = "";
    private final Map<UUID, String> allowed = new LinkedHashMap<>();

    public boolean hasOwner() {
        return owner != null;
    }

    public Optional<UUID> owner() {
        return Optional.ofNullable(owner);
    }

    public String ownerName() {
        return ownerName;
    }

    /** 設置者を決める。もう決まっていれば変えない。 */
    public boolean claim(UUID id, String name) {
        if (owner != null) {
            return false;
        }
        owner = id;
        ownerName = name;
        return true;
    }

    public boolean isOwner(UUID id) {
        return id.equals(owner);
    }

    /** id のプレイヤーが管理できるか（設置者か、許可されたプレイヤー）。 */
    public boolean canManage(UUID id) {
        return isOwner(id) || allowed.containsKey(id);
    }

    /** 許可する。設置者本人、すでに許可されたプレイヤー、上限を超えるときは何もせず false を返す。 */
    public boolean allow(UUID id, String name) {
        if (isOwner(id) || allowed.containsKey(id) || allowed.size() >= MAX_ALLOWED) {
            return false;
        }
        allowed.put(id, name);
        return true;
    }

    /** 名前 name のプレイヤーの許可を取り消す。大文字と小文字は区別しない。 */
    public boolean revoke(String name) {
        return allowed.values().removeIf(name::equalsIgnoreCase);
    }

    /** 許可されたプレイヤー（許可した順）。 */
    public Map<UUID, String> allowed() {
        return Collections.unmodifiableMap(allowed);
    }
}

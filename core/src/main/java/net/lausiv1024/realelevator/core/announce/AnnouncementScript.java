package net.lausiv1024.realelevator.core.announce;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.lausiv1024.realelevator.core.control.TravelDirection;

/**
 * アナウンスの文面のテンプレート。
 *
 * <ul>
 * <li>到着：「◯階です。」</li>
 * <li>行先方向：「上へ参ります。」「下へ参ります。」（方向が決まっていなければ何も流さない）</li>
 * <li>戸閉：「ドアが閉まります。」</li>
 * </ul>
 *
 * <p>同梱音声では、あらかじめ録った音声のかたまり（クリップ）をつなげて鳴らす。階は読みが崩れないよう、
 * 「◯階です。」までを1つのクリップにする。対応している階名は、数字の階（1〜{@value #MAX_FLOOR}）、
 * 地下（B1〜B{@value #MAX_BASEMENT}）、屋上（R）。それ以外の階名は、階のクリップを鳴らさない。
 */
public final class AnnouncementScript {
    public static final int MAX_FLOOR = 50;
    public static final int MAX_BASEMENT = 5;

    public static final String CLIP_UP = "dir_up";
    public static final String CLIP_DOWN = "dir_down";
    public static final String CLIP_DOOR_CLOSE = "door_close";

    /** 自動で戸を閉めるとき、「ドアが閉まります。」を流してから閉め始めるまでの時間 [tick]。 */
    public static final int DOOR_CLOSE_WAIT_TICKS = 30;
    /**
     * 行先方向（「上へ参ります。」「下へ参ります。」）を流している時間 [tick]。この間に戸を閉めるときは、
     * 実機と同じく「ドアが閉まります。」を流さない。サーバーは音声の長さを知らないので固定値とする。
     */
    public static final int DIRECTION_TICKS = 24;

    private static final Pattern NUMBER = Pattern.compile("([1-9][0-9]*)");
    private static final Pattern BASEMENT = Pattern.compile("B([1-9][0-9]*)");

    private AnnouncementScript() {
    }

    /** 同梱音声で鳴らすクリップの並び。 */
    public static List<String> clips(Announcement announcement) {
        List<String> clips = new ArrayList<>(2);
        switch (announcement.type()) {
            case ARRIVAL -> {
                String floor = floorClip(announcement.floorLabel());
                if (floor != null) {
                    clips.add(floor);
                }
            }
            case DIRECTION -> {
                if (announcement.direction() == TravelDirection.UP) {
                    clips.add(CLIP_UP);
                } else if (announcement.direction() == TravelDirection.DOWN) {
                    clips.add(CLIP_DOWN);
                }
            }
            case DOOR_CLOSE -> clips.add(CLIP_DOOR_CLOSE);
        }
        return clips;
    }

    /** 読み上げる文。音声合成で鳴らすときに使う。 */
    public static String text(Announcement announcement) {
        return switch (announcement.type()) {
            case ARRIVAL -> {
                String floor = floorSpeech(announcement.floorLabel());
                yield floor != null ? floor + "です。" : "";
            }
            case DIRECTION -> switch (announcement.direction()) {
                case UP -> "上へ参ります。";
                case DOWN -> "下へ参ります。";
                case NONE -> "";
            };
            case DOOR_CLOSE -> "ドアが閉まります。";
        };
    }

    /** 階名の「◯階です。」のクリップの名前。対応していない階名なら null。 */
    public static String floorClip(String label) {
        String normalized = label.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("R")) {
            return "floor_r";
        }
        Matcher basement = BASEMENT.matcher(normalized);
        if (basement.matches() && Integer.parseInt(basement.group(1)) <= MAX_BASEMENT) {
            return "floor_b" + basement.group(1);
        }
        Matcher number = NUMBER.matcher(normalized);
        if (number.matches() && Integer.parseInt(number.group(1)) <= MAX_FLOOR) {
            return "floor_" + number.group(1);
        }
        return null;
    }

    /** 階名の読み（「3階」「地下1階」「屋上」）。対応していない階名なら null。 */
    public static String floorSpeech(String label) {
        String normalized = label.trim().toUpperCase(Locale.ROOT);
        if (normalized.equals("R")) {
            return "屋上";
        }
        Matcher basement = BASEMENT.matcher(normalized);
        if (basement.matches()) {
            return "地下" + basement.group(1) + "階";
        }
        Matcher number = NUMBER.matcher(normalized);
        if (number.matches()) {
            return number.group(1) + "階";
        }
        return null;
    }
}

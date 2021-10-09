package lausiv1024.util;

import net.minecraft.util.IStringSerializable;

import java.util.Locale;

public enum EleSurfaceType implements IStringSerializable {
    OAK(0),
    DARK_OAK(1),
    BIRCH(2),
    JUNGLE(3),
    ACACIA(4),
    SPRUCE(5),
    CRIMSON(6),
    WARPED(7),
    ANDESITE(8),
    DIORITE(9),
    GRANITE(10),
    SMOOTH_STONE(11),
    WHITE(12),
    LIGHT_GRAY(13),
    YELLOW(14),
    PINK(15),
    LIME(16),
    CYAN(17),
    BLUE(18),
    BROWN(19),
    RED(20),
    GREEN(21),
    GRAY(22),
    BLACK(23),
    STAINLESS(24);

    private int id;
    EleSurfaceType(int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Deprecated
    public static EleSurfaceType getSurfaceFromId(int id){//ValueOfが使えることに気づいた
        if (id <= 24){
            switch (id){
                case 0:
                    return OAK;
                case 1:
                    return DARK_OAK;
                case 2:
                    return BIRCH;
                case 3:
                    return JUNGLE;
                case 4:
                    return ACACIA;
                case 5:
                    return SPRUCE;
                case 6:
                    return CRIMSON;
                case 7:
                    return WARPED;
                case 8:
                    return ANDESITE;
                case 9:
                    return DIORITE;
                case 10:
                    return GRANITE;
                case 11:
                    return SMOOTH_STONE;
                case 12:
                    return WHITE;
                case 13:
                    return LIGHT_GRAY;
                case 14:
                    return YELLOW;
                case 15:
                    return PINK;
                case 16:
                    return LIME;
                case 17:
                    return CYAN;
                case 18:
                    return BLUE;
                case 19:
                    return BROWN;
                case 20:
                    return RED;
                case 21:
                    return GREEN;
                case 22:
                    return GRAY;
                case 23:
                    return  BLACK;
                case 24:
                    return STAINLESS;
            }
        }
        return null;
    }
}

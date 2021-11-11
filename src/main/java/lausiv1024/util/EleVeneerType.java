package lausiv1024.util;

import net.minecraft.util.IStringSerializable;

import javax.annotation.Nullable;
import java.util.Locale;

public enum EleVeneerType implements IStringSerializable {
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
    EleVeneerType(int id){
        this.id = id;
    }

    public int getId() {
        return id;
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static EleVeneerType getSurfaceFromId(int id){
        if (id <= 24){
            EleVeneerType[] v = EleVeneerType.values();
            for (EleVeneerType s : v){
                if (id == s.getId()) return s;
            }
        }
        return null;
    }
}

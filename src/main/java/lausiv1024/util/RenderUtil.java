package lausiv1024.util;

import net.minecraft.client.Minecraft;

public class RenderUtil {
    public static float getPartialTicks(){
        Minecraft mc = Minecraft.getInstance();
        return mc.getFrameTime();
    }
}

package net.lausiv1024.realelevator;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(RealElevator.MODID)
public class RealElevator {
    public static final String MODID = "realelevator";
    public static final Logger LOGGER = LogUtils.getLogger();

    public RealElevator(IEventBus modEventBus, ModContainer modContainer) {
    }
}

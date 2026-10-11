package com.mistbeyond.rail;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

@Mod(Ids.MOD_ID)
public class RailMod {
    public RailMod(IEventBus modBus, ModContainer modContainer) {
        Init.registerCommon(modBus, modContainer);
    }
}

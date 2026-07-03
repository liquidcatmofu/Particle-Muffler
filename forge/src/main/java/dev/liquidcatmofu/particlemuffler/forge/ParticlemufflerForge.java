package dev.liquidcatmofu.particlemuffler.forge;

import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Particlemuffler.MOD_ID)
public final class ParticlemufflerForge {
    public ParticlemufflerForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(Particlemuffler.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        Particlemuffler.init();
    }
}

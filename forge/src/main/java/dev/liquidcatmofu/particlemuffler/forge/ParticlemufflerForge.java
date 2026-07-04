package dev.liquidcatmofu.particlemuffler.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import dev.liquidcatmofu.particlemuffler.client.ParticlemufflerClient;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Particlemuffler.MOD_ID)
public final class ParticlemufflerForge {
    public ParticlemufflerForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        EventBuses.registerModEventBus(Particlemuffler.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());

        // Run our common setup.
        Particlemuffler.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> ParticlemufflerClient::init);
    }
}

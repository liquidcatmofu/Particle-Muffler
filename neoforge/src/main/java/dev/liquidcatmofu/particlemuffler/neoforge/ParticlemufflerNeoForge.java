package dev.liquidcatmofu.particlemuffler.neoforge;

import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import dev.liquidcatmofu.particlemuffler.Particlemuffler;
import dev.liquidcatmofu.particlemuffler.client.ParticlemufflerClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Particlemuffler.MOD_ID)
public final class ParticlemufflerNeoForge {
    public ParticlemufflerNeoForge(IEventBus modEventBus) {
        // Run our common setup.
        Particlemuffler.init();
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> ParticlemufflerNeoForgeClient.init(modEventBus));
        EnvExecutor.runInEnv(Env.CLIENT, () -> ParticlemufflerClient::init);
    }
}

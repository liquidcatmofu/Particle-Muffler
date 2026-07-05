package dev.liquidcatmofu.particlemuffler.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.menu.MenuRegistry;
import dev.liquidcatmofu.particlemuffler.client.gui.FilteredParticleMufflerScreen;
import dev.liquidcatmofu.particlemuffler.config.ParticleMufflerConfig;
import dev.liquidcatmofu.particlemuffler.registry.ModMenus;
import net.minecraft.client.multiplayer.ClientLevel;

public final class ParticlemufflerClient {
    private static boolean initialized;
    private static int cleanupTicks;

    private ParticlemufflerClient() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> clearClientRegistry());
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> clearClientRegistry());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> clearClientRegistry());
        ClientTickEvent.CLIENT_POST.register(ParticleInspection::tick);
        ClientTickEvent.CLIENT_LEVEL_POST.register(ParticlemufflerClient::cleanupMissingBlockEntities);
        ParticleInspection.registerCommands();
    }

    private static void clearClientRegistry() {
        cleanupTicks = 0;
        ParticleMufflerClientRegistry.clear();
        ParticleInspection.clear();
    }

    private static void cleanupMissingBlockEntities(ClientLevel level) {
        cleanupTicks++;
        if (cleanupTicks < ParticleMufflerConfig.pruningIntervalTicks()) {
            return;
        }

        cleanupTicks = 0;
        ParticleMufflerClientRegistry.removeMissingBlockEntities(level);
    }

    public static void registerScreens() {
        MenuRegistry.registerScreenFactory(ModMenus.FILTERED_PARTICLE_MUFFLER.get(), FilteredParticleMufflerScreen::new);
    }
}

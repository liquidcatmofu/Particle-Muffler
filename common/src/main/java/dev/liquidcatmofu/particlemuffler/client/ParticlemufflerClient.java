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
        ClientLifecycleEvent.CLIENT_SETUP.register(client -> MenuRegistry.registerScreenFactory(ModMenus.FILTERED_PARTICLE_MUFFLER.get(), FilteredParticleMufflerScreen::new));
        ClientLifecycleEvent.CLIENT_LEVEL_LOAD.register(level -> ParticleMufflerClientRegistry.clear());
        ClientLifecycleEvent.CLIENT_STOPPING.register(client -> ParticleMufflerClientRegistry.clear());
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> ParticleMufflerClientRegistry.clear());
        ClientTickEvent.CLIENT_LEVEL_POST.register(ParticlemufflerClient::cleanupMissingBlockEntities);
    }

    private static void cleanupMissingBlockEntities(ClientLevel level) {
        cleanupTicks++;
        if (cleanupTicks < ParticleMufflerConfig.pruningIntervalTicks()) {
            return;
        }

        cleanupTicks = 0;
        ParticleMufflerClientRegistry.removeMissingBlockEntities(level);
    }
}

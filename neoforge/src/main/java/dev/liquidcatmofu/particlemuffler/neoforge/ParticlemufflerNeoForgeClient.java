package dev.liquidcatmofu.particlemuffler.neoforge;

import dev.liquidcatmofu.particlemuffler.client.gui.FilteredParticleMufflerScreen;
import dev.liquidcatmofu.particlemuffler.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

final class ParticlemufflerNeoForgeClient {
    private ParticlemufflerNeoForgeClient() {
    }

    static void init(IEventBus modEventBus) {
        modEventBus.addListener(RegisterMenuScreensEvent.class, event ->
                event.register(ModMenus.FILTERED_PARTICLE_MUFFLER.get(), FilteredParticleMufflerScreen::new));
    }
}

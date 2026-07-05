package dev.liquidcatmofu.particlemuffler.fabric.client;

import dev.liquidcatmofu.particlemuffler.client.ParticlemufflerClient;
import net.fabricmc.api.ClientModInitializer;

public final class ParticlemufflerFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ParticlemufflerClient.registerScreens();
        ParticlemufflerClient.init();
    }
}

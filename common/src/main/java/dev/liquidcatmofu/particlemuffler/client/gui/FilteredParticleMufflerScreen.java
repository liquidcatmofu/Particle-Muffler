package dev.liquidcatmofu.particlemuffler.client.gui;

import dev.architectury.networking.NetworkManager;
import dev.liquidcatmofu.particlemuffler.blockentity.FilterMode;
import dev.liquidcatmofu.particlemuffler.menu.FilteredParticleMufflerMenu;
import dev.liquidcatmofu.particlemuffler.network.ParticleMufflerNetworking;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public final class FilteredParticleMufflerScreen extends AbstractContainerScreen<FilteredParticleMufflerMenu> {
    private static final int VISIBLE_ROWS = 6;
    private static final int CANDIDATE_ROWS = 3;

    private final List<ResourceLocation> particleIds;
    private FilterMode filterMode;
    private EditBox particleIdInput;
    private Button modeButton;
    private final List<Button> removeButtons = new ArrayList<>();
    private String completionPrefix = "";
    private List<String> completionMatches = List.of();
    private int completionIndex = -1;

    public FilteredParticleMufflerScreen(FilteredParticleMufflerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 248;
        imageHeight = 220;
        inventoryLabelY = 10_000;
        filterMode = menu.getFilterMode();
        particleIds = new ArrayList<>(menu.getParticleIds());
    }

    @Override
    protected void init() {
        super.init();
        int left = leftPos;
        int top = topPos;

        modeButton = addRenderableWidget(Button.builder(modeLabel(), button -> toggleMode())
                .bounds(left + 12, top + 24, 104, 20)
                .build());

        particleIdInput = new EditBox(font, left + 12, top + 52, 160, 20, Component.translatable("screen.particlemuffler.filtered_particle_muffler.particle_id"));
        particleIdInput.setMaxLength(128);
        particleIdInput.setHint(Component.literal("minecraft:flame"));
        addRenderableWidget(particleIdInput);

        addRenderableWidget(Button.builder(Component.translatable("screen.particlemuffler.filtered_particle_muffler.add"), button -> addParticleId())
                .bounds(left + 180, top + 52, 56, 20)
                .build());

        removeButtons.clear();
        for (int index = 0; index < VISIBLE_ROWS; index++) {
            final int row = index;
            Button removeButton = addRenderableWidget(Button.builder(Component.literal(""), button -> removeParticleId(row))
                    .bounds(left + 12, top + 112 + index * 18, 224, 16)
                    .build());
            removeButtons.add(removeButton);
        }

        updateRemoveButtons();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (particleIdInput != null && particleIdInput.isFocused()) {
            if (keyCode == GLFW.GLFW_KEY_TAB) {
                completeParticleId();
                return true;
            }

            if (particleIdInput.keyPressed(keyCode, scanCode, modifiers)) {
                resetCompletion();
                return true;
            }

            if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xE0101010);
        guiGraphics.fill(left + 8, top + 20, left + imageWidth - 8, top + imageHeight - 8, 0x80202020);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, 12, 8, 0xFFFFFF, false);
        guiGraphics.drawString(font, Component.translatable("screen.particlemuffler.filtered_particle_muffler.filter_mode"), 122, 30, 0xD0D0D0, false);
        renderCompletionCandidates(guiGraphics);
        guiGraphics.drawString(font, Component.translatable("screen.particlemuffler.filtered_particle_muffler.entries"), 12, 104, 0xD0D0D0, false);
    }

    private void toggleMode() {
        filterMode = filterMode == FilterMode.BLACKLIST ? FilterMode.WHITELIST : FilterMode.BLACKLIST;
        menu.setLocalFilterMode(filterMode);
        modeButton.setMessage(modeLabel());
        sendUpdate();
    }

    private void addParticleId() {
        ResourceLocation particleId = ResourceLocation.tryParse(particleIdInput.getValue().trim());
        if (particleId == null || particleIds.contains(particleId)) {
            return;
        }

        particleIds.add(particleId);
        particleIdInput.setValue("");
        resetCompletion();
        menu.setLocalParticleIds(particleIds);
        updateRemoveButtons();
        sendUpdate();
    }

    private void completeParticleId() {
        String current = particleIdInput.getValue().trim();
        if (current.isEmpty()) {
            current = "minecraft:";
        }

        if (completionMatches.isEmpty() || !completionMatches.contains(current)) {
            completionPrefix = current;
            completionMatches = findParticleIdMatches(completionPrefix);
            completionIndex = -1;
        }

        if (completionMatches.isEmpty()) {
            return;
        }

        int currentIndex = completionMatches.indexOf(current);
        completionIndex = currentIndex >= 0 ? currentIndex + 1 : completionIndex + 1;
        if (completionIndex >= completionMatches.size()) {
            completionIndex = 0;
        }

        String completed = completionMatches.get(completionIndex);
        particleIdInput.setValue(completed);
        particleIdInput.setCursorPosition(completed.length());
    }

    private void renderCompletionCandidates(GuiGraphics guiGraphics) {
        if (particleIdInput == null || !particleIdInput.isFocused()) {
            return;
        }

        List<String> candidates = completionMatches.isEmpty()
                ? findParticleIdMatches(particleIdInput.getValue().trim())
                : completionMatches;

        int start = completionIndex < 0 ? 0 : Math.min(completionIndex, Math.max(0, candidates.size() - CANDIDATE_ROWS));
        int rows = Math.min(CANDIDATE_ROWS, candidates.size() - start);
        String prefix = completionPrefix.isEmpty() ? particleIdInput.getValue().trim() : completionPrefix;
        if (prefix.isEmpty()) {
            prefix = "minecraft:";
        }

        for (int index = 0; index < rows; index++) {
            int candidateIndex = start + index;
            String candidate = candidates.get(candidateIndex);
            int y = 74 + index * 10;
            int prefixColor = candidateIndex == completionIndex ? 0xFFFFFF : 0xB0B0B0;
            int suffixColor = candidateIndex == completionIndex ? 0x7CC7FF : 0x6FA8C8;
            if (candidate.startsWith(prefix)) {
                guiGraphics.drawString(font, prefix, 12, y, prefixColor, false);
                guiGraphics.drawString(font, candidate.substring(prefix.length()), 12 + font.width(prefix), y, suffixColor, false);
            } else {
                guiGraphics.drawString(font, candidate, 12, y, prefixColor, false);
            }
        }
    }

    private static List<String> findParticleIdMatches(String prefix) {
        String effectivePrefix = prefix.isEmpty() ? "minecraft:" : prefix;
        return BuiltInRegistries.PARTICLE_TYPE.keySet().stream()
                .map(ResourceLocation::toString)
                .filter(id -> id.startsWith(effectivePrefix))
                .sorted(Comparator.naturalOrder())
                .toList();
    }

    private void resetCompletion() {
        completionPrefix = "";
        completionMatches = List.of();
        completionIndex = -1;
    }

    private void removeParticleId(int row) {
        if (row >= particleIds.size()) {
            return;
        }

        particleIds.remove(row);
        menu.setLocalParticleIds(particleIds);
        updateRemoveButtons();
        sendUpdate();
    }

    private void updateRemoveButtons() {
        for (int index = 0; index < removeButtons.size(); index++) {
            Button button = removeButtons.get(index);
            if (index < particleIds.size()) {
                button.active = true;
                button.visible = true;
                button.setMessage(Component.literal("x  " + particleIds.get(index)));
            } else {
                button.active = false;
                button.visible = false;
                button.setMessage(Component.empty());
            }
        }
    }

    private Component modeLabel() {
        return Component.translatable("screen.particlemuffler.filtered_particle_muffler.mode." + filterMode.name().toLowerCase());
    }

    private void sendUpdate() {
        if (minecraft == null || minecraft.level == null) {
            return;
        }

        FriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), minecraft.level.registryAccess());
        ParticleMufflerNetworking.writeUpdateFilteredMuffler(buffer, menu.getBlockPos(), filterMode, particleIds);
        NetworkManager.sendToServer(ParticleMufflerNetworking.UPDATE_FILTERED_MUFFLER, (RegistryFriendlyByteBuf) buffer);
    }
}

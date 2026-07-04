package dev.liquidcatmofu.particlemuffler.client;

import dev.liquidcatmofu.particlemuffler.blockentity.FilterMode;
import dev.liquidcatmofu.particlemuffler.blockentity.ParticleMufflerBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class ParticleMufflerClientRegistry {
    private static boolean hasAnyActiveMuffler;
    private static boolean hasAnyFilteredMuffler;
    private static final Long2IntOpenHashMap SUPPRESSED_SECTION_REF_COUNTS = new Long2IntOpenHashMap();
    private static final Long2ObjectOpenHashMap<Set<BlockPos>> FILTERED_SECTION_POSITIONS = new Long2ObjectOpenHashMap<>();
    private static final Map<BlockPos, ClientMufflerEntry> ENTRIES_BY_POS = new HashMap<>();

    private ParticleMufflerClientRegistry() {
    }

    public static boolean hasAnyActiveMuffler() {
        return hasAnyActiveMuffler;
    }

    public static void addOrUpdate(BlockPos pos, int sectionRadius, boolean enabled) {
        if (!enabled) {
            remove(pos);
            return;
        }

        int clampedRadius = Mth.clamp(sectionRadius, 0, 2);
        ClientMufflerEntry entry = new ClientMufflerEntry(pos.immutable(), clampedRadius, true, null, Set.of());
        addOrUpdate(entry);
    }

    public static void addOrUpdateFiltered(BlockPos pos, int sectionRadius, boolean enabled, FilterMode filterMode, Set<ResourceLocation> particleIds) {
        if (!enabled) {
            remove(pos);
            return;
        }

        int clampedRadius = Mth.clamp(sectionRadius, 0, 2);
        ClientMufflerEntry entry = new ClientMufflerEntry(pos.immutable(), clampedRadius, true, filterMode, Set.copyOf(particleIds));
        addOrUpdate(entry);
    }

    private static void addOrUpdate(ClientMufflerEntry entry) {
        if (entry.equals(ENTRIES_BY_POS.get(entry.pos()))) {
            return;
        }

        remove(entry.pos());
        addSections(entry);
        ENTRIES_BY_POS.put(entry.pos(), entry);
        updateStateFlags();
    }

    public static void remove(BlockPos pos) {
        ClientMufflerEntry entry = ENTRIES_BY_POS.remove(pos);
        if (entry == null) {
            return;
        }

        removeSections(entry);
        updateStateFlags();
    }

    public static void clear() {
        ENTRIES_BY_POS.clear();
        SUPPRESSED_SECTION_REF_COUNTS.clear();
        FILTERED_SECTION_POSITIONS.clear();
        hasAnyActiveMuffler = false;
        hasAnyFilteredMuffler = false;
    }

    public static void removeMissingBlockEntities(Level level) {
        if (!hasAnyActiveMuffler) {
            return;
        }

        Iterator<Map.Entry<BlockPos, ClientMufflerEntry>> iterator = ENTRIES_BY_POS.entrySet().iterator();
        boolean removedAny = false;
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, ClientMufflerEntry> entry = iterator.next();
            if (!(level.getBlockEntity(entry.getKey()) instanceof ParticleMufflerBlockEntity)) {
                removeSections(entry.getValue());
                iterator.remove();
                removedAny = true;
            }
        }

        if (removedAny) {
            updateStateFlags();
        }
    }

    public static boolean isUnfilteredSuppressedFast(double x, double y, double z) {
        if (!hasAnyActiveMuffler) {
            return false;
        }

        int sectionX = Mth.floor(x) >> 4;
        int sectionY = Mth.floor(y) >> 4;
        int sectionZ = Mth.floor(z) >> 4;
        long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
        return SUPPRESSED_SECTION_REF_COUNTS.containsKey(sectionKey);
    }

    public static boolean hasAnyFilteredMuffler() {
        return hasAnyFilteredMuffler;
    }

    public static boolean isFilteredSuppressedFast(ResourceLocation particleId, double x, double y, double z) {
        if (!hasAnyFilteredMuffler) {
            return false;
        }

        int sectionX = Mth.floor(x) >> 4;
        int sectionY = Mth.floor(y) >> 4;
        int sectionZ = Mth.floor(z) >> 4;
        long sectionKey = SectionPos.asLong(sectionX, sectionY, sectionZ);
        Set<BlockPos> filteredPositions = FILTERED_SECTION_POSITIONS.get(sectionKey);
        if (filteredPositions == null || filteredPositions.isEmpty()) {
            return false;
        }

        for (BlockPos pos : filteredPositions) {
            ClientMufflerEntry entry = ENTRIES_BY_POS.get(pos);
            if (entry != null && isSuppressedByFilter(entry, particleId)) {
                return true;
            }
        }

        return false;
    }

    private static void addSections(ClientMufflerEntry entry) {
        if (entry.isFiltered()) {
            forEachSection(entry, key -> addFilteredSection(key, entry.pos()));
        } else {
            forEachSection(entry, ParticleMufflerClientRegistry::addSuppressedSection);
        }
    }

    private static void removeSections(ClientMufflerEntry entry) {
        if (entry.isFiltered()) {
            forEachSection(entry, key -> removeFilteredSection(key, entry.pos()));
        } else {
            forEachSection(entry, ParticleMufflerClientRegistry::removeSuppressedSection);
        }
    }

    private static void forEachSection(ClientMufflerEntry entry, SectionConsumer consumer) {
        int centerX = SectionPos.blockToSectionCoord(entry.pos().getX());
        int centerY = SectionPos.blockToSectionCoord(entry.pos().getY());
        int centerZ = SectionPos.blockToSectionCoord(entry.pos().getZ());
        int radius = entry.sectionRadius();

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = centerY - radius; y <= centerY + radius; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    consumer.accept(SectionPos.asLong(x, y, z));
                }
            }
        }
    }

    private static void addSuppressedSection(long key) {
        SUPPRESSED_SECTION_REF_COUNTS.addTo(key, 1);
    }

    private static void addFilteredSection(long key, BlockPos pos) {
        FILTERED_SECTION_POSITIONS.computeIfAbsent(key, ignored -> new HashSet<>()).add(pos);
    }

    private static void removeSuppressedSection(long key) {
        int oldCount = SUPPRESSED_SECTION_REF_COUNTS.get(key);
        if (oldCount <= 1) {
            SUPPRESSED_SECTION_REF_COUNTS.remove(key);
        } else {
            SUPPRESSED_SECTION_REF_COUNTS.put(key, oldCount - 1);
        }
    }

    private static void removeFilteredSection(long key, BlockPos pos) {
        Set<BlockPos> positions = FILTERED_SECTION_POSITIONS.get(key);
        if (positions == null) {
            return;
        }

        positions.remove(pos);
        if (positions.isEmpty()) {
            FILTERED_SECTION_POSITIONS.remove(key);
        }
    }

    private static boolean isSuppressedByFilter(ClientMufflerEntry entry, ResourceLocation particleId) {
        boolean containsParticle = entry.particleIds().contains(particleId);
        return switch (entry.filterMode()) {
            case BLACKLIST -> containsParticle;
            case WHITELIST -> !containsParticle;
        };
    }

    private static void updateStateFlags() {
        hasAnyFilteredMuffler = !FILTERED_SECTION_POSITIONS.isEmpty();
        hasAnyActiveMuffler = !SUPPRESSED_SECTION_REF_COUNTS.isEmpty() || hasAnyFilteredMuffler;
    }

    @FunctionalInterface
    private interface SectionConsumer {
        void accept(long key);
    }
}

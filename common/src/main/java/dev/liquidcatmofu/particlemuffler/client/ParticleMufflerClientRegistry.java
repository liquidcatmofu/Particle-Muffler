package dev.liquidcatmofu.particlemuffler.client;

import dev.liquidcatmofu.particlemuffler.blockentity.ParticleMufflerBlockEntity;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class ParticleMufflerClientRegistry {
    private static boolean hasAnyActiveMuffler;
    private static final Long2IntOpenHashMap SUPPRESSED_SECTION_REF_COUNTS = new Long2IntOpenHashMap();
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
        ClientMufflerEntry entry = new ClientMufflerEntry(pos.immutable(), clampedRadius, true);
        if (entry.equals(ENTRIES_BY_POS.get(entry.pos()))) {
            return;
        }

        remove(pos);
        addSections(entry);
        ENTRIES_BY_POS.put(entry.pos(), entry);
        updateHasAnyActiveMuffler();
    }

    public static void remove(BlockPos pos) {
        ClientMufflerEntry entry = ENTRIES_BY_POS.remove(pos);
        if (entry == null) {
            return;
        }

        removeSections(entry);
        updateHasAnyActiveMuffler();
    }

    public static void clear() {
        ENTRIES_BY_POS.clear();
        SUPPRESSED_SECTION_REF_COUNTS.clear();
        hasAnyActiveMuffler = false;
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
            updateHasAnyActiveMuffler();
        }
    }

    public static boolean isSuppressedFast(double x, double y, double z) {
        if (!hasAnyActiveMuffler) {
            return false;
        }

        int sectionX = Mth.floor(x) >> 4;
        int sectionY = Mth.floor(y) >> 4;
        int sectionZ = Mth.floor(z) >> 4;
        return SUPPRESSED_SECTION_REF_COUNTS.containsKey(SectionPos.asLong(sectionX, sectionY, sectionZ));
    }

    private static void addSections(ClientMufflerEntry entry) {
        forEachSection(entry, ParticleMufflerClientRegistry::addSuppressedSection);
    }

    private static void removeSections(ClientMufflerEntry entry) {
        forEachSection(entry, ParticleMufflerClientRegistry::removeSuppressedSection);
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

    private static void removeSuppressedSection(long key) {
        int oldCount = SUPPRESSED_SECTION_REF_COUNTS.get(key);
        if (oldCount <= 1) {
            SUPPRESSED_SECTION_REF_COUNTS.remove(key);
        } else {
            SUPPRESSED_SECTION_REF_COUNTS.put(key, oldCount - 1);
        }
    }

    private static void updateHasAnyActiveMuffler() {
        hasAnyActiveMuffler = !SUPPRESSED_SECTION_REF_COUNTS.isEmpty();
    }

    @FunctionalInterface
    private interface SectionConsumer {
        void accept(long key);
    }
}

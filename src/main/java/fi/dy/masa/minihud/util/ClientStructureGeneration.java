package fi.dy.masa.minihud.util;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.structure.StructurePieceWithDimensions;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.SwampHutGenerator;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.feature.*;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;

public class ClientStructureGeneration {

    // since methods to get chunkrange/regionsize/seperation/spacing and seed are protected, store in enum
    // consider combining with enum in minihud/renderer/util/StructureTypes
    // this would call getStructurePos() from DataStorage with the params already - avoiding having to look up a second time

    // regionSize = .getSpacing() in XXXFeature or .getXXXDistance() in ChunkGeneratorConfig
    // chunkRange = .getSpacing() - .getXXXSeparation in XXXFeature or .getXXXDistance() - .getXXXSeparation() in ChunkGeneratorConfig
    // the first option only works if XXXFeature extends AbstractTempleFeature, else need to use ChunkGeneratorConfig

    private static final ChunkGeneratorConfig CHUNK_GEN_CONFIG = new ChunkGeneratorConfig();

    private static final HashMap<String, StructureConfig> ID_TO_CONFIG = new HashMap<>();

    @Nullable
    public static StructureConfig byStructureId(String id) {
        return ID_TO_CONFIG.get(id);
    }

    public enum StructureConfig {

        DESERT_PYRAMID("Desert_Pyramid", 14357617, CHUNK_GEN_CONFIG.getTempleDistance(), CHUNK_GEN_CONFIG.getTempleSeparation()),
        IGLOO("Igloo", 14357618, CHUNK_GEN_CONFIG.getTempleDistance(), CHUNK_GEN_CONFIG.getTempleSeparation()),
        JUNGLE_TEMPLE("Jungle_Pyramid", 14357619, CHUNK_GEN_CONFIG.getTempleDistance(), CHUNK_GEN_CONFIG.getTempleSeparation()),
        WITCH_HUT("Swamp_Hut", 14357620, CHUNK_GEN_CONFIG.getTempleDistance(), CHUNK_GEN_CONFIG.getTempleSeparation()),

        PILLAGER_OUTPOST("Pillager_Outpost", 165745296, CHUNK_GEN_CONFIG.getTempleDistance(), CHUNK_GEN_CONFIG.getTempleSeparation()),
        VILLAGE("Village", 10387312, CHUNK_GEN_CONFIG.getVillageDistance(), CHUNK_GEN_CONFIG.getVillageSeparation()),
        //spacing instead of distance?  // DIFF FROM CUBIOMES
        OCEAN_RUIN("Ocean_Ruin", 14357621, CHUNK_GEN_CONFIG.getOceanRuinSpacing(), CHUNK_GEN_CONFIG.getOceanRuinSeparation()),
        SHIPWRECK("Shipwreck", 165745295, CHUNK_GEN_CONFIG.getShipwreckSpacing(), CHUNK_GEN_CONFIG.getShipwreckSeparation()),
        // large structures
        OCEAN_MONUMENT("Monument", 10387313, CHUNK_GEN_CONFIG.getOceanMonumentSpacing(), CHUNK_GEN_CONFIG.getOceanMonumentSeparation()),
        MANSION("Mansion", 10387319, CHUNK_GEN_CONFIG.getMansionDistance(), CHUNK_GEN_CONFIG.getMansionSeparation()),

//        BURIED_TREASURE     ("Buried_Treasure",10387320,  1,  0),

        // unknown or need testing
        END_CITY("EndCity", 10387313, CHUNK_GEN_CONFIG.getEndCityDistance(), CHUNK_GEN_CONFIG.getEndCitySeparation());
//        STRONGHOLD          (DimensionType.OVERWORLD,   "Stronghold",       StructureToggle.OVERLAY_STRUCTURE_STRONGHOLD),
//        MINESHAFT           (DimensionType.OVERWORLD,   "Mineshaft",        StructureToggle.OVERLAY_STRUCTURE_MINESHAFT),
//        END_CITY            (DimensionType.THE_END,     "EndCity",          StructureToggle.OVERLAY_STRUCTURE_END_CITY);

        private final String structureName;
        private final long structureSeed;
        private final int regionSize;
        private final int chunkRange;

        private StructureConfig(String structureName, long structureSeed, int regionSize, int separation) {

            this.structureName = structureName.toLowerCase(Locale.ROOT);
            this.structureSeed = structureSeed;
            this.regionSize = regionSize;
            this.chunkRange = regionSize - separation;

            StructureFeature<?> feature = Feature.STRUCTURES.get(structureName.toLowerCase(Locale.ROOT));

            if (feature != null) {
                Identifier key = Registry.STRUCTURE_FEATURE.getId(feature);

                if (key != null) {
                    ID_TO_CONFIG.put(key.getPath(), this);
                }
            }
        }

        public long getStructureSeed() {
            return this.structureSeed;
        }

        public int getRegionSize() {
            return this.regionSize;
        }

        public int getChunkRange() {
            return this.chunkRange;
        }

        public String getStructureName() {
            return structureName;
        }
    }

    private ClientStructureGeneration() {

    }

    public static StructureData getStructurePos(ClientWorld world, StructureTypes.StructureType structureType, int rx, int rz, BlockPos playerPos, final int maxChunkRange) {

        long seed = world.getSeed();

        String structureId = structureType.getStructureName().toLowerCase(Locale.ROOT);

        StructureConfig structureConfig = byStructureId(structureId);
        StructureFeature<?> structureFeature = Feature.STRUCTURES.get(structureId);

        if (structureConfig != null) {

            seed = rx * 341873128712L + rz * 132897987541L + seed + structureConfig.getStructureSeed();
            seed = (seed ^ 25214903917L);// & ((1LL << 48) - 1);en
            seed = (seed * 25214903917L + 11) & 281474976710655L;

            int posX, posZ;

            if ((structureConfig.getChunkRange() & (structureConfig.getChunkRange() - 1)) != 0) {
                // not power of 2
                posX = (int) (seed >> 17) % structureConfig.getChunkRange();

                seed = (seed * 25214903917L + 11) & 281474976710655L;
                posZ = (int) (seed >> 17) % structureConfig.getChunkRange();
            } else {
                // power of 2
                posX = (int) (structureConfig.getChunkRange() * (seed >> 17)) >> 31;

                seed = (seed * 25214903917L + 11) & 281474976710655L;
                posZ = (int) (structureConfig.getChunkRange() * (seed >> 17)) >> 31;
            }

            // convert from chunk to block pos
            posX = ((rx * structureConfig.getRegionSize() + posX) << 4);
            posZ = ((rz * structureConfig.getRegionSize() + posZ) << 4);

            // create feature - orientation set by first call to ChunkRandom()
            ChunkRandom rotationChunkRandom = new ChunkRandom();
            rotationChunkRandom.setStructureSeed(seed, posX >> 4, posZ >> 4);

            StructurePieceWithDimensions swampHutGenerator = new SwampHutGenerator(rotationChunkRandom, posX, posZ);
            BlockBox witchBB = swampHutGenerator.getBoundingBox();

            // correct for witch hut height bug
            if (structureConfig == StructureConfig.WITCH_HUT) {
                witchBB.minY -= 1;
                witchBB.maxY -= 1;
            }

            if (MiscUtils.isStructureWithinRange(witchBB, playerPos, maxChunkRange << 4)) {
                // use actual biome of world as minimal check of validity of generated structure pos - also much simpler
                // biome check is performed at (9, 9) in chunk. but structure origin is at (0, 0)
                Biome biome = world.getBiome(new BlockPos(posX + 9, 100, posZ + 9));

                if (biome != null && biome.hasStructureFeature(structureFeature)) {
                    // debug print
                    String message = String.format("pos found: %d %d %s, %s", posX, posZ, biome.toString(), structureFeature.toString());
                    InfoUtils.showInGameMessage(Message.MessageType.SUCCESS, 1000, message);

                    // create and return feature
                    StructureStart structureStart = new SwampHutFeature.Start(structureFeature, posX >> 4, posZ >> 4, witchBB, 0, 14357620L);
                    return StructureData.fromStructureStart(structureType, structureStart);
                }
            }
        }
        // no structure found
        return null;
    }
}
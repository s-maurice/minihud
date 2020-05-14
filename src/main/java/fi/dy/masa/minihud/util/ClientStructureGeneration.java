package fi.dy.masa.minihud.util;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.minihud.config.StructureToggle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.structure.StructurePieceWithDimensions;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.SwampHutGenerator;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorConfig;
import net.minecraft.world.gen.feature.AbstractTempleFeature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.SwampHutFeature;

import java.util.HashMap;
import java.util.Locale;

public class ClientStructureGeneration {

    // since methods to get chunkrange/regionsize/seperation/spacing and seed are protected, store in enum
    // consider combining with enum in minihud/renderer/util/StructureTypes
    // this would call getStructurePos() from DataStorage with the params already - avoiding having to look up a second time

    // regionSize = .getSpacing() in XXXFeature or .getXXXDistance() in ChunkGeneratorConfig
    // chunkRange = .getSpacing() - .getXXXSeparation in XXXFeature or .getXXXDistance() - .getXXXSeparation() in ChunkGeneratorConfig
    // the first option only works if XXXFeature extends AbstractTempleFeature, else need to use ChunkGeneratorConfig

    private static final HashMap<StructureTypes.StructureType, StructureConfig> TYPE_TO_CONFIG =  new HashMap<>();

    private static final ChunkGeneratorConfig CHUNK_GENERATOR_CONFIG = new ChunkGeneratorConfig();

    private enum StructureConfig {

        DESERT_PYRAMID      ("Desert_Pyramid",14357617, CHUNK_GENERATOR_CONFIG.getTempleDistance(), 24),
        IGLOO               ("Igloo",14357618, 32, 24),
        JUNGLE_TEMPLE       ("Jungle_Pyramid", 14357619, 32, 24),
        WITCH_HUT           ("Swamp_Hut" ,14357620, 32, 24),

        PILLAGER_OUTPOST    ("Pillager_Outpost",165745296, 32, 24),
        VILLAGE             ("Village",10387312, 32, 24),
        OCEAN_RUIN          ("Ocean_Ruin",14357621, 20, 12),
        SHIPWRECK           ("Shipwreck",165745295, 24, 20),
        OCEAN_MONUMENT      ("Monument",10387313, 32, 27), // LARGE
        MANSION             ( "Mansion",10387319, 80, 60),  // LARGE

        BURIED_TREASURE     ("Buried_Treasure",10387320,  1,  0);  // TREASURE CONFIG

//        NETHER_FORTRESS     (DimensionType.THE_NETHER,  "Fortress",         StructureToggle.OVERLAY_STRUCTURE_NETHER_FORTRESS)
//        STRONGHOLD          (DimensionType.OVERWORLD,   "Stronghold",       StructureToggle.OVERLAY_STRUCTURE_STRONGHOLD),
//        MINESHAFT           (DimensionType.OVERWORLD,   "Mineshaft",        StructureToggle.OVERLAY_STRUCTURE_MINESHAFT),
//        END_CITY            (DimensionType.THE_END,     "EndCity",          StructureToggle.OVERLAY_STRUCTURE_END_CITY);

        private final long structureSeed;
        private final int regionSize;
        private final int chunkRange;

        private StructureConfig(String structureName, long structureSeed, int regionSize, int separation) {

            this.structureSeed = structureSeed;
            this.regionSize = regionSize;
            this.chunkRange = regionSize - separation;

            // change to use identifier or string
            StructureTypes.StructureType structureType = StructureTypes.byStructureId(structureName.toLowerCase(Locale.ROOT));
            TYPE_TO_CONFIG.put(structureType, this);
        }

        public long getStructureSeed() {
            return structureSeed;
        }

        public int getRegionSize() {
            return regionSize;
        }

        public int getChunkRange() {
            return chunkRange;
        }
    }

    public StructureData getStructurePos(final ClientWorld world, StructureTypes.StructureType structureType, int rx, int rz, BlockPos playerPos, final int maxChunkRange) {

        long seed = world.getSeed();

        seed = rx*341873128712L + rz*132897987541L + seed + 14357620;
//                      seed = rz*341873128712L + rz*132897987541L + seed + config.seed;

        seed = (seed ^ 25214903917L);// & ((1LL << 48) - 1);en

        seed = (seed * 25214903917L + 11) & 281474976710655L;

        int posX, posZ;

//                      if ((config.chunkrange & (config.chunkrange-1)) == 0)
        if ((24 & (24 - 1)) != 0) {
            // not power of 2
//                          posX = (int) (seed >> 17) % config.chunkRange;
            posX = (int) (seed >> 17) % 24;

            seed = (seed * 25214903917L + 11) & 281474976710655L;
//                          posZ = (int) (seed >> 17) % config.chunkRange;
            posZ = (int) (seed >> 17) % 24;
        } else {
            // power of 2

//                          posX = (config.chunkRange * (seed >> 17)) >> 31;
            posX = (int) (24 * (seed >> 17)) >> 31;

            seed = (seed * 25214903917L + 11) & 281474976710655L;
//                          posZ = (config.chunkRange * (seed >> 17)) >> 31;
            posZ = (int) (24 * (seed >> 17)) >> 31;
        }

        // offset pos to (9, 9) in chunk
        // biome check is performed at (9, 9) in chunk. but structure origin is at (0, 0)
//                      posX = ((rx*config.regionSize + posX) << 4) + 9;
//                      posZ = ((rz*config.regionSize + posZ) << 4) + 9;
        posX = ((rx*32 + posX) << 4) + 9;
        posZ = ((rz*32 + posZ) << 4) + 9;

//                      create feature
        ChunkRandom rotationChunkRandom = new ChunkRandom();
        rotationChunkRandom.setStructureSeed(seed, posX >> 4, posZ >> 4);

        StructurePieceWithDimensions swampHutGenerator = new SwampHutGenerator(rotationChunkRandom, posX-9, posZ-9);
        BlockBox witchBB = swampHutGenerator.getBoundingBox();

        // correct for witch hut height bug
        witchBB.minY -= 1;
        witchBB.maxY -= 1;

//        StructureFeature.DESERT_PYRAMID

        if (MiscUtils.isStructureWithinRange(witchBB, playerPos, maxChunkRange << 4)){
            // use actual biome of world as minimal check of validity of generated structure pos - also much simpler
            Biome biome = world.getBiome(new BlockPos(posX, 100, posZ));

            if (biome != null && biome.hasStructureFeature(StructureFeature.SWAMP_HUT)) {
                // debug print
                String message = String.format("pos found: %d %d %s", posX-9, posZ-9, biome.toString());
                InfoUtils.showInGameMessage(Message.MessageType.SUCCESS, 1000, message);

                // create and return feature
                StructureStart structureStart = new SwampHutFeature.Start(StructureFeature.SWAMP_HUT, posX>>4, posZ>>4, witchBB, 0, 14357620L);
                return StructureData.fromStructureStart(StructureTypes.StructureType.WITCH_HUT, structureStart);
            }
        }

        // debug so it compiles
        StructureStart structureStart = new SwampHutFeature.Start(StructureFeature.SWAMP_HUT, posX>>4, posZ>>4, witchBB, 0, 14357620L);
        return StructureData.fromStructureStart(StructureTypes.StructureType.WITCH_HUT, structureStart);
    }
}
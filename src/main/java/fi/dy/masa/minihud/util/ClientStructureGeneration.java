package fi.dy.masa.minihud.util;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.structure.StructurePieceWithDimensions;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.SwampHutGenerator;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.feature.AbstractTempleFeature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.SwampHutFeature;

public class ClientStructureGeneration {

    // since methods to get chunkrange/regionsize/seperation/spacing and seed are protected, store in enum
    // consider combining with enum in minihud/renderer/util/StructureTypes
    // this would call getStructurePos() from DataStorage with the params already - avoiding having to look up a second time

    // regionSize = .getSpacing() in XXXFeature or .getXXXDistance() in ChunkGeneratorConfig
    // chunkRange = .getSpacing() - .getXXXSeparation in XXXFeature or .getXXXDistance() - .getXXXSeparation() in ChunkGeneratorConfig
    // the first option only works if XXXFeature extends AbstractTempleFeature, else need to use ChunkGeneratorConfig

    private enum StructureConfig {}

    public StructureData getStructurePos(final ClientWorld world, StructureTypes.StructureType structureType, int rx, int rz, BlockPos playerPos, final int maxChunkRange) {

        long seed = world.getSeed();

        seed = rx*341873128712L + rz*132897987541L + seed + 14357620;
//                      seed = rz*341873128712L + rz*132897987541L + seed + config.seed;

        seed = (seed ^ 25214903917L);// & ((1LL << 48) - 1);

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
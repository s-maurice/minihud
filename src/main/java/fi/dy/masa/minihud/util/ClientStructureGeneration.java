package fi.dy.masa.minihud.util;

import fi.dy.masa.malilib.gui.Message;
import fi.dy.masa.malilib.util.InfoUtils;
import net.minecraft.structure.StructurePieceWithDimensions;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.SwampHutGenerator;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.ChunkRandom;
import net.minecraft.world.gen.feature.StructureFeature;
import net.minecraft.world.gen.feature.SwampHutFeature;

public class ClientStructureGeneration {
    public int[] GetStructurePos(Long seed, int rx, int rz) {

        seed = rx*341873128712L + rz*132897987541L + seed + 14357620; // testing with witch hut temporarily
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
        rotationChunkRandom.setStructureSeed(this.getWorldSeed(dimensionType), posX >> 4, posZ >> 4);

        StructurePieceWithDimensions swampHutGenerator = new SwampHutGenerator(rotationChunkRandom, posX-9, posZ-9);
        BlockBox witchBB = swampHutGenerator.getBoundingBox();

        // correct for witch hut height bug
        witchBB.minY -= 1;
        witchBB.maxY -= 1;

        if (MiscUtils.isStructureWithinRange(witchBB, playerPos, maxChunkRange << 4)){
//                         using actual biome to check is structure pos is valid
//                         greatly reduces complexity, as well as acting as a minimal check for validity of generated structure

            Biome biome = mc.world.getBiome(new BlockPos(posX, 100, posZ));
            if (biome != null) {
                String message = String.format("pos found: %d %d %s", posX-9, posZ-9, biome.toString());
                InfoUtils.showInGameMessage(Message.MessageType.SUCCESS, 1000, message);
                System.out.println(message);

                // create and add structure
                StructureStart structureStart = new SwampHutFeature.Start(StructureFeature.SWAMP_HUT, posX>>4, posZ>>4, witchBB, 0, 14357620L);
                StructureData structureData = StructureData.fromStructureStart(StructureTypes.StructureType.WITCH_HUT, structureStart);
                this.structures.put(StructureTypes.StructureType.OCEAN_MONUMENT, structureData); // dif colour for debug

                System.out.println("created, put");
            }
        }
        else {
            String outrngmsg = String.format("out range %d %d", posX, posZ);
            System.out.println(outrngmsg);
        }
    }
}
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        RomReader rom = new RomReader(Path.of(args[0]));

        // final int pokedex = rom.read24(0x44e8b0);
        final int names = rom.read24(0x144);
        final int iconSprites = rom.read24(0x138);
        final int iconPalettes = rom.read24(0x13c);
        //Icon Sprites = 0x138
        //Icon Palettes = 0x13c - this is the table that determines what palette each sprite has
        //(Actual)Icon Palettes = 0x8ab40 - this is where the palettes are located for the above table

        // 0x3521AC — Map Bank Table (FireRed)
        //Each bank contains a table of 12-byte map entries:
        //00–03: map header pointer
        //04–07: event pointer
        //08–0B: map scripts pointer

        final int pokemonSpecies = rom.read24(0x1bc);
        final int frontSprites = rom.read24(0x128);
        final int backSprites = rom.read24(0x12c);
        final int encounters = rom.read24(0x82ebc);

        final int mapBank = rom.read24(0x55260); // = 71a29c
        System.out.printf("Master Map Pointer @ 55260 -> %06X\n", mapBank);
        // 43 contiguous bank pointers. 0x77777777 will be after the last bank pointer
        // https://datacrystal.tcrf.net/wiki/Pok%C3%A9mon_3rd_Generation
        for (int i = 0; i < 43; i++) {
            int bank = rom.read24(mapBank + i * 4); // 352074
            System.out.printf("Bank %02d @ %06X\n", i, bank);

            // how to know how many maps are in this bank?
            // 1. calculate (banks[i+1] - banks[i]) / 4
            for (int j = 0; j < 5; j++) {
                int map = rom.read24(bank); // 34f1f8
                System.out.printf("Map %d.%d @ %06X\n", i, j, map);

                // Below we read the TODO byte map header
                int mapData = rom.read24(map);
                int eventData = rom.read24(map + 4);
                int mapScripts = rom.read24(map + 8);
                int connections = rom.read24(map + 12);
                System.out.printf("   Map Data @ %06X\n", mapData);
                System.out.printf("     Events @ %06X\n", eventData);
                System.out.printf("    Scripts @ %06X\n", mapScripts);
                System.out.printf("Connections @ %06X\n", connections);

                int musicIndex = rom.read16(map + 16); // 0x012f -> pokemon center music
                int mapPointerIndex = rom.read16(map + 18); // 0x002f
                int labelIndex = rom.read8(map + 20); // c4 (do not show name on entering?)
                int visibility = rom.read8(map + 21); // 00 cave: regular
                int weather = rom.read8(map + 22); // 00 (in house weather)
                int mapType = rom.read8(map + 23); // 08 (inside)
                // [24, 25] are 2 mystery bytes
                // something for riding a bicycle on the map
                // species floor number, above ground countr up from 01 where f1 below ground count down from ff where ff is bf1
                int showLabelOnEntry = rom.read8(map + 26); // 00
                int inBattleFieldModelId = rom.read8(map + 27); // 08
                System.out.printf("      Music Index = 0x%04X\n", musicIndex);
                System.out.printf("Map Pointer Index = 0x%04X\n", mapPointerIndex);
                System.out.printf("      Label Index = 0x%02X\n", labelIndex);
                System.out.printf("       Visibility = 0x%02X\n", visibility);
                System.out.printf("          Weather = 0x%02X\n", weather);
                System.out.printf("         Map Type = 0x%02X\n", mapType);
                System.out.printf("       Show Label = 0x%02X\n", showLabelOnEntry);
                System.out.printf("   Field Model ID = 0x%02X\n", inBattleFieldModelId); // fight type?

                // map data
                if (mapData != 0) {
                    int mapWidth = rom.read24(mapData); // 16 (4 bytes)
                    int mapHeight = rom.read24(mapData + 4); // 9 (4 bytes)
                    int border = rom.read24(mapData + 8); // 0x2d8954, points to a single block?
                    int tileStructure = rom.read24(mapData + 12); // 0x2d895c no 01 01 first 2 bytes.
                    int globalTileset = rom.read24(mapData + 16); // 0x2d4c24, size 0x90 before the local tileset data starts.
                    int localTileset = rom.read24(mapData + 20); // 0x2d4cb4 // starts with 00 01 00 00
                    int borderWidth = rom.read8(mapData + 24); // 2
                    int borderHeight = rom.read8(mapData + 25); // 2

                    System.out.println("=== Map Data ===");
                    System.out.printf("Dimensions: %d x %d\n", mapWidth, mapHeight);
                    System.out.printf("        Border @ %06X\n", border); // this points to a block?
                    System.out.printf("Tile Structure @ %06X\n", tileStructure);
                    System.out.printf("Global Tileset @ %06X\n", globalTileset);
                    System.out.printf(" Local Tileset @ %06X\n", localTileset);
                    System.out.printf("Border Size: %d x %d\n", borderWidth, borderHeight);

                    if (globalTileset != 0) {
                        int compressed = rom.read8(globalTileset);
                        int isPrimary = rom.read8(globalTileset + 1);
                        int tilesetImage = rom.read24(globalTileset + 4); // 275304
                        int colorPalettes = rom.read24(globalTileset + 8); // 277704
                        int blocks = rom.read24(localTileset + 12); // 2ad824
                        int animationRoutine = rom.read24(localTileset + 16); // 0
                        int bahaviourAndBackgroundBytes = rom.read24(localTileset + 20); // 2b0024

                        // Note: has same blocks and behaviour pointers that the local tileset has.
                        System.out.println("=== Global Tileset Header ===");
                        System.out.printf("Compressed = %d\n", compressed);
                        System.out.printf("Is Primary = %d\n", isPrimary);
                        System.out.printf("       Tileset Image @ %06X\n", tilesetImage);
                        System.out.printf("      Color Palettes @ %06X\n", colorPalettes);
                        System.out.printf("              Blocks @ %06X\n", blocks); // 2C6234 which is block 0x280
                        System.out.printf("   Animation Routine @ %06X\n", animationRoutine);
                        System.out.printf("Bahaviour & BG Bytes @ %06X\n", bahaviourAndBackgroundBytes);

                        byte[] pixelBuffer = new byte[0x6000]; // some overhead for decompressing 0x5000 bytes
                        // each 32 bytes is a new tile pixel data
                        // so tile 281 is [0x281 * 32, +32) into that buffer

                        rom.readBytes(tilesetImage, pixelBuffer);
                        if (compressed != 0) {
                            pixelBuffer = RomReader.decompressLZ10(pixelBuffer);
                        }

                        int[][][] rgbPalettes = rom.readPalettes(colorPalettes, 13, false);
                        for (int pal = 0; pal < 13; pal++) {
                            BufferedImage tilesetImg = SpriteBuilder.CreateTilesetImage(pixelBuffer, rgbPalettes[pal], 16);
                            SpriteBuilder.WriteImage(tilesetImg, "out/global_tileset" + pal + ".png");
                        }

                    }

                    if (localTileset != 0) {
                        int compressed = rom.read8(localTileset);
                        int isPrimary = rom.read8(localTileset + 1);
                        int tilesetImage = rom.read24(localTileset + 4);
                        int colorPalettes = rom.read24(localTileset + 8);
                        int blocks = rom.read24(localTileset + 12);
                        int animationRoutine = rom.read24(localTileset + 16);
                        int bahaviourAndBackgroundBytes = rom.read24(localTileset + 20);

                        // The first 6 palettes belong to the first tileset of the map and palettes 7-12 are from the second one.
                        // so: each tileset has 6 palettes
                        // each tileset has 1024 tiles?

                        // how can i know the bank size? the delta between this and the next bank pointers?
                        System.out.println("=== Local Tileset Header ===");
                        System.out.printf("Compressed = %d\n", compressed);
                        System.out.printf("Is Primary = %d\n", isPrimary);
                        System.out.printf("       Tileset Image @ %06X\n", tilesetImage);
                        System.out.printf("      Color Palettes @ %06X\n", colorPalettes);
                        System.out.printf("              Blocks @ %06X\n", blocks); // 2C6234 which is block 0x280
                        System.out.printf("   Animation Routine @ %06X\n", animationRoutine);
                        System.out.printf("Bahaviour & BG Bytes @ %06X\n", bahaviourAndBackgroundBytes);

                        // there are multiple tilesets
                        // each can hold at max 384 blocks
                        // TODO: use pixelBuffer to create a list of tiles
                        byte[] raw = new byte[0x10000];
                        rom.readBytes(tilesetImage, raw);

                        if (compressed != 0) {
                            raw = RomReader.decompressLZ10(raw);
                        }

                        // raw into 32-byte chunks
                        List<byte[]> tiles = new ArrayList<>();
                        for (int tileIndex = 0; tileIndex < raw.length; tileIndex += 32) {
                            byte[] tile = Arrays.copyOfRange(raw, tileIndex, tileIndex + 32);

                            // 32 bytes of 0 == no more tiles
                            if (allZero(tile)) break;

                            tiles.add(tile);
                            System.out.printf("Tile %03X: %s", tileIndex, Arrays.toString(tile));
                        }


                        int[][][] rgbPalettes = rom.readPalettes(colorPalettes, 13, compressed != 0);
                        BufferedImage tilesetImg = SpriteBuilder.CreateTilesetImage(raw, rgbPalettes[7], 16);
                        SpriteBuilder.WriteImage(tilesetImg, "out/tileset.png");

                        // TODO: advance map shows golbal tileset and local tileset merged into 1.
                        // there is a third tileset at the bottom as well? what is this?
                        if (blocks != 0) {
                            BlockInfo[][] mapBlocks = new BlockInfo[mapHeight][mapWidth];
                            System.out.println("=== Tile Structure ===");
                            for (int h = 0; h < mapHeight; h++) {
                                for (int w = 0; w < mapWidth; w++) {
                                    int tile = h * mapWidth + w;
                                    int value = rom.read16(tileStructure + tile * 2);
                                    int attributes = (value >> 10) & 0b111111; // top 6 bits. 0x1=nowalk, 0xC=walk
                                    int blockIndex = value & 0b1111111111; // bottom 10 bits

                                    // Note: weird subtraction since blocks pointer starts pointing at block 0x280
                                    // maybe something to do with the local tileset vs global tileset?
                                    // minus 0x280 since the first 0x280 are in the global
                                    int blockOffset = blocks + (blockIndex-0x280) * BlockInfo.BYTES;
                                    byte[] blockBytes = new byte[BlockInfo.BYTES];
                                    rom.readBytes(blockOffset, blockBytes);
                                    List<TileInfo> tileInfos = new ArrayList<>();
                                    for (int t = 0; t < 8; t++) {
                                        tileInfos.add(new TileInfo(rom.read16(blockOffset + t * 2), null));
                                    }
                                    mapBlocks[h][w] = new BlockInfo(tileInfos);

                                    System.out.println(mapBlocks[h][w]);
                                }
                                System.out.println();
                            }

                            BufferedImage mapImage = SpriteBuilder.CreateMapImage(mapBlocks, rgbPalettes);
                            SpriteBuilder.WriteImage(mapImage, "out/celadon_dept_0_0.png");
                        }
                    }
                }
            }
        }

        for (int mm = 0; mm < 10; mm++) {
            int offset = encounters + mm * 0x50;
            int bank = rom.read8(offset);
            int map = rom.read8(offset + 1);

            int grass = rom.read24(offset + 4);
            int water = rom.read24(offset + 8); // 5 slots
            int trees = rom.read24(offset + 12); // what is this?
            int fishing = rom.read24(offset + 16); // 10 slots

            if (grass != 0) {
                int encounterRate = rom.read8(grass);
                System.out.printf("Bank %d Map %d Rate=%d\n", bank, map, encounterRate);
                // todo replace with seeks and reads
                for (int i = 0; i < 12; i++) {
                    int minLv = rom.read8(grass + 8 + i * 4);
                    int maxLv = rom.read8(grass + 9 + i * 4);
                    int pokemonID = rom.read16(grass + 10 + i * 4);
                    System.out.printf("Slot %02d: #%03d Lv %d to %d\n", i, pokemonID, minLv, maxLv);
                }
            }

            // rate uint32, ptr, ten slots
            if (water != 0) {
                System.out.printf("Water @ %06X\n", water);
            }

            if (trees != 0) {
                System.out.printf("Trees @ %06X\n", trees);
            }

            // rate uint32, ptr, ten slots
            if (fishing != 0) {
                System.out.printf("Fishing @ %06X\n", fishing);
            }
        }

        //Footprint = 0x105e14
        final int[] palettes = {
                rom.read24(0x130), // Normal
                rom.read24(0x134), // Shiny
        };

        byte[] pixelBuffer = new byte[0x900]; // TODO: what is the least bytes needed to decompress this?
        byte[] colorBuffer = new byte[0x28]; // TODO: ^^

        final int NUM_POKEMON = 250;

        // https://bulbapedia.bulbagarden.net/wiki/List_of_locations_by_index_number_in_Generation_III
        final int maps = rom.read24(0xc0ca8);
        for (int i = 0; i < 109; i++) {
            int offset = maps + i * 4;
            int mapNameLocation = rom.read24(offset);
            String mapName = rom.readString(mapNameLocation);
            //System.out.printf("%03d: %s\n", i, mapName);
        }

        final int types = rom.read24(0x309dc);
        for (int i = 0; i < 18; i++) {
            String typeString = rom.readString(types + i * 7);
        }

        // Loop over original 151 Pokemon
        for (int pid = 1; pid <= NUM_POKEMON; pid++) {
            final int frontSprite = rom.read24(frontSprites + 8 * pid);
            final int speciesOffset = pokemonSpecies + 28 * pid;
            final int nameOffset = names + 11 * pid;
            String name = rom.readString(nameOffset);

            final int type1 = rom.read8(speciesOffset + 6);
            final int type2 = rom.read8(speciesOffset + 7);
            // TODO: if type2 == type1 then set it to null, this is a single type pokemon
            final int gender = rom.read8(speciesOffset + 16); // 0=male, 254=female, 255=unknown, 1-253=mixed
            // this value is compared to the lowest byte of a Pokémon's personality value to determine its gender.

            // HP AT DF SP SA SD T1 T2 CR XP EY--- IT1-- IT2-- SX EC BF LU E1 E2 A1 A2 SZ CF PADDING
            // 2D 31 31 2D 41 41 0C 03 2D 40 00 01 00 00 00 00 1F 14 46 03 01 07 41 00 00 03 00 00

            rom.readBytes(frontSprite, pixelBuffer);
            byte[] pixels = RomReader.decompressLZ10(pixelBuffer);

            int[] colorCounts = new int[16];
            for (byte pixel: pixels) {
                int lo = pixel & 0xF;
                int hi = (pixel >>> 4) & 0x0F;
                colorCounts[lo]++;
                colorCounts[hi]++;
            }

            for (int p = 0; p < palettes.length; p++) {
                int pokemonId = (p == 0) ? pid : -pid;
                int pokemon = rom.read24(palettes[p] + 8 * pid);

                int[][] rgb = rom.readPalettes(pokemon, 1, true)[0];

                SpriteBuilder.CreateSprite(pokemonId, pixels, rgb);
            }
        }
        //SpriteBuilder.WriteSheet();
    }

    private static boolean allZero(byte[] data) {
        for (byte b : data) {
            if (b != 0) return false;
        }
        return true;
    }
}
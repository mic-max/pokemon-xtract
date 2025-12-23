import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;

public class SpriteBuilder {
    static final int SPRITE_SIZE = 64;
    static final int NUM_TILES = 64;
    static final int TILE_BYTES = 32; // 8x8 pixels, 4bpp
    static final int TILE_W = 8;
    static final int TILE_H = 8;
    static final int BLOCK_H = TILE_H * 2;
    static final int BLOCK_W = TILE_W * 2;

    static final int SPRITES_WIDE = 20;
    static BufferedImage sheet = new BufferedImage(SPRITE_SIZE * SPRITES_WIDE, SPRITE_SIZE * (int) (Math.ceil((double) 151 / 10)), BufferedImage.TYPE_INT_ARGB);

    static BufferedImage CreateTilesetImage(byte[] raw, int[][] rgb, int tilesPerRow) {
        final int numTiles = raw.length / TILE_BYTES;
        int rows = (int)Math.ceil(numTiles / (double)tilesPerRow);
        int outW = tilesPerRow * TILE_W;
        int outH = rows * TILE_H;

        BufferedImage img = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);

        for (int t = 0; t < numTiles; t++) {
            int tileX = (t % tilesPerRow) * TILE_W;
            int tileY = (t / tilesPerRow) * TILE_H;
            int offset = t * TILE_BYTES;

            for (int y = 0; y < TILE_H; y++) {
                for (int x = 0; x < TILE_W; x++) {

                    int byteIndex = offset + (y * 4) + (x / 2);
                    int b = raw[byteIndex] & 0xFF;

                    int index4bpp;
                    if ((x & 1) == 0) {
                        index4bpp = b & 0x0F;       // low nibble
                    } else {
                        index4bpp = (b >> 4) & 0x0F; // high nibble
                    }

                    int argb = 0xFF000000 | (rgb[index4bpp][0] << 16) | (rgb[index4bpp][1] << 8) | rgb[index4bpp][2];
                    img.setRGB(tileX + x, tileY + y, argb);
                }
            }
        }

        return img;
    }

    static void DrawTile(BufferedImage img, int[][][] rgb, TileInfo tile, int yOffset, int xOffset) {
        for (int y = 0; y < TILE_H; y++) {
            for (int x = 0; x < TILE_W; x++) {
                byte v = tile.pixels[y][x];
                int argb = 0xFF000000 | (rgb[tile.palette][v][0] << 16) | (rgb[tile.palette][v][1] << 8) | rgb[tile.palette][v][2];
                img.setRGB(xOffset + x, yOffset + y, argb);
            }
        }
    }

    static BufferedImage CreateMapImage(BlockInfo[][] blocks, int[][][] rgb) {
        int rows = blocks.length;
        int cols = blocks[0].length;

        int outH = rows * BLOCK_H;
        int outW = cols * BLOCK_W;
        BufferedImage img = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_ARGB);

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // down
                // TODO: omit the background pixels
                DrawTile(img, rgb, blocks[r][c].getTile(TileOrder.DownTopLeft), r * BLOCK_H, c * BLOCK_W);
                DrawTile(img, rgb, blocks[r][c].getTile(TileOrder.DownTopRight), r * BLOCK_H, c * BLOCK_W + TILE_W);
                DrawTile(img, rgb, blocks[r][c].getTile(TileOrder.DownBottomLeft), r * BLOCK_H + TILE_H, c * BLOCK_W);
                DrawTile(img, rgb, blocks[r][c].getTile(TileOrder.DownBottomRight), r * BLOCK_H + TILE_H, c * BLOCK_W + TILE_W);
                // TODO: up
            }
        }

        return img;
    }

    static void CreateSprite(int pokemonId, byte[] pixels, int[][] rgb) {
        BufferedImage img = new BufferedImage(SPRITE_SIZE, SPRITE_SIZE, BufferedImage.TYPE_INT_ARGB);
        for (int t = 0; t < NUM_TILES; t++) {
            int tileOffset = t * 32; // 32 bytes per 4bpp tile

            for (int i = 0; i < 32; i++) {
                int b = pixels[tileOffset + i] & 0xFF;
                int lowNibble  = b & 0x0F;
                int highNibble = (b >> 4) & 0x0F;
                int argb1 = 0xFF000000 | (rgb[lowNibble][0] << 16) | (rgb[lowNibble][1] << 8) | rgb[lowNibble][2];
                int argb2 = 0xFF000000 | (rgb[highNibble][0] << 16) | (rgb[highNibble][1] << 8) | rgb[highNibble][2];;

                // Make background transparent
//                if (lowNibble == 0) {
//                    argb1 -= 0xFF000000;
//                }
//                if (highNibble == 0) {
//                    argb2 -= 0xFF000000;
//                }

                int pixelIndex = i * 2;
                int dx = pixelIndex % 8;
                int dy = pixelIndex / 8;
                int tileX = (t % 8) * 8;
                int tileY = (t / 8) * 8;

                img.setRGB(tileX + dx,     tileY + dy, argb1);
                img.setRGB(tileX + dx + 1, tileY + dy, argb2);
            }
        }

        // Write img to sheet
        int offsetX = ((Math.abs(pokemonId) - 1) * 2 * SPRITE_SIZE) % (SPRITES_WIDE * SPRITE_SIZE);
        int offsetY = ((Math.abs(pokemonId) - 1) / 10) * SPRITE_SIZE;
        if (pokemonId < 0) {
            offsetX += SPRITE_SIZE;
        }
        Graphics2D g = sheet.createGraphics();
        g.drawImage(img, offsetX, offsetY, null);
        g.dispose();

        // Write sprite image to disk
        //File out = new File(String.format("out/%03d.png", pokemonId));
        //ImageIO.write(img, "png", out);
    }

    static void WriteImage(BufferedImage img, String pathname) throws Exception {
        ImageIO.write(img, "png", new File(pathname));
    }
}

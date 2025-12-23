import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;

public class RomReader implements AutoCloseable {
    private static final int PALETTE_LENGTH = 16;
    final int BYTES_PER_PALETTE = 32;
    // TODO: compare 16MB of RAM to load the entire file into RAM perf vs this method.
    // TODO: add seek, maybe still make thread safe so each instance i make operates on the same memory but can maintain their own position in the ROM.
    private final FileChannel fc;
    private final MappedByteBuffer map;

    public RomReader(Path path) throws Exception {
        this.fc = FileChannel.open(path, StandardOpenOption.READ);
        this.map = fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
        this.map.order(ByteOrder.LITTLE_ENDIAN);
    }

    public int read8(int offset) {
        return map.get(offset) & 0xFF;
    }

    public int read16(int offset) {
        int b0 = map.get(offset)     & 0xFF;
        int b1 = map.get(offset + 1) & 0xFF;
        return b0 | (b1 << 8);
    }

    public int read24(int offset) {
        int b0 = map.get(offset)     & 0xFF;
        int b1 = map.get(offset + 1) & 0xFF;
        int b2 = map.get(offset + 2) & 0xFF;
        return b0 | (b1 << 8) | (b2 << 16);
    }

    public void readBytes(int offset, byte[] out) {
        for (int i = 0; i < out.length; i++) {
            out[i] = map.get(offset + i);
        }
    }

    public int[][][] readPalettes(int offset, int numPalettes, boolean compressed) {
        // TODO: let user request different colour formats other than RGB
        // TODO: if compressed, decompress it first (will require a larger buffer?)
        byte[] buffer = new byte[BYTES_PER_PALETTE * numPalettes];
        readBytes(offset, buffer);
        if (compressed) {
            buffer = RomReader.decompressLZ10(buffer);
        }

        int[][][] rgb = new int[numPalettes][PALETTE_LENGTH][3];

        for (int p = 0; p < numPalettes; p++) {
            for (int k = 0; k < 32; k += 2) {
                int index = k / 2;
                int lo = buffer[p * BYTES_PER_PALETTE + k] & 0xFF;
                int hi = buffer[p * BYTES_PER_PALETTE + k + 1] & 0xFF;
                int color = (hi << 8) | lo;

                int red = color & 0x1F;
                int green = (color >>> 5) & 0x1F;
                int blue = (color >>> 10) & 0x1F;

                rgb[p][index][0] = 8 * red;
                rgb[p][index][1] = 8 * green;
                rgb[p][index][2] = 8 * blue;
            }
        }

        return rgb;
    }

    public String readString(int offset) {
        StringBuilder sb = new StringBuilder();
        map.position(offset);

        while (true) {
            byte b = map.get();
            int v = b & 0xFF;

            if (v == 0xFF) {
                break; // terminator reached
            }

            sb.append(mapChar(b));
        }

        return sb.toString();
    }

    private char mapChar(byte b) {
        int v = b & 0xFF;

        switch (v) {
            case 0x00: return ' ';

            case 0x1B: return 'é';

            case 0xA1: return '0';
            case 0xA2: return '1';
            case 0xA3: return '2';
            case 0xA4: return '3';
            case 0xA5: return '4';
            case 0xA6: return '5';
            case 0xA7: return '6';
            case 0xA8: return '7';
            case 0xA9: return '8';
            case 0xAA: return '9';
            case 0xAB: return '!';
            case 0xAC: return '?';
            case 0xAD: return '.';

            case 0xB4: return '\'';
            case 0xB5: return '♂';
            case 0xB6: return '♀';

            case 0xBB: return 'A';
            case 0xBC: return 'B';
            case 0xBD: return 'C';
            case 0xBE: return 'D';
            case 0xBF: return 'E';

            case 0xC0: return 'F';
            case 0xC1: return 'G';
            case 0xC2: return 'H';
            case 0xC3: return 'I';
            case 0xC4: return 'J';
            case 0xC5: return 'K';
            case 0xC6: return 'L';
            case 0xC7: return 'M';
            case 0xC8: return 'N';
            case 0xC9: return 'O';
            case 0xCA: return 'P';
            case 0xCB: return 'Q';
            case 0xCC: return 'R';
            case 0xCD: return 'S';
            case 0xCE: return 'T';
            case 0xCF: return 'U';

            case 0xD0: return 'V';
            case 0xD1: return 'W';
            case 0xD2: return 'X';
            case 0xD3: return 'Y';
            case 0xD4: return 'Z';

            default:
                System.out.printf("Unknown Character: %02X\n", v);
                return '\n';
        }
    }

    @Override
    public void close() throws Exception {
        fc.close();
    }

    public static byte[] decompressLZ10(byte[] input) {
        if ((input[0] & 0xFF) != 0x10) {
            throw new IllegalArgumentException("Not LZ10 compressed");
        }

        int outSize = ((input[1] & 0xFF) |
                ((input[2] & 0xFF) << 8) |
                ((input[3] & 0xFF) << 16));

        byte[] output = new byte[outSize];

        int inPos = 4;
        int outPos = 0;

        while (outPos < outSize) {
            int flags = input[inPos++] & 0xFF;

            for (int i = 7; i >= 0; i--) {
                if (outPos >= outSize) break;

                boolean compressed = ((flags >> i) & 1) == 1;

                if (!compressed) {
                    output[outPos++] = input[inPos++];
                } else {
                    int b1 = input[inPos++] & 0xFF;
                    int b2 = input[inPos++] & 0xFF;

                    int length = (b1 >> 4) + 3;
                    int disp = ((b1 & 0x0F) << 8) | b2;
                    int srcPos = outPos - (disp + 1);

                    for (int j = 0; j < length; j++) {
                        output[outPos++] = output[srcPos++];
                    }
                }
            }
        }

        return output;
    }
}

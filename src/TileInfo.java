class TileInfo {
    int palette;
    boolean vFlip;
    boolean hFlip;
    int tileIndex;
    byte[] pixels;

    public TileInfo(int value, byte[] pixels) {
        this.palette   = (value >> 12) & 0xF;
        this.vFlip = ((value >> 11) & 1) == 1;
        this.hFlip = ((value >> 10) & 1) == 1;
        this.tileIndex =  value & 0x3FF; // lowest 10 bits
        this.pixels = pixels; // 32 bytes. 4 bits per pixel. to make an 8x8 image.
    }

    public String toString() {
        return String.format("%03Xp%X", this.tileIndex, this.palette);
    }
}



import java.util.List;

public class BlockInfo {
    static final int BYTES = 16;
    private List<TileInfo> tiles;

    public BlockInfo(List<TileInfo> tiles) {
        this.tiles = tiles;
    }

    public TileInfo getTile(TileOrder tileOrder) {
        return tiles.get(tileOrder.ordinal());
    }

    public String toString() {
        return tiles.toString();
    }
}

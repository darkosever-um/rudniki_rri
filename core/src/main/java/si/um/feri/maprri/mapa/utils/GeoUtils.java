package si.um.feri.maprri.mapa.utils;

public class GeoUtils {

    /**
     * Pretvori koordinate na zaslonu (x, y) v GPS (lat, lon).
     */
    public static Geolocation unprojectMapCoordinates(float x, float y, ZoomXY beginTile) {
        double mapSize = MapRasterTiles.TILE_SIZE * Math.pow(2, beginTile.zoom);
        double globalPixelX = (beginTile.x * MapRasterTiles.TILE_SIZE) + x;
        double globalPixelY = (beginTile.y * MapRasterTiles.TILE_SIZE) + (Constants.NUM_TILES * MapRasterTiles.TILE_SIZE) - y;

        double n = Math.PI - 2.0 * Math.PI * globalPixelY / mapSize;

        double lng = (globalPixelX / mapSize * 360.0) - 180.0;
        double lat = 180.0 / Math.PI * Math.atan(0.5 * (Math.exp(n) - Math.exp(-n)));

        return new Geolocation(lat, lng);
    }
}

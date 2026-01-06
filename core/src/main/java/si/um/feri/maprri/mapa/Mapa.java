package si.um.feri.maprri.mapa;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;
import java.util.List;

import si.um.feri.maprri.ServerController;
import si.um.feri.maprri.mapa.utils.Constants;
import si.um.feri.maprri.mapa.utils.Geolocation;
import si.um.feri.maprri.mapa.utils.MapRasterTiles;
import si.um.feri.maprri.mapa.utils.ZoomXY;
import si.um.feri.maprri.models.Mine;
import si.um.feri.maprri.util.NetworkCallback;

public class Mapa extends ApplicationAdapter implements GestureDetector.GestureListener {

    private ShapeRenderer shapeRenderer;
    private Vector3 touchPosition;

    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;
    private OrthographicCamera camera;

    private Texture[] mapTiles;
    private ZoomXY beginTile;   // top left tile

    private ServerController server;

    // center geolocation
    private final Geolocation CENTER_GEOLOCATION = new Geolocation(46.1199, 14.8153);

    // test marker
    private final Geolocation MARKER_GEOLOCATION = new Geolocation(46.559070, 15.638100);

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Constants.MAP_WIDTH, Constants.MAP_HEIGHT);
        camera.position.set(Constants.MAP_WIDTH / 2f, Constants.MAP_HEIGHT / 2f, 0);
        camera.viewportWidth = Constants.MAP_WIDTH / 2f;
        camera.viewportHeight = Constants.MAP_HEIGHT / 2f;
        camera.zoom = 2f;
        camera.update();

        touchPosition = new Vector3();

        ZoomXY centerTile = MapRasterTiles.getTileNumber(CENTER_GEOLOCATION.lat, CENTER_GEOLOCATION.lng, Constants.ZOOM);
        beginTile = new ZoomXY(Constants.ZOOM, centerTile.x - ((Constants.NUM_TILES - 1) / 2), centerTile.y - ((Constants.NUM_TILES - 1) / 2));

        tiledMap = new TiledMap();
        TiledMapTileLayer layer = new TiledMapTileLayer(Constants.NUM_TILES, Constants.NUM_TILES, MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE);
        tiledMap.getLayers().add(layer);
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        server = new ServerController("http://127.0.0.1:8080");
        List<Mine> myMines = new ArrayList<>();
//        server.getAllMines(new NetworkCallback<List<Mine>>() {
//            @Override
//            public void onSuccess(List<Mine> result) {
//                System.out.println("Success");
//                System.out.println("MINES size: " + result.size());
//                myMines.addAll(result);
//                //Save mines to json
////                Mine.saveMineListToFile(myMines);
//                System.out.println("MY MINES SAVED");
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                System.out.println("ERROR:" + t.toString());
//            }
//        });
//        server.getMine(new NetworkCallback<Mine>() {
//            @Override
//            public void onSuccess(Mine result) {
//                System.out.println("Success");
//                System.out.println("MINE: " + result.toString());
//                Mine.saveMineToFile(result);
//                System.out.println("1 MINE SAVED");
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                System.out.println("ERROR:" + t.toString());
//            }
//        }, "27bc35d1c5e248bfbb89977a");

        myMines = Mine.loadMineList();
        System.out.println(myMines.size());
        System.out.println(myMines.toString());

        loadTilesAsync(layer);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);

        handleInput();

        camera.update();

        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        drawMarkers();
    }

    private void drawMarkers() {
        Vector2 marker = MapRasterTiles.getPixelPosition(MARKER_GEOLOCATION.lat, MARKER_GEOLOCATION.lng, beginTile.x, beginTile.y);

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.setColor(Color.RED);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.circle(marker.x, marker.y, 10);
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        touchPosition.set(x, y, 0);
        camera.unproject(touchPosition);
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        return false;
    }

    @Override
    public boolean longPress(float x, float y) {
        return false;
    }

    @Override
    public boolean fling(float velocityX, float velocityY, int button) {
        return false;
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        camera.translate(-deltaX, deltaY);
        return false;
    }

    @Override
    public boolean panStop(float x, float y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        if (initialDistance >= distance)
            camera.zoom += 0.02;
        else
            camera.zoom -= 0.02;
        return false;
    }

    @Override
    public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2, Vector2 pointer1, Vector2 pointer2) {
        return false;
    }

    @Override
    public void pinchStop() {

    }

    private void handleInput() {

        float moveFor = 12f; // 3f

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.zoom += 0.02;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom -= 0.02;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            camera.translate(-moveFor, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            camera.translate(moveFor, 0, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
            camera.translate(0, -moveFor, 0);
        }
        if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
            camera.translate(0, moveFor, 0);
        }

        camera.zoom = MathUtils.clamp(camera.zoom, 0.5f, 2f);

        float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
        float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

        camera.position.x = MathUtils.clamp(camera.position.x, effectiveViewportWidth / 2f, Constants.MAP_WIDTH - effectiveViewportWidth / 2f);
        camera.position.y = MathUtils.clamp(camera.position.y, effectiveViewportHeight / 2f, Constants.MAP_HEIGHT - effectiveViewportHeight / 2f);
    }

    private void loadTilesAsync(TiledMapTileLayer layer) {
        int size = Constants.NUM_TILES;

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {

                // Izračun X Y za ploščico
                final int tileX = beginTile.x + i;
                final int tileY = beginTile.y + (size - 1 - j); // flipnen j

                final int cellX = i;
                final int cellY = j;

                MapRasterTiles.loadTileAsync(Constants.ZOOM, tileX, tileY, new MapRasterTiles.TileLoadedCallback() {
                    @Override
                    public void onTileLoaded(Texture texture, int x, int y) { // ko je nalozena slika
                        TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
                        cell.setTile(new StaticTiledMapTile(new TextureRegion(texture)));

                        layer.setCell(cellX, cellY, cell); // setnemo v naš layer
                    }
                });
            }
        }
    }
}

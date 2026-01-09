package si.um.feri.maprri.mapa;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
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
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Logger;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.ShortArray;

import java.util.ArrayList;
import java.util.List;

import si.um.feri.maprri.ServerController;
import si.um.feri.maprri.mapa.utils.Constants;
import si.um.feri.maprri.mapa.utils.Geolocation;
import si.um.feri.maprri.mapa.utils.MapRasterTiles;
import si.um.feri.maprri.mapa.utils.ZoomXY;
import si.um.feri.maprri.models.Borders;
import si.um.feri.maprri.models.Mine;
import si.um.feri.maprri.util.NetworkCallback;
import com.badlogic.gdx.math.EarClippingTriangulator;

public class Mapa extends ApplicationAdapter implements GestureDetector.GestureListener {

    private ShapeRenderer shapeRenderer;
    private Vector3 touchPosition;

    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;
    private OrthographicCamera camera;

    private Texture[] mapTiles;
    private ZoomXY beginTile;   // top left tile

    private ServerController server;

    private List<Mine> myMines;

    // center geolocation
    private final Geolocation CENTER_GEOLOCATION = new Geolocation(46.1199, 14.8153);

    // test marker
    private final Geolocation MARKER_GEOLOCATION = new Geolocation(46.559070, 15.638100);

    private final EarClippingTriangulator triangulator = new EarClippingTriangulator();

    private Mine selectedMine = null;

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
        myMines = new ArrayList<>();

        List<Mine> finalMyMines = myMines;
        server.getAllMines(new NetworkCallback<List<Mine>>() {
            @Override
            public void onSuccess(List<Mine> result) {
                System.out.println("Success");
                System.out.println("MINES size: " + result.size());
                finalMyMines.addAll(result);

                Mine.saveMineListToFile(finalMyMines);
                System.out.println("MY MINES SAVED");
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR:" + t.toString());
            }
        });

        myMines = Mine.loadMineList();
        System.out.println(myMines.size());
        System.out.println(myMines.toString());

        server.getAllMines(new NetworkCallback<List<Mine>>() {
            @Override
            public void onSuccess(List<Mine> result) {
                Gdx.app.postRunnable(() -> {
                    myMines.clear();
                    myMines.addAll(result);
                    Mine.saveMineListToFile(result);
                    System.out.println("Mines loaded: " + myMines.size());
                });
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("ERROR: " + t.toString());
            }
        });


        InputMultiplexer multiplexer = new InputMultiplexer();

        multiplexer.addProcessor(new GestureDetector(this));

        multiplexer.addProcessor(new InputAdapter() {
            @Override
            public boolean scrolled(float amountX, float amountY) {
                camera.zoom += amountY * 0.1f;

                camera.zoom = MathUtils.clamp(camera.zoom, 0.1f, 3.0f);

                return true;
            }
        });

        Gdx.input.setInputProcessor(multiplexer);

        loadTilesAsync(layer);
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);

        handleInput();

        camera.update();

        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        drawMines();

        drawMineEntities(true, true);

    }

    private void drawMineEntities(boolean showWorkers, boolean showInfra) {
        if (myMines == null || myMines.isEmpty() || (!showWorkers && !showInfra)) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Mine mine : myMines) {
            if (mine.geometry == null) continue;

            for (Borders border : mine.geometry) {
                if (border.coordinates == null) continue;

                for (int i = 0; i < border.coordinates.length; i++) {
                    float[][][] polygonData = border.coordinates[i];
                    if (polygonData.length == 0) continue;

                    // samo enkrat za rudnik izracunamo gi lehko risemo
                    float[][] outerRing = polygonData[0];
                    float[] vertices = new float[outerRing.length * 2];
                    float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
                    float minY = Float.MAX_VALUE, maxY = -Float.MAX_VALUE;

                    for (int k = 0; k < outerRing.length; k++) {
                        Vector2 pixelPos = MapRasterTiles.getPixelPosition(outerRing[k][1], outerRing[k][0], beginTile.x, beginTile.y);
                        vertices[k * 2] = pixelPos.x;
                        vertices[k * 2 + 1] = pixelPos.y;

                        if (pixelPos.x < minX) minX = pixelPos.x;
                        if (pixelPos.x > maxX) maxX = pixelPos.x;
                        if (pixelPos.y < minY) minY = pixelPos.y;
                        if (pixelPos.y > maxY) maxY = pixelPos.y;
                    }
                    Polygon libGdxPolygon = new Polygon(vertices);

                    // inftrastrukture
                    if (showInfra && mine.getInfrastructures() != null) {
                        drawRandomDotsInPolygon(mine.getInfrastructures().size(), libGdxPolygon, minX, maxX, minY, maxY,
                            Color.RED, 4.0f, mine.getName().hashCode() + 123);
                    }

                    // workerji
                    if (showWorkers && mine.getWorkers() != null) {
                        drawRandomDotsInPolygon(mine.getWorkers().size(), libGdxPolygon, minX, maxX, minY, maxY,
                            Color.ORANGE, 3.0f, mine.getName().hashCode());
                    }
                }
            }
        }
        shapeRenderer.end();
    }

    private void drawRandomDotsInPolygon(int count, Polygon poly, float minX, float maxX, float minY, float maxY,
                                         Color color, float baseRadius, int seed) {
        java.util.Random random = new java.util.Random(seed);
        shapeRenderer.setColor(color);

        float dynamicRadius = baseRadius * camera.zoom;
        if (dynamicRadius < 2.0f) dynamicRadius = 2.0f;

//        if(baseRadius == 6.0f) dynamicRadius += 1f;

        for (int j = 0; j < count; j++) {
            float randomX = 0, randomY = 0;
            boolean found = false;
            int attempts = 0;

            while (!found && attempts < 20) {
                randomX = minX + random.nextFloat() * (maxX - minX);
                randomY = minY + random.nextFloat() * (maxY - minY);
                if (poly.contains(randomX, randomY)) found = true;
                attempts++;
            }

            if (found) {
                shapeRenderer.circle(randomX, randomY, dynamicRadius);
            }
        }
    }

    private void drawMines() {
        if (myMines == null || myMines.isEmpty()) return;

        shapeRenderer.setProjectionMatrix(camera.combined);

        // Omogočimo prosojnost za polnilo
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        for (Mine mine : myMines) {
            if (mine.geometry == null) continue;

            for (Borders border : mine.geometry) {
                if (border.coordinates == null) continue;

                for (int i = 0; i < border.coordinates.length; i++) {
                    float[][][] polygon = border.coordinates[i];
                    if (polygon.length == 0) continue;

                    // popravek točk
                    float[][] outerRing = polygon[0];
                    float[] vertices = new float[outerRing.length * 2];
                    for (int k = 0; k < outerRing.length; k++) {
                        Vector2 p = MapRasterTiles.getPixelPosition(outerRing[k][1], outerRing[k][0], beginTile.x, beginTile.y);
                        vertices[k * 2] = p.x;
                        vertices[k * 2 + 1] = p.y;
                    }

                    // fill
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    shapeRenderer.setColor(new Color(0.2f, 0.5f, 1f, 0.3f));

                    try {
                        ShortArray triangleIndices = triangulator.computeTriangles(vertices);
                        for (int j = 0; j < triangleIndices.size; j += 3) {
                            int v1 = triangleIndices.get(j) * 2;
                            int v2 = triangleIndices.get(j + 1) * 2;
                            int v3 = triangleIndices.get(j + 2) * 2;
                            shapeRenderer.triangle(
                                vertices[v1], vertices[v1 + 1],
                                vertices[v2], vertices[v2 + 1],
                                vertices[v3], vertices[v3 + 1]
                            );
                        }
                    } catch (Exception e) {
                        // ko nemre zrisati
                    }
                    shapeRenderer.end();

                    // 3. RISANJE ROBA (Temno modra, da so črte povezane)
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
                    shapeRenderer.setColor(Color.BLUE);
                    for (int k = 0; k < vertices.length - 2; k += 2) {
                        shapeRenderer.line(vertices[k], vertices[k+1], vertices[k+2], vertices[k+3]);
                    }
                    // Povežemo zadnjo točko s prvo, če nista identični
                    shapeRenderer.line(vertices[vertices.length-2], vertices[vertices.length-1], vertices[0], vertices[1]);
                    shapeRenderer.end();
                }
            }
        }
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    private class DisplayMine {
        Mine originalData;
        List<Polygon> hitboxes;
        List<float[]> triangulatedVertices;
        List<ShortArray> triangleIndices;

        public DisplayMine(Mine mine) {
            this.originalData = mine;
            this.hitboxes = new ArrayList<>();
            this.triangulatedVertices = new ArrayList<>();
            this.triangleIndices = new ArrayList<>();
        }
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

        camera.zoom = MathUtils.clamp(camera.zoom, 0.05f, 2f); // zoom edit

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

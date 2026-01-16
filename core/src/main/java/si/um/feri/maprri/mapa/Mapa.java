package si.um.feri.maprri.mapa;

import static si.um.feri.maprri.mapa.utils.GeoUtils.unprojectMapCoordinates;
import static si.um.feri.maprri.mapa.utils.MineFactory.createMineFromGeoPoints;
import static si.um.feri.maprri.mapa.utils.MineFactory.updateInfrastructureList;
import static si.um.feri.maprri.mapa.utils.MineFactory.updateWorkerList;

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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.ShortArray;

import java.util.ArrayList;
import java.util.List;

import com.graphhopper.util.PointList;
import si.um.feri.maprri.ServerController;
import si.um.feri.maprri.mapa.utils.Constants;
import si.um.feri.maprri.mapa.utils.Geolocation;
import si.um.feri.maprri.mapa.utils.MapRasterTiles;
import si.um.feri.maprri.mapa.utils.ZoomXY;
import si.um.feri.maprri.models.*;
import si.um.feri.maprri.util.InfrastructurePath;
import si.um.feri.maprri.util.NetworkCallback;

import com.badlogic.gdx.math.EarClippingTriangulator;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import si.um.feri.maprri.mapa.utils.LoadIndustry;


public class Mapa extends ApplicationAdapter implements GestureDetector.GestureListener {

    private ShapeRenderer shapeRenderer;
    private Vector3 touchPosition;

    private TiledMap tiledMap;
    private TiledMapRenderer tiledMapRenderer;
    private OrthographicCamera camera;

    private ZoomXY beginTile;

    private ServerController server;

    private List<Mine> myMines;
    private final Geolocation CENTER_GEOLOCATION = new Geolocation(46.1199, 14.8153); // center

    private final EarClippingTriangulator triangulator = new EarClippingTriangulator();

    private Mine selectedMine = null;

    private Stage stage;
    private Skin skin;

    private List<Mine> localMines = new ArrayList<>();
    private com.badlogic.gdx.utils.Json json = new com.badlogic.gdx.utils.Json();

    private com.badlogic.gdx.scenes.scene2d.ui.Window editWindow;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField nameField;

    // stanje za risanje
    private boolean isDrawing = false;
    private List<Vector2> drawnPoints = new ArrayList<>(); // Točke v pikslih za izrisovanje črt med risanjem
    private List<double[]> drawnGeoPoints = new ArrayList<>(); // Točke v lat/lon za shranjevanje v Mine

    // za tipko spreminjati
    private TextButton btnAdd;

    // za inpute
    private com.badlogic.gdx.scenes.scene2d.ui.TextField workersField;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField infraField;

    private com.badlogic.gdx.scenes.scene2d.ui.SelectBox<si.um.feri.maprri.models.enums.MineStatus> statusSelect;
    private com.badlogic.gdx.scenes.scene2d.ui.SelectBox<si.um.feri.maprri.models.enums.MineType> typeSelect;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField municipalityField;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField startYearField;
    private com.badlogic.gdx.scenes.scene2d.ui.TextField endYearField;

    private List<Industry> industries = new ArrayList<>();
    private List<PathInfo> allPaths = new ArrayList<>();

    private com.badlogic.gdx.graphics.g2d.SpriteBatch batch;
    private Texture factoryIcon;
    private Texture mineIcon;

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

        batch = new com.badlogic.gdx.graphics.g2d.SpriteBatch();
        factoryIcon = new Texture(Gdx.files.internal("icons/factory.png"));
        mineIcon = new Texture(Gdx.files.internal("icons/mine.png"));

        touchPosition = new Vector3();

        ZoomXY centerTile = MapRasterTiles.getTileNumber(CENTER_GEOLOCATION.lat, CENTER_GEOLOCATION.lng, Constants.ZOOM);
        beginTile = new ZoomXY(Constants.ZOOM, centerTile.x - ((Constants.NUM_TILES - 1) / 2), centerTile.y - ((Constants.NUM_TILES - 1) / 2));

        tiledMap = new TiledMap();
        TiledMapTileLayer layer = new TiledMapTileLayer(Constants.NUM_TILES, Constants.NUM_TILES, MapRasterTiles.TILE_SIZE, MapRasterTiles.TILE_SIZE);
        tiledMap.getLayers().add(layer);
        tiledMapRenderer = new OrthogonalTiledMapRenderer(tiledMap);

        server = new ServerController("http://127.0.0.1:8080");
        myMines = new ArrayList<>();

        industries = LoadIndustry.load();

        for(Industry industry : industries){

        }

        loadLocalMines();

        //Load graphhopper:
        new Thread(() -> {
            InfrastructurePath.init();

            Gdx.app.postRunnable(() -> {
                if (!myMines.isEmpty() && !industries.isEmpty()) {
                    for(Mine mine : myMines){
                        List<Infrastructure> infrastructure = mine.getInfrastructures();
                        for(int i = 0; i < infrastructure.size()-1; i++){
                            int randomNum = (int)(Math.random() * (industries.size() - 1));
                            Industry industry = industries.get(randomNum);
                            PathInfo points = InfrastructurePath.findPath(mine.getLat(), mine.getLon(), industry.lat, industry.lng);
                            allPaths.add(points);
                        }
                    }
                }
            });
        }).start();

        server.getAllMines(new NetworkCallback<List<Mine>>() {
            @Override
            public void onSuccess(List<Mine> result) {
                Gdx.app.postRunnable(() -> {
                    myMines.clear();
                    myMines.addAll(result);
                    if (localMines != null) {
                        myMines.addAll(localMines);
                    }
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
                camera.zoom = MathUtils.clamp(camera.zoom, 0.005f, 2.0f);
                return true;
            }
        });
        Gdx.input.setInputProcessor(multiplexer);

        loadTilesAsync(layer);

        // UI Setup
        skin = new Skin(Gdx.files.internal("metal-ui.json"));
        stage = new Stage(new ScreenViewport());

        Table uiTable = new Table();
        uiTable.setFillParent(true);
        uiTable.bottom().right().pad(20);

        btnAdd = new TextButton("Dodaj Rudnik", skin);
        btnAdd.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (!isDrawing) {
                    isDrawing = true;
                    drawnPoints.clear();
                    drawnGeoPoints.clear();
                    btnAdd.setText("ZAKLJUCI");
                    if (editWindow != null) editWindow.remove();
                    selectedMine = null;
                } else {
                    if (drawnGeoPoints.size() < 3) return;
                    Mine newMine = createMineFromGeoPoints(drawnGeoPoints);
                    isDrawing = false;
                    btnAdd.setText("Dodaj Rudnik");
                    drawnPoints.clear();
                    selectedMine = newMine;
                    showEditPanel(newMine, true);
                }
            }
        });

        uiTable.add(btnAdd).width(150).height(50);
        stage.addActor(uiTable);

        InputMultiplexer mainMultiplexer = (InputMultiplexer) Gdx.input.getInputProcessor();
        mainMultiplexer.addProcessor(0, stage);
    }

    @Override
    public void render() {
        super.render();
        ScreenUtils.clear(0, 0, 0, 1);

        handleInput();

        camera.update();

        tiledMapRenderer.setView(camera);
        tiledMapRenderer.render();

        float ZOOM_THRESHOLD = 1f;

        if (camera.zoom > ZOOM_THRESHOLD) {
            drawMineIcons();
        } else {
            drawMines();
            drawMineEntities(true, true);
        }

        drawIndustries();

        if(!allPaths.isEmpty()){
            for(PathInfo path : allPaths){
                drawPath(path.points);
            }
        }

        if (isDrawing && !drawnPoints.isEmpty()) {
            shapeRenderer.setProjectionMatrix(camera.combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(Color.RED);

            for (int i = 0; i < drawnPoints.size() - 1; i++) {
                Vector2 p1 = drawnPoints.get(i);
                Vector2 p2 = drawnPoints.get(i + 1);
                shapeRenderer.line(p1.x, p1.y, p2.x, p2.y);
            }

            if (drawnPoints.size() > 2) {
                Vector2 first = drawnPoints.get(0);
                Vector2 last = drawnPoints.get(drawnPoints.size() - 1);
                shapeRenderer.setColor(Color.ORANGE); // Oranžna za "zapiralno" črto
                shapeRenderer.line(last.x, last.y, first.x, first.y);
            }
            shapeRenderer.end();

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(Color.RED);
            for (Vector2 point : drawnPoints) {
                shapeRenderer.circle(point.x, point.y, 5 * camera.zoom);
            }
            shapeRenderer.end();
        }

        stage.act(Gdx.graphics.getDeltaTime());
        stage.draw();
    }

    private void drawMineIcons() {
        if (myMines == null || myMines.isEmpty()) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float iconSize = 25f * camera.zoom;
        float halfSize = iconSize / 2f;

        for (Mine mine : myMines) {
            if (mine.geometry == null || mine.geometry.isEmpty()) continue;
            double sumLat = 0;
            double sumLng = 0;
            int count = 0;

            try {
                float[][][] poly = mine.geometry.get(0).coordinates[0];
                for(int i=0; i<poly[0].length; i++) {
                    sumLat += poly[0][i][0];
                    sumLng += poly[0][i][1];
                    count++;
                }
            } catch (Exception e) { continue; }

            if (count == 0) continue;

            double centerLat = sumLat / count;
            double centerLng = sumLng / count;

            Vector2 pos = MapRasterTiles.getPixelPosition(centerLng, centerLat, beginTile.x, beginTile.y);

            if (camera.frustum.pointInFrustum(pos.x, pos.y, 0)) {
                batch.draw(mineIcon, pos.x - halfSize, pos.y - halfSize, iconSize, iconSize);
            }
        }
        batch.end();
    }

    private void drawIndustries() {
        if (industries == null || industries.isEmpty()) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        float iconSize = 30f * camera.zoom;
        if(camera.zoom < 1) iconSize = 30f;
        float halfSize = iconSize / 2f;

        for (Industry ind : industries) {
            Vector2 pos = MapRasterTiles.getPixelPosition(ind.lat, ind.lng, beginTile.x, beginTile.y);

            boolean isVisible = pos.x > camera.position.x - (camera.viewportWidth * camera.zoom) &&
                pos.x < camera.position.x + (camera.viewportWidth * camera.zoom) &&
                pos.y > camera.position.y - (camera.viewportHeight * camera.zoom) &&
                pos.y < camera.position.y + (camera.viewportHeight * camera.zoom);

            if (isVisible) {
                batch.draw(factoryIcon, pos.x - halfSize, pos.y - halfSize, iconSize, iconSize);
            }
        }
        batch.end();
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

                    if (showInfra && mine.getInfrastructures() != null) {
                        drawRandomDotsInPolygon(mine.getInfrastructures().size(), libGdxPolygon, minX, maxX, minY, maxY,
                            Color.RED, 4.0f, mine.getName().hashCode() + 123);
                    }

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

        float polyWidth = maxX - minX;
        float polyHeight = maxY - minY;

        float minDimension = Math.min(polyWidth, polyHeight);

        float zoomBasedRadius = baseRadius * camera.zoom;

        float maxAllowedRadius = minDimension / 5.0f;

        float finalRadius = Math.min(zoomBasedRadius, maxAllowedRadius);

        if (finalRadius < 0.5f) finalRadius = 0.5f;

        if (finalRadius > minDimension / 2.0f) finalRadius = minDimension / 2.0f;

        int segments = Math.max(6, (int)(8 + finalRadius));

        for (int j = 0; j < count; j++) {
            float randomX = 0, randomY = 0;
            boolean found = false;
            int attempts = 0;

            while (!found && attempts < 20) {
                randomX = minX + random.nextFloat() * (maxX - minX);
                randomY = minY + random.nextFloat() * (maxY - minY);

                if (poly.contains(randomX, randomY)) {
                    found = true;
                }
                attempts++;
            }

            if (found) {
                shapeRenderer.circle(randomX, randomY, finalRadius, segments);
            }
        }
    }

    private void drawMines() {
        if (myMines == null || myMines.isEmpty()) return;

        shapeRenderer.setProjectionMatrix(camera.combined);
        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA, com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);

        // naredimo vse fille
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (Mine mine : myMines) {
            if (mine.geometry == null) continue;
            for (Borders border : mine.geometry) {
                if (border.coordinates == null) continue;
                for (float[][][] polygon : border.coordinates) {
                    if (polygon.length == 0) continue;
                    float[] vertices = getVertices(polygon[0]);

                    // za izbroni rudnik damo drugo barvo
                    if (mine == selectedMine) shapeRenderer.setColor(new Color(1f, 1f, 0f, 0.4f)); // Rumena
                    else shapeRenderer.setColor(new Color(0.2f, 0.5f, 1f, 0.3f)); // Modra

                    try {
                        ShortArray indices = triangulator.computeTriangles(vertices);
                        for (int j = 0; j < indices.size; j += 3) {
                            shapeRenderer.triangle(
                                vertices[indices.get(j)*2], vertices[indices.get(j)*2+1],
                                vertices[indices.get(j+1)*2], vertices[indices.get(j+1)*2+1],
                                vertices[indices.get(j+2)*2], vertices[indices.get(j+2)*2+1]
                            );
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        for (Mine mine : myMines) {
            if (mine.geometry == null) continue;

            if (mine == selectedMine) shapeRenderer.setColor(Color.YELLOW);
            else shapeRenderer.setColor(Color.BLUE);

            for (Borders border : mine.geometry) {
                for (float[][][] polygon : border.coordinates) {
                    float[] vertices = getVertices(polygon[0]);
                    for (int k = 0; k < vertices.length - 2; k += 2) {
                        shapeRenderer.line(vertices[k], vertices[k+1], vertices[k+2], vertices[k+3]);
                    }
                    shapeRenderer.line(vertices[vertices.length-2], vertices[vertices.length-1], vertices[0], vertices[1]);
                }
            }
        }
        shapeRenderer.end();
        Gdx.gl.glDisable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
    }

    private float[] getVertices(float[][] ring) {
        float[] vertices = new float[ring.length * 2];
        for (int k = 0; k < ring.length; k++) {
            Vector2 p = MapRasterTiles.getPixelPosition(ring[k][1], ring[k][0], beginTile.x, beginTile.y);
            vertices[k * 2] = p.x;
            vertices[k * 2 + 1] = p.y;
        }
        return vertices;
    }

    public void drawPath(PointList path){
        if(path == null || path.isEmpty()){
            return;
        }

        Color randomColor = new Color(
            (float)Math.random(),
            (float)Math.random(),
            (float)Math.random(),
            1f
        );

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(Color.RED);

        float lineWidth = 10f * camera.zoom;

        for(int i = 0; i < path.size() - 1; i++){
            double lat1 = path.getLat(i);
            double lon1 = path.getLon(i);
            double lat2 = path.getLat(i+1);
            double lon2 = path.getLon(i+1);

            Vector2 point1 = MapRasterTiles.getPixelPosition(lat1, lon1, beginTile.x, beginTile.y);
            Vector2 point2 = MapRasterTiles.getPixelPosition(lat2, lon2, beginTile.x, beginTile.y);

            shapeRenderer.rectLine(point1.x, point1.y, point2.x, point2.y, lineWidth);
        }
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        if (batch != null) batch.dispose();
        if (factoryIcon != null) factoryIcon.dispose();
        if (mineIcon != null) mineIcon.dispose();
    }

    @Override
    public boolean touchDown(float x, float y, int pointer, int button) {
        touchPosition.set(x, y, 0);
        camera.unproject(touchPosition);
        return false;
    }

    @Override
    public boolean tap(float x, float y, int count, int button) {
        touchPosition.set(x, y, 0);
        camera.unproject(touchPosition);

        if (isDrawing) {
            drawnPoints.add(new Vector2(touchPosition.x, touchPosition.y));

            Geolocation geo = unprojectMapCoordinates(touchPosition.x, touchPosition.y, beginTile);
            drawnGeoPoints.add(new double[]{geo.lat, geo.lng});

            Gdx.app.log("RISANJE", "Dodana točka: " + geo.lat + ", " + geo.lng);
            return true;
        }

        if(selectedMine != null) editWindow.remove();
        selectedMine = null;

        for (Mine mine : myMines) {
            if (mine.geometry == null) continue;
            for (Borders border : mine.geometry) {
                for (float[][][] polygon : border.coordinates) {
                    float[] vertices = getVertices(polygon[0]);
                    Polygon poly = new Polygon(vertices);
                    if (poly.contains(touchPosition.x, touchPosition.y)) {
                        selectedMine = mine;
                        Gdx.app.log("MAPA", "Izbran rudnik: " + mine.getName());
                        showEditPanel(mine, false);
                        return true;
                    }
                }
            }
        }
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

        float moveFor = 1.5f;

        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            camera.zoom += 0.004;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.Q)) {
            camera.zoom -= 0.004;
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

        camera.zoom = MathUtils.clamp(camera.zoom, 0.005f, 2f);

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

    private void saveLocalMines() {
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("MyMapSettings");
        prefs.putString("local_mines", json.toJson(localMines));
        prefs.flush();
    }

    private void loadLocalMines() {
        com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("MyMapSettings");
        String data = prefs.getString("local_mines", "");
        if (!data.isEmpty()) {
            localMines = json.fromJson(ArrayList.class, Mine.class, data);
        }
    }

    // panel za urejanje rudnika
    private void showEditPanel(final Mine mine, final boolean isNew) {
        if (editWindow != null) editWindow.remove();

        com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter digitsFilter = new com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter() {
            @Override
            public boolean acceptChar(com.badlogic.gdx.scenes.scene2d.ui.TextField textField, char c) {
                return Character.isDigit(c);
            }
        };

        editWindow = new com.badlogic.gdx.scenes.scene2d.ui.Window("", skin);

        editWindow.setSize(320, stage.getHeight());
        editWindow.setPosition(0, 0);
        editWindow.setMovable(false);

        editWindow.top().left().padTop(30).padLeft(15).padRight(15);

        editWindow.defaults().left().width(290).padBottom(5);

        com.badlogic.gdx.scenes.scene2d.ui.Label titleLabel = new com.badlogic.gdx.scenes.scene2d.ui.Label(isNew ? "NOV RUDNIK" : "UREDI RUDNIK", skin);
        titleLabel.setFontScale(1.2f);
        editWindow.add(titleLabel).padBottom(15).row();

        editWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Ime rudnika:", skin)).row();
        nameField = new com.badlogic.gdx.scenes.scene2d.ui.TextField(mine.getName() != null ? mine.getName() : "", skin);
        editWindow.add(nameField).padBottom(10).row();

        editWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Obcina:", skin)).row();
        String muniVal = "";

         muniVal = mine.getMunicipality() != null ? mine.getMunicipality() : "";
        municipalityField = new com.badlogic.gdx.scenes.scene2d.ui.TextField(muniVal, skin);
        editWindow.add(municipalityField).padBottom(10).row();

        com.badlogic.gdx.scenes.scene2d.ui.Table yearsTable = new com.badlogic.gdx.scenes.scene2d.ui.Table();
        yearsTable.left();

        yearsTable.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Zacetek:", skin)).padRight(5);
        String startY = mine.getStartYear() != null ? String.valueOf(mine.getStartYear()) : "";
        startYearField = new com.badlogic.gdx.scenes.scene2d.ui.TextField(startY, skin);
        startYearField.setTextFieldFilter(digitsFilter);
        yearsTable.add(startYearField).width(80).padRight(15);

        yearsTable.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Konec:", skin)).padRight(5);
        String endY = mine.getEndYear() != null ? String.valueOf(mine.getEndYear()) : "";
        endYearField = new com.badlogic.gdx.scenes.scene2d.ui.TextField(endY, skin);
        endYearField.setTextFieldFilter(digitsFilter);
        yearsTable.add(endYearField).width(80);

        editWindow.add(yearsTable).padBottom(10).row();

        editWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Tip rudnika:", skin)).row();
        typeSelect = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(skin);
        typeSelect.setItems(si.um.feri.maprri.models.enums.MineType.values());

        typeSelect.setSelected(mine.getType());
        editWindow.add(typeSelect).padBottom(10).row();

        editWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Status:", skin)).row();
        statusSelect = new com.badlogic.gdx.scenes.scene2d.ui.SelectBox<>(skin);
        statusSelect.setItems(si.um.feri.maprri.models.enums.MineStatus.values());
        statusSelect.setSelected(mine.getStatus());
        editWindow.add(statusSelect).padBottom(10).row();

        editWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Stevilo delavcev:", skin)).row();
        int workerCount = (mine.getWorkers() != null) ? mine.getWorkers().size() : 0;
        workersField = new com.badlogic.gdx.scenes.scene2d.ui.TextField(String.valueOf(workerCount), skin);
        workersField.setTextFieldFilter(digitsFilter);
        editWindow.add(workersField).padBottom(10).row();

        editWindow.add(new com.badlogic.gdx.scenes.scene2d.ui.Label("Stevilo infrastrukture:", skin)).row();
        int infraCount = (mine.getInfrastructures() != null) ? mine.getInfrastructures().size() : 0;
        infraField = new com.badlogic.gdx.scenes.scene2d.ui.TextField(String.valueOf(infraCount), skin);
        infraField.setTextFieldFilter(digitsFilter);
        editWindow.add(infraField).padBottom(20).row();

        com.badlogic.gdx.scenes.scene2d.ui.Table buttonTable = new com.badlogic.gdx.scenes.scene2d.ui.Table();

        com.badlogic.gdx.scenes.scene2d.ui.TextButton btnSave = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("SHRANI", skin);
        com.badlogic.gdx.scenes.scene2d.ui.TextButton btnCancel = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("ZAPRI", skin);
        com.badlogic.gdx.scenes.scene2d.ui.TextButton btnDelete = new com.badlogic.gdx.scenes.scene2d.ui.TextButton("IZBRISI", skin);
        btnDelete.setColor(Color.RED);

        btnSave.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                mine.setName(nameField.getText());

                mine.setMunicipality(municipalityField.getText());

                try {
                    if (!startYearField.getText().isEmpty())
                         mine.setStartYear(Integer.parseInt(startYearField.getText()));
                    if (!endYearField.getText().isEmpty())
                         mine.setEndYear(Integer.parseInt(endYearField.getText()));
                } catch (NumberFormatException ignored) {}

                int newWorkerCount = 0;
                try {
                    String txt = workersField.getText();
                    if (!txt.isEmpty()) newWorkerCount = Integer.parseInt(txt);
                } catch (NumberFormatException ignored) {}
                updateWorkerList(mine, newWorkerCount);

                int newInfraCount = 0;
                try {
                    String txt = infraField.getText();
                    if (!txt.isEmpty()) newInfraCount = Integer.parseInt(txt);
                } catch (NumberFormatException ignored) {}
                updateInfrastructureList(mine, newInfraCount);

                if (isNew) {
                    localMines.add(mine);
                    myMines.add(mine);
                }
                saveLocalMines();
                editWindow.remove();
                selectedMine = null;
            }
        });

        btnCancel.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                editWindow.remove();
                selectedMine = null;
            }
        });

        btnDelete.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                myMines.remove(mine);
                localMines.remove(mine);
                saveLocalMines();
                editWindow.remove();
                selectedMine = null;
            }
        });

        buttonTable.add(btnSave).width(135).height(45).padRight(10);
        buttonTable.add(btnCancel).width(135).height(45).row();
        buttonTable.add(btnDelete).width(280).height(45).colspan(2).padTop(10);

        editWindow.add(buttonTable).row();

        stage.addActor(editWindow);
        stage.setKeyboardFocus(nameField);
    }
}

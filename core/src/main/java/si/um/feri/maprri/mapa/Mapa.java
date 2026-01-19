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
    import com.badlogic.gdx.scenes.scene2d.Actor;
    import com.badlogic.gdx.scenes.scene2d.InputEvent;
    import com.badlogic.gdx.scenes.scene2d.Stage;
    import com.badlogic.gdx.scenes.scene2d.ui.*;
    import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
    import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
    import com.badlogic.gdx.utils.ScreenUtils;
    import com.badlogic.gdx.utils.Select;
    import com.badlogic.gdx.utils.ShortArray;

    import java.time.LocalDate;
    import java.time.format.DateTimeFormatter;
    import java.util.ArrayList;
    import java.util.HashMap;
    import java.util.List;
    import java.util.Map;

    import com.badlogic.gdx.utils.Timer;
    import com.graphhopper.util.PointList;
    import org.w3c.dom.Text;
    import si.um.feri.maprri.ServerController;
    import si.um.feri.maprri.mapa.utils.Constants;
    import si.um.feri.maprri.mapa.utils.Geolocation;
    import si.um.feri.maprri.mapa.utils.Legend;
    import si.um.feri.maprri.mapa.utils.MapRasterTiles;
    import si.um.feri.maprri.mapa.utils.ZoomXY;
    import si.um.feri.maprri.models.*;
    import si.um.feri.maprri.models.enums.InfrastructureStatus;
    import si.um.feri.maprri.models.enums.MineralName;
    import si.um.feri.maprri.models.enums.WorkerType;
    import si.um.feri.maprri.util.InfrastructurePath;
    import si.um.feri.maprri.util.NetworkCallback;

    import com.badlogic.gdx.math.EarClippingTriangulator;
    import com.badlogic.gdx.utils.viewport.ScreenViewport;

    import si.um.feri.maprri.mapa.utils.LoadIndustry;
    import si.um.feri.maprri.util.Simulation;

    import javax.swing.*;


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

        // Zoom level
        private int currentMapZoom = Constants.ZOOM;

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

        private Legend legend;

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

            loadLocalMines();

            //Load graphhopper:
            new Thread(() -> {
                InfrastructurePath.init();

                Gdx.app.postRunnable(() -> {
                    System.out.println("MINES: " + myMines.size());
                    if (!myMines.isEmpty() && !industries.isEmpty()) {
                        for(Mine mine : myMines){
                            List<Infrastructure> infrastructure = mine.getInfrastructures();
                            for(int i = 0; i < infrastructure.size()-1; i++){
                                int randomNum = (int)(Math.random() * (industries.size() - 1));
                                Industry industry = industries.get(randomNum);

                                if(infrastructure.get(i).status != InfrastructureStatus.ACTIVE){
                                    continue;
                                }

                                PathInfo temp = InfrastructurePath.findPath(mine.getLat(), mine.getLon(), industry.lat, industry.lng);
                                System.out.println(temp.toString());
                                assert temp != null;
                                allPaths.add(temp);
                                infrastructure.get(i).setPath(temp.points);
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
                        if (!myMines.isEmpty() && !industries.isEmpty()) {
                            for(Mine mine : myMines){
                                List<Infrastructure> infrastructure = mine.getInfrastructures();
                                for(int i = 0; i < infrastructure.size()-1; i++){
                                    int randomNum = (int)(Math.random() * (industries.size() - 1));
                                    Industry industry = industries.get(randomNum);

                                    if(infrastructure.get(i).status != InfrastructureStatus.ACTIVE){
                                        continue;
                                    }

                                    PathInfo temp = InfrastructurePath.findPath(mine.getLat(), mine.getLon(), industry.lat, industry.lng);
                                    System.out.println(temp.toString());
                                    assert temp != null;
                                    allPaths.add(temp);
                                    infrastructure.get(i).setPath(temp.points);
                                }
                            }
                        }
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
                    camera.zoom = MathUtils.clamp(camera.zoom, 0.005f, 2.2f);
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

            legend = new Legend(skin, stage);

            InputMultiplexer mainMultiplexer = (InputMultiplexer) Gdx.input.getInputProcessor();
            mainMultiplexer.addProcessor(0, stage);
        }

        @Override
        public void render() {
            super.render();
            ScreenUtils.clear(0, 0, 0, 1);

            handleInput();

            float centerX = Constants.MAP_WIDTH / 2f;
            float centerY = Constants.MAP_HEIGHT / 2f;

            float threshold = MapRasterTiles.TILE_SIZE * 0.5f;

            if (Math.abs(camera.position.x - centerX) > threshold ||
                Math.abs(camera.position.y - centerY) > threshold) {
                updateTiles();
            }

            if (camera.zoom < 0.5f) {
                changeMapZoom(true);
            }
            else if (camera.zoom > 2.0f) {
                changeMapZoom(false);
            }

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

//            if(!allPaths.isEmpty()){
//                for(PathInfo path : allPaths){
//                    drawPath(path.points);
//                }
//            }

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
                    shapeRenderer.setColor(Color.ORANGE);
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

            legend.draw(batch, shapeRenderer);

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

                Vector2 pos = MapRasterTiles.getPixelPosition(centerLng, centerLat, beginTile.x, beginTile.y, currentMapZoom);

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
            if(camera.zoom < 1) iconSize = 60f * camera.zoom;
            float halfSize = iconSize / 2f;

            for (Industry ind : industries) {
                Vector2 pos = MapRasterTiles.getPixelPosition(ind.lat, ind.lng, beginTile.x, beginTile.y, currentMapZoom);

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

            float delta = Gdx.graphics.getDeltaTime();

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
                            Vector2 pixelPos = MapRasterTiles.getPixelPosition(outerRing[k][1], outerRing[k][0], beginTile.x, beginTile.y, currentMapZoom);
                            vertices[k * 2] = pixelPos.x;
                            vertices[k * 2 + 1] = pixelPos.y;

                            if (pixelPos.x < minX) minX = pixelPos.x;
                            if (pixelPos.x > maxX) maxX = pixelPos.x;
                            if (pixelPos.y < minY) minY = pixelPos.y;
                            if (pixelPos.y > maxY) maxY = pixelPos.y;
                        }
                        Polygon libGdxPolygon = new Polygon(vertices);

                        if (showWorkers && mine.getWorkers() != null) {
                            drawRandomDotsInPolygon(mine.getWorkers().size(), libGdxPolygon, minX, maxX, minY, maxY,
                                Color.YELLOW, 4.0f, mine.getName().hashCode() + 123);
                        }

                        if (showInfra && mine.getInfrastructures() != null) {
                            for (Infrastructure infra : mine.getInfrastructures()) {
                                if (infra.isMoving()) {
                                    infra.update(delta, beginTile, currentMapZoom);
                                    if (infra.isReturning()) {
                                        shapeRenderer.setColor(Color.ORANGE);
                                    } else {
                                        shapeRenderer.setColor(Color.RED);
                                    }

                                    Vector2 pos = infra.getCurrentPixelPos();
                                    float size = 5.0f * camera.zoom;
                                    if (size < 2f) size = 2f;

                                    shapeRenderer.circle(pos.x, pos.y, size);
                                }
                            }
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

            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (Mine mine : myMines) {
                if (mine.geometry == null) continue;
                for (Borders border : mine.geometry) {
                    if (border.coordinates == null) continue;
                    for (float[][][] polygon : border.coordinates) {
                        if (polygon.length == 0) continue;
                        float[] vertices = getVertices(polygon[0]);

                        if (mine == selectedMine) shapeRenderer.setColor(new Color(0f, 1f, 0f, 0.4f)); // Rumena
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
                Vector2 p = MapRasterTiles.getPixelPosition(ring[k][1], ring[k][0], beginTile.x, beginTile.y, currentMapZoom);
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
            shapeRenderer.setColor(Color.LIGHT_GRAY);
    //        shapeRenderer.setColor(1f, 1f, 0f, 0.5f);

            float lineWidth = 10f * camera.zoom;

            for(int i = 0; i < path.size() - 1; i++){
                double lat1 = path.getLat(i);
                double lon1 = path.getLon(i);
                double lat2 = path.getLat(i+1);
                double lon2 = path.getLon(i+1);

                Vector2 point1 = MapRasterTiles.getPixelPosition(lat1, lon1, beginTile.x, beginTile.y, currentMapZoom);
                Vector2 point2 = MapRasterTiles.getPixelPosition(lat2, lon2, beginTile.x, beginTile.y, currentMapZoom);

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

            camera.zoom = MathUtils.clamp(camera.zoom, 0.005f, 2.2f);

            float effectiveViewportWidth = camera.viewportWidth * camera.zoom;
            float effectiveViewportHeight = camera.viewportHeight * camera.zoom;

            camera.position.x = MathUtils.clamp(camera.position.x, effectiveViewportWidth / 2f, Constants.MAP_WIDTH - effectiveViewportWidth / 2f);
            camera.position.y = MathUtils.clamp(camera.position.y, effectiveViewportHeight / 2f, Constants.MAP_HEIGHT - effectiveViewportHeight / 2f);
        }

        private void loadTilesAsync(TiledMapTileLayer layer) {
            int size = Constants.NUM_TILES;

            int centerX = beginTile.x + (size / 2);
            int centerY = beginTile.y + (size / 2);

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {

                    final int tileX = beginTile.x + i;
                    final int tileY = beginTile.y + (size - 1 - j);

                    final int cellX = i;
                    final int cellY = j;

                    MapRasterTiles.loadTileAsync(currentMapZoom, tileX, tileY, centerX, centerY, new MapRasterTiles.TileLoadedCallback() {
                        @Override
                        public void onTileLoaded(Texture texture, int x, int y) {
                            TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
                            cell.setTile(new StaticTiledMapTile(new TextureRegion(texture)));
                            layer.setCell(cellX, cellY, cell);
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
            TextButton btnSimulation = new TextButton("SIMULACIJA", skin);
            btnSimulation.setColor(Color.YELLOW);
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

            List<Infrastructure> infrastructure = mine.getInfrastructures();
            for(int i = 0; i < infrastructure.size()-1; i++){
                int randomNum = (int)(Math.random() * (industries.size() - 1));
                Industry industry = industries.get(randomNum);

                if(infrastructure.get(i).status != InfrastructureStatus.ACTIVE){
                    continue;
                }

                PathInfo temp = InfrastructurePath.findPath(mine.getLat(), mine.getLon(), industry.lat, industry.lng);
                System.out.println(temp.toString());
                assert temp != null;
                allPaths.add(temp);
                infrastructure.get(i).setPath(temp.points);
            }

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

            btnSimulation.addListener(new ClickListener(){
                @Override
                public void clicked(InputEvent event, float x, float y){
                    showSimulationWindow(mine);
//                    editWindow.remove();
                }
            });

            buttonTable.add(btnSave).width(135).height(45).padRight(10);
            buttonTable.add(btnCancel).width(135).height(45).row();
            buttonTable.add(btnDelete).width(280).height(45).colspan(2).padTop(10).row();
            buttonTable.add(btnSimulation).width(280).height(45).colspan(2).padTop(10).row();

            editWindow.add(buttonTable).row();

            stage.addActor(editWindow);
            stage.setKeyboardFocus(nameField);
        }

        private void showSimulationWindow(Mine mine) {
            final Simulation simulation = new Simulation(mine);

            final Window simulationWindow = new Window("SIMULATION", skin);
            simulationWindow.setSize(900, 700);
//            simulationWindow.setModal(true);
            simulationWindow.setMovable(true);
            simulationWindow.setPosition(stage.getWidth() / 2 - 450, stage.getHeight() / 2 - 350);

            final Map<Integer, Label> workerStatusLabels = new HashMap<>();
            final Map<Integer, Label> infraStatusLabels = new HashMap<>();
            final Map<si.um.feri.maprri.models.enums.MineralName, Label> mineralMinedLabels = new HashMap<>();

            Table contentTable = new Table();
            contentTable.top().left().pad(20);

            // Section 1: Konfiguracija
            Label titleInput = new Label("1. Konfiguracija simulacije", skin);
            titleInput.setFontScale(1.1f);
            contentTable.add(titleInput).left().padBottom(10).row();

            Table inputTable = new Table();
            inputTable.left();

            inputTable.add(new Label("Datum od (dd.mm.llll): ", skin)).padRight(10);
            final TextField dateFromField = new TextField("", skin);
            inputTable.add(dateFromField).width(120).padRight(20);

            inputTable.add(new Label("Datum do (dd.mm.llll): ", skin)).padRight(10);
            final TextField dateToField = new TextField("", skin);
            inputTable.add(dateToField).width(120).padRight(20);

            inputTable.add(new Label("Casovna enota: ", skin)).padRight(10);
            SelectBox<String> timeUnitSelectBox = new SelectBox<>(skin);
            timeUnitSelectBox.setItems("Ura", "Dan");
            inputTable.add(timeUnitSelectBox).width(100);

            contentTable.add(inputTable).left().padBottom(30).row();

            // Section 2: Mine Info
            Label titleMineInfo = new Label("Podatki o rudniku", skin);
            titleMineInfo.setFontScale(1.1f);
            contentTable.add(titleMineInfo).left().padBottom(10).row();

            Table mineInfoTable = new Table();
            mineInfoTable.left();

            mineInfoTable.add(new Label("Trenutni datum: ", skin)).padRight(5);
            final Label lblCurrentDate = new Label("-", skin);
            lblCurrentDate.setColor(Color.YELLOW);
            mineInfoTable.add(lblCurrentDate).padRight(20);
            mineInfoTable.row();

            mineInfoTable.add(new Label("Ime rudnika: ", skin)).padRight(5);
            mineInfoTable.add(new Label(mine.getName(), skin)).padRight(20);
            mineInfoTable.row();

            mineInfoTable.add(new Label("Skupni stroski: ", skin)).padRight(5);
            final Label totalExpenses = new Label("0.00 EUR", skin);
            totalExpenses.setColor(Color.RED);
            mineInfoTable.add(totalExpenses).padRight(20);
            mineInfoTable.row();

            mineInfoTable.add(new Label("Dnevni stroski: ", skin)).padRight(5);
            final Label dailyExpenses = new Label("0.00 EUR", skin);
            dailyExpenses.setColor(Color.RED);
            mineInfoTable.add(dailyExpenses).padRight(20);

            contentTable.add(mineInfoTable).left().padBottom(20).row();

            // Workers List
            Label titleWorkers = new Label("Delavci", skin);
            titleWorkers.setColor(Color.CYAN);
            contentTable.add(titleWorkers).left().padBottom(5).row();

            Table workersTable = new Table();
            workersTable.defaults().left().padRight(15).padBottom(2);
            workersTable.add(new Label("Ime", skin));
            workersTable.add(new Label("Priimek", skin));
            workersTable.add(new Label("Placa", skin));
            workersTable.add(new Label("Vrsta", skin));
            workersTable.add(new Label("STATUS", skin)).row();

            if (mine.getWorkers() != null) {
                for (Worker worker : mine.getWorkers()) {
                    workersTable.add(new Label(worker.firstName, skin));
                    workersTable.add(new Label(worker.lastName, skin));
                    workersTable.add(new Label(String.valueOf(worker.salary), skin));
                    workersTable.add(new Label(WorkerType.values()[worker.type].name(), skin));

                    Label statusLabel = new Label("-", skin);
                    workerStatusLabels.put(worker.idNumber, statusLabel);
                    workersTable.add(statusLabel).row();
                }
            }
            contentTable.add(workersTable).left().padBottom(20).row();

            // Infrastructure List
            Label titleInfrastructure = new Label("Infrastruktura", skin);
            titleInfrastructure.setColor(Color.CYAN);
            contentTable.add(titleInfrastructure).left().padBottom(5).row();

            Table infrastructureTable = new Table();
            infrastructureTable.defaults().left().padRight(15).padBottom(2);
            infrastructureTable.add(new Label("Znamka", skin));
            infrastructureTable.add(new Label("Model", skin));
            infrastructureTable.add(new Label("Status (Original)", skin));
            infrastructureTable.add(new Label("SIM STATUS", skin)).row();

            if (mine.getInfrastructures() != null) {
                for (Infrastructure infrastructure : mine.getInfrastructures()) {
                    infrastructureTable.add(new Label(infrastructure.brand, skin));
                    infrastructureTable.add(new Label(infrastructure.model, skin));
                    infrastructureTable.add(new Label(infrastructure.status.name(), skin));

                    Label infraLabel = new Label("-", skin);
                    infraStatusLabels.put(infrastructure.IDNumber, infraLabel);
                    infrastructureTable.add(infraLabel).row();
                }
            }
            contentTable.add(infrastructureTable).left().padBottom(20).row();

            // Minerals List
            Label titleMinerals = new Label("Minerali", skin);
            titleMinerals.setColor(Color.CYAN);
            contentTable.add(titleMinerals).left().padBottom(5).row();

            Table mineralsTable = new Table();
            mineralsTable.defaults().left().padRight(15).padBottom(2);
            mineralsTable.add(new Label("Ime", skin));
            mineralsTable.add(new Label("Izkopano danes", skin)).row();

            if (mine.getMinerals() != null) {
                for (Mineral mineral : mine.getMinerals()) {
                    mineralsTable.add(new Label(mineral.name.name(), skin));
                    Label minedLabel = new Label("0.0", skin);
                    mineralMinedLabels.put(mineral.name, minedLabel);
                    mineralsTable.add(minedLabel).row();
                }
            }
            contentTable.add(mineralsTable).left().padBottom(20).row();

            ScrollPane scrollPane = new ScrollPane(contentTable, skin);
            scrollPane.setFadeScrollBars(true);
            scrollPane.setScrollingDisabled(true, false);

            simulationWindow.add(scrollPane).grow().row();

            final com.badlogic.gdx.scenes.scene2d.ui.Slider progressSlider = new com.badlogic.gdx.scenes.scene2d.ui.Slider(0, 100, 1, false, skin);
            final Label progressLabel = new Label("Korak: 0 / 0", skin);

            class UIUpdater {
                public void updateUI(SimulationHistory step) {
                    if (step == null) return;

                    lblCurrentDate.setText(step.date.toString());

                    totalExpenses.setText(String.format("%.2f EUR", simulation.totalExpenses));
                    dailyExpenses.setText(String.format("%.2f EUR", step.expenses));

                    progressLabel.setText("Korak: " + step.step + " / " + (int)progressSlider.getMaxValue());

                    for (java.util.Map.Entry<Integer, Label> entry : workerStatusLabels.entrySet()) {
                        int wId = entry.getKey();
                        Label lbl = entry.getValue();
                        if (step.workerAttendance.containsKey(wId)) {
                            boolean present = step.workerAttendance.get(wId);
                            lbl.setText(present ? "PRISOTEN" : "ODSOTEN");
                            lbl.setColor(present ? Color.GREEN : Color.RED);
                        } else {
                            lbl.setText("-");
                        }
                    }

                    for (InfrastructureSimStep infStep : step.infrastructureSimStepList) {
                        if (infraStatusLabels.containsKey(infStep.IDNumber)) {
                            Label lbl = infraStatusLabels.get(infStep.IDNumber);
                            lbl.setText(infStep.status.name());

                            if (infStep.status == InfrastructureStatus.BROKEN) lbl.setColor(Color.RED);
                            else if (infStep.status == InfrastructureStatus.ACTIVE) lbl.setColor(Color.GREEN);
                            else lbl.setColor(Color.ORANGE);
                        }
                    }

                    for (Map.Entry<MineralName, Label> entry : mineralMinedLabels.entrySet()) {
                        Label lbl = entry.getValue();
                        if (step.minedAmount.containsKey(entry.getKey())) {
                            lbl.setText(String.format("%.2f", step.minedAmount.get(entry.getKey())));
                        } else {
                            lbl.setText("0.0");
                        }
                    }
                }
            }

            final UIUpdater uiUpdater = new UIUpdater();

            final Timer.Task simTask = new Timer.Task() {
                @Override
                public void run() {
                    if (!simulation.isFinished() && simulation.isRunning()) {
                        simulation.calculateNextStep();
                        SimulationHistory step = simulation.getLastStep();

                        if (step != null) {
                            progressSlider.setValue(step.step);
                            uiUpdater.updateUI(step);
                        }
                    } else if (simulation.isFinished()) {
                        this.cancel();
                        simulation.setRunning(false);
                        System.out.println("Simulation finished");
                    }
                }
            };

            progressSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (progressSlider.isDragging()) {
                        simulation.setRunning(false);
                    }

                    int index = (int) progressSlider.getValue() - 1; // index 0 based, steps 1 based
                    if (index >= 0 && index < simulation.simulationHistory.size()) {
                        SimulationHistory historyStep = simulation.simulationHistory.get(index);
                        uiUpdater.updateUI(historyStep);
                    }
                }
            });

            TextButton btnClose = new TextButton("NAZAJ", skin);
            TextButton btnStart = new TextButton("ZACNI", skin);
            TextButton btnStop = new TextButton("STOP", skin);

            btnClose.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    simTask.cancel();
                    simulationWindow.remove();
                }
            });

            btnStart.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    try {
                        if (!simulation.isRunning() && simulation.simulationHistory.isEmpty()) {
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                            LocalDate startDate = LocalDate.parse(dateFromField.getText(), dtf);
                            LocalDate endDate = LocalDate.parse(dateToField.getText(), dtf);

                            if (startDate.isAfter(endDate)) return;

                            simulation.startDate = startDate;
                            simulation.endDate = endDate;
                            simulation.stepUnit = timeUnitSelectBox.getSelected();
                            simulation.reset();

                            long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
                            if(simulation.stepUnit.equals("Ura")) {
                                daysBetween *= 24;
                            }

                            progressSlider.setRange(0, daysBetween);
                            progressSlider.setValue(0);
                        }

                        simulation.setRunning(true);

                        if (!simTask.isScheduled()) {
                            com.badlogic.gdx.utils.Timer.schedule(simTask, 0f, 0.5f);
                        }
                    } catch (Exception e) {
                        System.out.println("Error: " + e.getMessage());
                    }
                }
            });

            btnStop.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    simulation.setRunning(false);
                }
            });

            Table controlsTable = new Table();
            controlsTable.add(new Label("Napredek: ", skin)).padRight(10);
            controlsTable.add(progressSlider).width(400).padRight(10);
            controlsTable.add(progressLabel).width(100).row();

            simulationWindow.add(controlsTable).padTop(10).padBottom(10).row();

            Table buttonTable = new Table();
            buttonTable.add(btnStart).width(150).height(50).padRight(10);
            buttonTable.add(btnStop).width(150).height(50).padRight(10);
            buttonTable.add(btnClose).width(150).height(50);

            simulationWindow.add(buttonTable).pad(10);

            stage.addActor(simulationWindow);
        }

        // Funkcija preveri, ki je kamera da lahko naložimo nove tile
        private void updateTiles() {
            float centerX = Constants.MAP_WIDTH / 2f;
            float centerY = Constants.MAP_HEIGHT / 2f;

            float diffX = camera.position.x - centerX;
            float diffY = camera.position.y - centerY;

            int tilesMovedX = Math.round(diffX / MapRasterTiles.TILE_SIZE);
            int tilesMovedY = Math.round(diffY / MapRasterTiles.TILE_SIZE);

            if (tilesMovedX == 0 && tilesMovedY == 0) return;

            beginTile.x += tilesMovedX;
            beginTile.y -= tilesMovedY;

            camera.translate(-tilesMovedX * MapRasterTiles.TILE_SIZE, -tilesMovedY * MapRasterTiles.TILE_SIZE);
            camera.update();

            TiledMapTileLayer layer = (TiledMapTileLayer) tiledMap.getLayers().get(0);
            loadTilesAsync(layer);
        }

        private void changeMapZoom(boolean zoomIn) {

            if (zoomIn && currentMapZoom >= 19) return;
            if (!zoomIn && currentMapZoom <= 2) return;

            MapRasterTiles.clearQueue();

            // GeoLocation kot anchor
            Geolocation centerGeo = si.um.feri.maprri.mapa.utils.GeoUtils.unprojectMapCoordinates(
                camera.position.x,
                camera.position.y,
                beginTile
            );

            currentMapZoom += (zoomIn ? 1 : -1);

            ZoomXY centerTile = MapRasterTiles.getTileNumber(centerGeo.lat, centerGeo.lng, currentMapZoom);

            beginTile = new ZoomXY(currentMapZoom,
                centerTile.x - ((Constants.NUM_TILES - 1) / 2),
                centerTile.y - ((Constants.NUM_TILES - 1) / 2)
            );

            Vector2 precisePosition = MapRasterTiles.getPixelPosition(
                centerGeo.lat,
                centerGeo.lng,
                MapRasterTiles.TILE_SIZE,
                currentMapZoom,
                beginTile.x,
                beginTile.y,
                Constants.MAP_HEIGHT
            );

            camera.position.set(precisePosition.x, precisePosition.y, 0);
            camera.zoom = 1.0f;
            camera.update();

            TiledMapTileLayer layer = (TiledMapTileLayer) tiledMap.getLayers().get(0);
            loadTilesAsync(layer);
        }
    }

package si.um.feri.maprri.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import si.um.feri.maprri.models.enums.InfrastructureStatus;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.graphhopper.util.PointList;
import si.um.feri.maprri.mapa.utils.MapRasterTiles;
import si.um.feri.maprri.mapa.utils.ZoomXY;

public class Infrastructure implements Json.Serializable {
    public String brand;
    public String model;
    public Integer IDNumber;
    public InfrastructureStatus status;
    public long lastMaintenance;
    public float operatingHours;
    public Integer kilometer;
    public double avgFuelConsumption;

    public Infrastructure(){}

    // za animacijo start
    public PointList path;
    public int currentPointIndex = 0;
    public float progress = 0;
    public float speed;
    public Vector2 currentPixelPos = new Vector2();
    public boolean hasPath = false;
    public boolean returning = false;
    public float waitTimer = 0;
    // za animacijo end

    public Infrastructure(String brand,
                          String model,
                          Integer IDNumber,
                          InfrastructureStatus status,
                          long lastMaintenance,
                          float operatingHours,
                          Integer kilometer, double avgFuelConsumption){
        this.brand = brand;
        this.model = model;
        this.IDNumber = IDNumber;
        this.status = status;
        this.lastMaintenance = lastMaintenance;
        this.operatingHours = operatingHours;
        this.kilometer = kilometer;
        this.avgFuelConsumption = avgFuelConsumption;
    }

    public Infrastructure(Infrastructure infrastructure){
        this.brand = infrastructure.brand;
        this.model = infrastructure.model;
        this.IDNumber = infrastructure.IDNumber;
        this.status = infrastructure.status;
        this.lastMaintenance = infrastructure.lastMaintenance;
        this.operatingHours = infrastructure.operatingHours;
        this.kilometer = infrastructure.kilometer;
        this.avgFuelConsumption = infrastructure.avgFuelConsumption;
    }


    public boolean validateInfrastructure(){
        if(status == null){
            return false;
        }

        return true;
    }

    public Infrastructure getInfraStructureFromJson(String jsonStr){
        try{
            Json json = new Json();
            return json.fromJson(Infrastructure.class, jsonStr);
        } catch (Exception e) {
            Gdx.app.error("INFRASTRUCTURE", "error loading scores:", e);
        }
        return null;
    }

    @Override
    public void write(Json json) {
        json.writeValue("brand", brand);
        json.writeValue("model", model);
        json.writeValue("IDNumber", IDNumber);
        json.writeValue("status", status.ordinal());

        json.writeObjectStart("lastMaintenance");
        json.writeValue("$date", lastMaintenance);
        json.writeObjectEnd();

        json.writeValue("operatingHours", operatingHours);
        json.writeValue("kilometer", kilometer);
        json.writeValue("avgFuelConsumption", avgFuelConsumption);
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {
        brand = jsonValue.getString("brand", null);
        model = jsonValue.getString("model", null);
        IDNumber = jsonValue.getInt("IDNumber", 0);
        int statusInt = jsonValue.getInt("status", 7);
        status = InfrastructureStatus.values()[statusInt];

        JsonValue dateObj = jsonValue.get("lastMaintenance");
        if (dateObj != null && dateObj.has("$date")) {
            lastMaintenance = dateObj.getLong("$date", 0);
        }

        operatingHours = jsonValue.getFloat("operatingHours", 0.00F);
        kilometer = jsonValue.getInt("kilometer", 0);
        avgFuelConsumption = jsonValue.getFloat("avgFuelConsumption", 0.0F);
    }

    public String toString(){
        return "Brand: " + brand + "\n"
            + "Model: " + model + "\n"
            + "ID: " + IDNumber + "\n"
            + "Status: " + status + "\n"
        + "Last maintenance: " + lastMaintenance + "\n"
        + "Operation hours: " + operatingHours + "\n"
        + "Kilometers: " + kilometer + "\n"
        + "avg fuel consumption: " + avgFuelConsumption + "\n";
    }

    public void setPath(PointList pointList) {
        this.path = pointList;
        if (path != null && path.size() > 1) {
            this.hasPath = true;
            this.speed = MathUtils.random(4.0f, 5.0f);
            this.currentPointIndex = MathUtils.random(0, path.size() - 2);
            this.progress = MathUtils.random(0f, 1f);
        }
    }

    public void update(float delta, ZoomXY beginTile, int zoom) {
        if (!hasPath) return;

        if (waitTimer > 0) {
            waitTimer -= delta;
            return;
        }

        int nextIdx = returning ? currentPointIndex - 1 : currentPointIndex + 1;

        if (nextIdx < 0 || nextIdx >= path.size()) {
            handlePathEnd();
            return;
        }

        Vector2 p1 = MapRasterTiles.getPixelPosition(path.getLat(currentPointIndex), path.getLon(currentPointIndex), beginTile.x, beginTile.y, zoom);
        Vector2 p2 = MapRasterTiles.getPixelPosition(path.getLat(nextIdx), path.getLon(nextIdx), beginTile.x, beginTile.y, zoom);

        float segmentDistance = p1.dst(p2);

        if (segmentDistance > 0) {
            progress += (speed / segmentDistance) * delta;
        } else {
            progress = 1.1f;
        }

        if (progress >= 1.0f) {
            progress = 0;
            currentPointIndex = nextIdx;

            if ((!returning && currentPointIndex >= path.size() - 1) || (returning && currentPointIndex <= 0)) {
                handlePathEnd();
            }
        }

        p1 = MapRasterTiles.getPixelPosition(path.getLat(currentPointIndex), path.getLon(currentPointIndex), beginTile.x, beginTile.y, zoom);
        int p2Idx = returning ? currentPointIndex - 1 : currentPointIndex + 1;
        if (p2Idx < 0) p2Idx = 0;
        if (p2Idx >= path.size()) p2Idx = path.size() - 1;
        p2 = MapRasterTiles.getPixelPosition(path.getLat(p2Idx), path.getLon(p2Idx), beginTile.x, beginTile.y, zoom);

        currentPixelPos.x = MathUtils.lerp(p1.x, p2.x, Math.min(progress, 1.0f));
        currentPixelPos.y = MathUtils.lerp(p1.y, p2.y, Math.min(progress, 1.0f));
    }

    private void handlePathEnd() {
        if (!returning) {
            returning = true;
            currentPointIndex = path.size() - 1;
        } else {
            returning = false;
            currentPointIndex = 0;
        }
        progress = 0;
        waitTimer = 2.0f;
    }

    public Vector2 getCurrentPixelPos() {
        return currentPixelPos;
    }

    public boolean isMoving() {
        return hasPath;
    }
    public boolean isReturning() { return returning; }
}

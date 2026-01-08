package si.um.feri.maprri.models;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import si.um.feri.maprri.models.enums.MineStatus;
import si.um.feri.maprri.models.enums.MineType;

public class Mine implements Json.Serializable {
    private String id;
    private String name;
    private String municipality;
    private Integer startYear;
    private Integer endYear;
    private Double lon;
    private Double lat;
    private String ownerId;
    private MineStatus status;
    private MineType type;
    private long created;
    private long modified;

    private List<Mineral> minerals;
    private List<Infrastructure> infrastructure;
    private List<Worker> workers;

    public ArrayList<Borders> geometry;

    public static final Preferences jsonfile = Gdx.app.getPreferences("mines");

    public Mine() {
    }

    public Mine(String name, String ownerId, MineStatus status, MineType type,
                List<Mineral> minerals, List<Infrastructure> infrastructure, List<Worker> workers, String municipality,
                Integer startYear, Integer endYear, Double lon, Double lat, long created, long modified) {

        this.name = name;
        this.ownerId = ownerId;
        this.status = status;
        this.type = type;
        this.minerals = minerals;
        this.infrastructure = infrastructure;
        this.workers = workers;
        this.municipality = municipality;
        this.startYear = startYear;
        this.endYear = endYear;
        this.lon = lon;
        this.lat = lat;
        this.created = created;
        this.modified = modified;
    }

    // Preveri ali so podatki validi
    public boolean validateMineData() {
        boolean isLocationValid = (lat != null && lat >= -90.0 && lat <= 90.0) &&
            (lon != null && lon >= -180.0 && lon <= 180.0);

        boolean areYearsValid = true;
        if (startYear != null && endYear != null) {
            areYearsValid = endYear >= startYear;
        }

        return name != null && !name.isEmpty() &&
            status != null &&
            type != null &&
            isLocationValid &&
            areYearsValid;
    }

    public static Mine getMineFromJson(String jsonStr){
        try{
            Json json = new Json();
            json.setIgnoreUnknownFields(true);
            return json.fromJson(Mine.class, jsonStr);
        } catch (Exception e) {
            Gdx.app.error("MINE", "error loading mines:", e);
        }
        return null;
    }

    public static List<Mine> getMineListFromJson(String jsonStr){
        List<Mine> mines = new ArrayList<>();

        try{
            Json json = new Json();
            json.setIgnoreUnknownFields(true);

            JsonValue jsonValue = new JsonReader().parse(jsonStr);

            for (JsonValue entry : jsonValue) {
                Mine m = json.readValue(Mine.class, entry);
                mines.add(m);
            }

        } catch (Exception e) {
            Gdx.app.error("MINE", "error loading mines:", e);
        }
        return mines;
    }

    @Override
    public void write(Json json) {
        if (id != null) {
            json.writeObjectStart("_id");
            json.writeValue("$oid", id);
            json.writeObjectEnd();
        }
        json.writeValue("name", name);
        json.writeValue("municipality", municipality);
        json.writeValue("startYear", startYear);
        json.writeValue("endYear", endYear);
        json.writeValue("lat", lat);
        json.writeValue("lon", lon);

        json.writeObjectStart("ownerId");
        json.writeValue("$oid", ownerId);
        json.writeObjectEnd();

        json.writeValue("status", status != null ? status.ordinal() : 0);
        json.writeValue("type", type != null ? type.ordinal() : 0);

        json.writeObjectStart("modified");
        json.writeValue("$date", modified);
        json.writeObjectEnd();

        json.writeObjectStart("created");
        json.writeValue("$date", created);
        json.writeObjectEnd();

        json.writeValue("minerals", minerals, List.class, Mineral.class);
        json.writeValue("infrastructure", infrastructure, List.class, Infrastructure.class);
        json.writeValue("workers", workers, List.class, Worker.class);

        json.writeValue("geometry", geometry, ArrayList.class, Borders.class);
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {
        if (jsonValue.has("_id")) {
            this.id = jsonValue.get("_id").getString("$oid");
        }

        if (jsonValue.has("ownerId") && !jsonValue.get("ownerId").isNull()) {
            this.ownerId = jsonValue.get("ownerId").getString("$oid");
        } else {
            this.ownerId = null;
        }

        if (jsonValue.has("created")) {
            this.created = jsonValue.get("created").getLong("$date");
        }

        if (jsonValue.has("modified")) {
            this.modified = jsonValue.get("modified").getLong("$date");
        }

        this.name = jsonValue.getString("name", null);
        this.municipality = jsonValue.getString("municipality", null);
        this.startYear = jsonValue.getInt("startYear", 0);
        this.endYear = jsonValue.getInt("endYear", 0);
        this.lon = jsonValue.getDouble("lon", 0.0);
        this.lat = jsonValue.getDouble("lat", 0.0);

        this.status = MineStatus.values()[jsonValue.getInt("status", 0)];
        this.type = MineType.values()[jsonValue.getInt("type", 0)];

        this.minerals = json.readValue(ArrayList.class, Mineral.class, jsonValue.get("minerals"));
        this.infrastructure = json.readValue(ArrayList.class, Infrastructure.class, jsonValue.get("infrastructure"));
        this.workers = json.readValue(ArrayList.class, Worker.class, jsonValue.get("workers"));

        if (jsonValue.has("geometry") && !jsonValue.get("geometry").isNull()) {
            this.geometry = json.readValue(ArrayList.class, Borders.class, jsonValue.get("geometry"));
        } else {
            this.geometry = new ArrayList<>();
        }
    }

    public static void saveMineToFile(Mine mine){
        String mineStr = Mine.jsonfile.getString("MINES", "[]");
        Json json = new Json();
        List<Mine> mines = new ArrayList<>();
        try{
            mines = json.fromJson(ArrayList.class, Mine.class, mineStr);
        } catch(Exception e){
            Gdx.app.error("MINES", "error loading mines from file:", e);
            mines = new ArrayList<>();
        }
        mines.add(mine);
        saveMineListToFile(mines);
    }

    public static void saveMineListToFile(List<Mine> mines){
        Json json = new Json();
        String jsonStr = json.toJson(mines);
        Mine.jsonfile.putString("MINES", jsonStr);
        Mine.jsonfile.flush();
    }

    public static List<Mine> loadMineList(){
        String mineStr = Mine.jsonfile.getString("MINES", "[]");
        Json json = new Json();
        List<Mine> mines = new ArrayList<>();
        try{
            mines = json.fromJson(ArrayList.class, Mine.class, mineStr);
        } catch(Exception e){
            Gdx.app.error("MINES", "error loading mines from file:", e);
        }
        return mines;
    }

    public List<Worker> getWorkers(){
        return workers;
    }

    public String getName() {
        return name;
    }
}

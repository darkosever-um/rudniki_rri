package si.um.feri.maprri.models;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import java.util.Arrays;

public class Borders implements Json.Serializable {
    public String id;
    public String type;
    public String mineId;
    public float[][][][] coordinates;

    @Override
    public void write(Json json) {
        json.writeObjectStart("_id");
        json.writeValue("$oid", id);
        json.writeObjectEnd();

        if (mineId != null) {
            json.writeObjectStart("mineId");
            json.writeValue("$oid", mineId);
            json.writeObjectEnd();
        }

        json.writeObjectStart("geometry");
        json.writeValue("type", type);
        json.writeValue("coordinates", coordinates);
        json.writeObjectEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {
        JsonValue idObj = jsonValue.get("_id");
        if (idObj != null && idObj.has("$oid")) {
            this.id = idObj.getString("$oid");
        }

        JsonValue mineIdObj = jsonValue.get("mineId");
        if (mineIdObj != null && mineIdObj.has("$oid")) {
            this.mineId = mineIdObj.getString("$oid");
        }

        JsonValue geometryNode = jsonValue.get("geometry");

        if (geometryNode == null && jsonValue.has("coordinates")) {
            geometryNode = jsonValue;
        }

        if (geometryNode != null) {
            this.type = geometryNode.getString("type", "MultiPolygon");
            JsonValue coordsJson = geometryNode.get("coordinates");

            if (coordsJson != null && !coordsJson.isNull()) {
                try {
                    if ("MultiPolygon".equalsIgnoreCase(this.type)) {
                        this.coordinates = new float[coordsJson.size][][][];
                        for (int i = 0; i < coordsJson.size; i++) {
                            this.coordinates[i] = parsePolygon(coordsJson.get(i));
                        }
                    } else {
                        float[][][] simplePoly = parsePolygon(coordsJson);
                        this.coordinates = new float[][][][]{simplePoly};
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Error parsing coordinates for ID: " + this.id);
                }
            }
        } else {
            System.err.println("Warning: Geometry node missing for ID: " + this.id);
        }
    }

    private float[][][] parsePolygon(JsonValue polygonJson) {
        float[][][] polygon = new float[polygonJson.size][][];
        for (int i = 0; i < polygonJson.size; i++) {
            JsonValue ringJson = polygonJson.get(i);
            polygon[i] = new float[ringJson.size][2];
            for (int j = 0; j < ringJson.size; j++) {
                JsonValue pointJson = ringJson.get(j);
                polygon[i][j][0] = pointJson.getFloat(0);
                polygon[i][j][1] = pointJson.getFloat(1);
            }
        }
        return polygon;
    }

    @Override
    public String toString() {
        String str = "ID: " + id + "\n"
            + "Mine id: " + mineId + "\n"
            + "Type: " + type + "\n";
        if(coordinates != null){
            str += "Coordinates: " + Arrays.deepToString(coordinates);
        }
        return str;
    }
}

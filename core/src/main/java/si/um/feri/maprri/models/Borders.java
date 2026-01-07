package si.um.feri.maprri.models;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
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
        json.writeValue("type", "MultiPolygon");
        json.writeValue("coordinates", coordinates);
        json.writeObjectEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonValue) {
        JsonValue idObj = jsonValue.get("_id");
        if (idObj != null && idObj.has("$oid")) this.id = idObj.getString("$oid");

        JsonValue mineIdObj = jsonValue.get("mineId");
        if (mineIdObj != null && mineIdObj.has("$oid")) this.mineId = mineIdObj.getString("$oid");

        JsonValue geometryNode = jsonValue.get("geometry");
        if (geometryNode == null && jsonValue.has("coordinates")) geometryNode = jsonValue;

        if (geometryNode != null) {
            this.type = geometryNode.getString("type", "MultiPolygon");
            JsonValue coordsJson = geometryNode.get("coordinates");

            if (coordsJson != null && !coordsJson.isNull() && coordsJson.size > 0) {
                try {
                    int depth = getArrayDepth(coordsJson);

                    if (depth == 4) {
                        // MultiPolygon: [ [ [ [x,y] ] ] ]
                        int polyCount = coordsJson.size;
                        this.coordinates = new float[polyCount][][][];
                        for (int i = 0; i < polyCount; i++) {
                            this.coordinates[i] = parsePolygon(coordsJson.get(i));
                        }
                    } else if (depth == 3) {
                        // Polygon: [ [ [x,y] ] ]
                        this.coordinates = new float[1][][][];
                        this.coordinates[0] = parsePolygon(coordsJson);
                    }
                } catch (Exception e) {
                    System.err.println("Error parsing coordinates for ID: " + this.id);
                }
            }
        }
    }

    private float[][][] parsePolygon(JsonValue polygonJson) {
        // polygonJson is array of rings
        int ringCount = polygonJson.size;
        float[][][] polygon = new float[ringCount][][];

        for (int i = 0; i < ringCount; i++) {
            JsonValue ringJson = polygonJson.get(i);
            int pointCount = ringJson.size;
            polygon[i] = new float[pointCount][2];

            for (int j = 0; j < pointCount; j++) {
                JsonValue pointJson = ringJson.get(j);

                if (pointJson.size >= 2) {
                    polygon[i][j][0] = pointJson.get(0).asFloat();
                    polygon[i][j][1] = pointJson.get(1).asFloat();
                }
            }
        }
        return polygon;
    }

    private int getArrayDepth(JsonValue val) {
        int depth = 0;
        JsonValue current = val;
        while (current != null && current.isArray() && current.size > 0) {
            depth++;
            current = current.get(0);
        }
        return depth;
    }

    @Override
    public String toString() {
        return "Borders ID: " + id;
    }
}

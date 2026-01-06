package si.um.feri.maprri.models;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

public class Borders implements Json.Serializable{
    public String id;
    public String type;
    public String mineId;
    public float[][][] coordinates;
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

        JsonValue innerGeometry = jsonValue.get("geometry");
        if (innerGeometry != null) {
            this.type = innerGeometry.getString("type", "Polygon");

            JsonValue coords = innerGeometry.get("coordinates");
            if (coords != null) {
                this.coordinates = json.readValue(float[][][].class, coords);
            }
        }
    }

    public String toString(){
        String str = "ID: " + id + "\n"
            + "Mine id: " + mineId + "\n"
            + "Type: " + type + "\n";
            str += "Coordinates: " + java.util.Arrays.deepToString(coordinates);
        return  str;
    }
}

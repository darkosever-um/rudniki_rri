package si.um.feri.maprri.mapa.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.List;
import si.um.feri.maprri.models.Industry;

public class LoadIndustry {

    public static List<Industry> load() {
        List<Industry> list = new ArrayList<>();

        try {
            if (!Gdx.files.internal("Industry.geojson").exists()) {
                Gdx.app.error("LOADER", "NAPAKA: Datoteka Industry.geojson ne obstaja!");
                return list;
            }

            JsonReader reader = new JsonReader();
            JsonValue root = reader.parse(Gdx.files.internal("Industry.geojson"));

            if (root == null || !root.has("features")) {
                Gdx.app.error("LOADER", "NAPAKA: JSON nima 'features' seznama.");
                return list;
            }

            JsonValue features = root.get("features");

            for (JsonValue feature : features) {
                try {
                    JsonValue geometry = feature.get("geometry");
                    if (geometry == null) continue;

                    String type = geometry.getString("type");
                    JsonValue coordinates = geometry.get("coordinates");

                    double lng = 0;
                    double lat = 0;

                    if ("MultiPoint".equalsIgnoreCase(type)) {
                        JsonValue firstPoint = coordinates.get(0);

                        lng = firstPoint.getDouble(0);
                        lat = firstPoint.getDouble(1);
                    }
                    else if ("Point".equalsIgnoreCase(type)) {
                        lng = coordinates.getDouble(0);
                        lat = coordinates.getDouble(1);
                    } else {
                        continue;
                    }

                    String name = "Neznano";
                    if (feature.has("properties") && feature.get("properties").has("name")) {
                        name = feature.get("properties").getString("name");
                    }

                    list.add(new Industry(name, lat, lng));

                } catch (Exception ex) {
                    System.out.println("Preskočeno eno podjetje zaradi napake: " + ex.getMessage());
                }
            }

            System.out.println("USPEH: Naloženih " + list.size() + " industrij.");

        } catch (Exception e) {
            Gdx.app.error("LOADER", "Kritična napaka: " + e.getMessage());
            e.printStackTrace();
        }

        return list;
    }
}

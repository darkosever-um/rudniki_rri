package si.um.feri.maprri.mapa.utils;

import com.badlogic.gdx.math.MathUtils;
import java.util.ArrayList;
import java.util.List;
import si.um.feri.maprri.models.Borders;
import si.um.feri.maprri.models.Mine;

public class MineFactory {
    public static Mine createMineFromGeoPoints(List<double[]> geoPoints) {
        Mine mine = new Mine();
        mine.setName("Nov Rudnik");
        mine.setId(java.util.UUID.randomUUID().toString());

        int size = geoPoints.size();

        float[][][] coordinates = new float[1][size + 1][2];

        for (int i = 0; i < size; i++) {
            coordinates[0][i][0] = (float) geoPoints.get(i)[1]; // Lon
            coordinates[0][i][1] = (float) geoPoints.get(i)[0]; // Lat
        }

        // Zapremo poligon
        coordinates[0][size][0] = (float) geoPoints.get(0)[1];
        coordinates[0][size][1] = (float) geoPoints.get(0)[0];

        Borders border = new Borders();
        float[][][][] multiPolygon = new float[1][][][];
        multiPolygon[0] = coordinates;
        border.coordinates = multiPolygon;

        ArrayList<Borders> geometry = new ArrayList<>();
        geometry.add(border);
        mine.geometry = geometry;

        mine.setLat(geoPoints.get(0)[0]);
        mine.setLon(geoPoints.get(0)[1]);


        return mine;
    }

    public static void updateWorkerList(Mine mine, int targetCount) {
        List<si.um.feri.maprri.models.Worker> list = mine.getWorkers();
        if (list == null) list = new ArrayList<>();

        while (list.size() < targetCount) {
            si.um.feri.maprri.models.Worker w = new si.um.feri.maprri.models.Worker();
            w.firstName = "Worker";
            w.lastName = "#" + (list.size() + 1);
            w.idNumber = 1000 + list.size();
            w.salary = 1200.0 + MathUtils.random(500);
            w.type = 1;
            w.birthDate = System.currentTimeMillis();
            list.add(w);
        }
        while (list.size() > targetCount) {
            list.remove(list.size() - 1);
        }
        mine.setWorkers(list);
    }

    public static void updateInfrastructureList(Mine mine, int targetCount) {
        List<si.um.feri.maprri.models.Infrastructure> list = mine.getInfrastructures();
        if (list == null) list = new ArrayList<>();

        si.um.feri.maprri.models.enums.InfrastructureStatus defaultStatus =
            si.um.feri.maprri.models.enums.InfrastructureStatus.values()[0];

        while (list.size() < targetCount) {
            si.um.feri.maprri.models.Infrastructure infra = new si.um.feri.maprri.models.Infrastructure(
                "Generic Brand",
                "Model-" + (list.size() + 1),
                5000 + list.size(),
                defaultStatus,
                System.currentTimeMillis(),
                0f,
                0
            );
            list.add(infra);
        }
        while (list.size() > targetCount) {
            list.remove(list.size() - 1);
        }
        mine.setInfrastructures(list);
    }
}

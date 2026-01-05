package si.um.feri.maprri.models;

import java.util.List;
import si.um.feri.maprri.models.enums.MineStatus;
import si.um.feri.maprri.models.enums.MineType;

public class Mine {

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

    // Using Lists for vectors
    private List<Mineral> minerals;
    private List<Infrastructure> infrastructure;
    private List<Worker> workers;

    public Mine() {
    }

    public Mine(String name, String ownerId, MineStatus status, MineType type,
                     List<Mineral> minerals, List<Infrastructure> infrastructure, List<Worker> workers, String municipality,
                     Integer startYear, Integer endYear, Double lon, Double lat) {

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
}
